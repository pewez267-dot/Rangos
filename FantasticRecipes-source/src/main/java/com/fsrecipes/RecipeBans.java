package com.fsrecipes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nucleo del baneo de recetas. SIN mixins: quitamos del RecipeManager las recetas cuyo item de
 * salida este baneado (replaceRecipes) y guardamos aparte las que sacamos para poder devolverlas
 * al desbanear sin necesidad de /reload. Tras cada cambio reenviamos la lista de recetas filtrada
 * a los clientes (ClientboundUpdateRecipesPacket), asi el cliente ni siquiera conoce la receta y
 * no aparece resultado fantasma en la mesa.
 *
 * Todo esto es autoritativo del servidor. La persistencia es un JSON en la carpeta config.
 */
public final class RecipeBans {

    private RecipeBans() {}

    /** Items cuyo crafteo esta prohibido (en memoria, autoritativo del servidor). */
    private static final Set<ResourceLocation> BANNED = ConcurrentHashMap.newKeySet();

    /** Recetas que hemos quitado del manager (para devolverlas al desbanear sin /reload). */
    private static final Map<ResourceLocation, Recipe<?>> REMOVED = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("fantasticrecipes-bans.json");
    }

    // ---------------------------------------------------------------- estado

    public static Set<ResourceLocation> banned() {
        return Collections.unmodifiableSet(BANNED);
    }

    public static boolean isBanned(ResourceLocation itemId) {
        return itemId != null && BANNED.contains(itemId);
    }

    public static int count() {
        return BANNED.size();
    }

    // ---------------------------------------------------------------- persistencia

    /** Lee el JSON de disco a memoria. Se llama al arrancar el servidor y en cada reload. */
    public static synchronized void loadFromDisk() {
        BANNED.clear();
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray()) {
                    ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                    if (id != null) {
                        BANNED.add(id);
                    }
                }
            }
        } catch (Exception ex) {
            FSRecipes.LOGGER.error("[FantasticRecipes] No se pudo leer {}: {}", path, ex.toString());
        }
    }

    private static synchronized void saveToDisk() {
        JsonArray arr = new JsonArray();
        List<String> ids = new ArrayList<>();
        for (ResourceLocation id : BANNED) {
            ids.add(id.toString());
        }
        Collections.sort(ids);
        for (String s : ids) {
            arr.add(s);
        }
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            FSRecipes.LOGGER.error("[FantasticRecipes] No se pudo guardar {}: {}", file(), ex.toString());
        }
    }

    // ---------------------------------------------------------------- aplicar al manager

    /**
     * Reconstruye el set completo de recetas (manager actual + las que teniamos guardadas) y vuelve
     * a repartir: las de salida baneada se sacan (y quedan en REMOVED), el resto se queda.
     *
     * @param freshReload true cuando el manager acaba de recargar TODAS las recetas (server start o
     *                    /reload): en ese caso descartamos REMOVED porque el manager ya trae todo.
     */
    public static synchronized void applyToManager(RecipeManager rm, RegistryAccess registryAccess, boolean freshReload) {
        if (rm == null) {
            return;
        }
        Map<ResourceLocation, Recipe<?>> full = new LinkedHashMap<>();
        for (Recipe<?> r : rm.getRecipes()) {
            full.put(r.getId(), r);
        }
        if (!freshReload) {
            for (Recipe<?> r : REMOVED.values()) {
                full.putIfAbsent(r.getId(), r);
            }
        }
        REMOVED.clear();
        List<Recipe<?>> keep = new ArrayList<>(full.size());
        for (Recipe<?> r : full.values()) {
            if (isBannedOutput(r, registryAccess)) {
                REMOVED.put(r.getId(), r);
            } else {
                keep.add(r);
            }
        }
        rm.replaceRecipes(keep);
    }

    private static boolean isBannedOutput(Recipe<?> recipe, RegistryAccess registryAccess) {
        if (BANNED.isEmpty()) {
            return false;
        }
        ItemStack out;
        try {
            out = recipe.getResultItem(registryAccess);
        } catch (Throwable t) {
            return false;
        }
        if (out == null || out.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(out.getItem());
        return id != null && BANNED.contains(id);
    }

    // ---------------------------------------------------------------- reenvio a clientes

    /** Reenvia la lista (filtrada) de recetas a todos los jugadores online. */
    public static void resyncClients(MinecraftServer server) {
        if (server == null) {
            return;
        }
        ClientboundUpdateRecipesPacket pkt = new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(pkt);
        }
    }

    // ---------------------------------------------------------------- API de cambios (servidor)

    /** Banea o desbanea el crafteo de un item y aplica el cambio en vivo. Devuelve true si cambio. */
    public static synchronized boolean setBanned(MinecraftServer server, ResourceLocation itemId, boolean ban) {
        if (itemId == null) {
            return false;
        }
        boolean changed = ban ? BANNED.add(itemId) : BANNED.remove(itemId);
        if (changed) {
            afterChange(server);
        }
        return changed;
    }

    /** Banea/desbanea una lista de items de golpe (categorias, etc.). Devuelve cuantos cambiaron. */
    public static synchronized int setBannedBulk(MinecraftServer server, List<ResourceLocation> ids, boolean ban) {
        int n = 0;
        for (ResourceLocation id : ids) {
            if (id == null) {
                continue;
            }
            if (ban ? BANNED.add(id) : BANNED.remove(id)) {
                n++;
            }
        }
        if (n > 0) {
            afterChange(server);
        }
        return n;
    }

    /** Desbanea TODO. */
    public static synchronized int clearAll(MinecraftServer server) {
        int n = BANNED.size();
        if (n > 0) {
            BANNED.clear();
            afterChange(server);
        }
        return n;
    }

    private static void afterChange(MinecraftServer server) {
        saveToDisk();
        if (server != null) {
            applyToManager(server.getRecipeManager(), server.registryAccess(), false);
            resyncClients(server);
        }
    }
}
