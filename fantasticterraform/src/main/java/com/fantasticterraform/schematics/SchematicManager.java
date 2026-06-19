package com.fantasticterraform.schematics;

import com.fantasticterraform.config.TerraformConfig;
import com.fantasticterraform.editing.ClipboardManager;
import com.fantasticterraform.editing.EditOperations;
import com.fantasticterraform.schematics.litematica.LitematicaReader;
import com.fantasticterraform.schematics.litematica.LitematicaWriter;
import com.fantasticterraform.schematics.sponge.SpongeSchematicReader;
import com.fantasticterraform.schematics.sponge.SpongeSchematicWriter;
import com.fantasticterraform.schematics.vanilla.VanillaStructureReader;
import com.fantasticterraform.schematics.vanilla.VanillaStructureWriter;
import com.fantasticterraform.selection.SelectionManager;
import com.fantasticterraform.selection.SelectionShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gestiona la lectura/escritura de schematics en los tres formatos y su integracion
 * con el portapapeles. La E/S de archivos se hace en un {@link ExecutorService}
 * dedicado (asincrono); el acceso al mundo siempre ocurre en el hilo del servidor.
 */
public final class SchematicManager {

    private static final ExecutorService IO_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "FantasticTerraform-SchematicIO");
                t.setDaemon(true);
                return t;
            });

    private SchematicManager() {
    }

    public static File schematicsDir() {
        File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "fantasticterraform/schematics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static List<String> list(SchematicFormat filter) {
        File dir = schematicsDir();
        File[] files = dir.listFiles();
        List<String> out = new ArrayList<>();
        if (files == null) {
            return out;
        }
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            if (filter == null || SchematicFormat.fromFileName(f.getName()) == filter) {
                out.add(f.getName());
            }
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /**
     * Guarda la seleccion activa (respetando su forma real, no solo el bounding box)
     * en el formato indicado. La extraccion del mundo es sincrona; la escritura es
     * asincrona.
     */
    public static void save(ServerPlayer player, SchematicFormat format, String name) {
        SelectionShape sel = SelectionManager.get(player).getShape();
        if (sel == null) {
            player.sendSystemMessage(Component.literal("\u00a7cNecesitas una seleccion valida para guardar un schematic."));
            return;
        }
        if (!EditOperations.checkVolume(player, sel)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        SchematicData data = extract(level, sel);

        String fileName = sanitize(name) + "." + format.extension();
        File file = new File(schematicsDir(), fileName);
        MinecraftServer server = player.server;
        boolean async = TerraformConfig.GENERAL.asyncSchematicIo.get();

        Runnable writeJob = () -> {
            try {
                switch (format) {
                    case LITEMATICA:
                        LitematicaWriter.write(file, data, sanitize(name));
                        break;
                    case VANILLA:
                        VanillaStructureWriter.write(file, data);
                        break;
                    case SPONGE:
                    default:
                        SpongeSchematicWriter.write(file, data);
                        break;
                }
                server.execute(() -> player.sendSystemMessage(Component.literal(
                        "\u00a7aSchematic guardado: \u00a7f" + fileName + " \u00a77(" + data.volume() + " bloques)")));
            } catch (Exception e) {
                server.execute(() -> player.sendSystemMessage(Component.literal(
                        "\u00a7cError al guardar el schematic: " + e.getMessage())));
            }
        };
        if (async) {
            IO_EXECUTOR.submit(writeJob);
        } else {
            writeJob.run();
        }
    }

    /** Carga un schematic al portapapeles del jugador (E/S asincrona, sin acceso al mundo). */
    public static void loadIntoClipboard(ServerPlayer player, String fileName) {
        File file = new File(schematicsDir(), fileName);
        if (!file.isFile()) {
            player.sendSystemMessage(Component.literal("\u00a7cNo existe el schematic: " + fileName));
            return;
        }
        MinecraftServer server = player.server;
        HolderLookup<Block> lookup = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        SchematicFormat format = SchematicFormat.fromFileName(fileName);
        boolean async = TerraformConfig.GENERAL.asyncSchematicIo.get();

        Runnable readJob = () -> {
            try {
                SchematicData data = readFormat(file, format, lookup);
                ClipboardManager.Clipboard clip = toClipboard(data);
                server.execute(() -> {
                    ClipboardManager.set(player.getUUID(), clip);
                    sendPreview(player, clip);
                    player.sendSystemMessage(Component.literal(
                            "\u00a7aSchematic cargado al portapapeles: \u00a7f" + fileName
                                    + " \u00a77(" + clip.size() + " bloques)"));
                });
            } catch (Exception e) {
                server.execute(() -> player.sendSystemMessage(Component.literal(
                        "\u00a7cError al cargar el schematic: " + e.getMessage())));
            }
        };
        if (async) {
            IO_EXECUTOR.submit(readJob);
        } else {
            readJob.run();
        }
    }

    /** Carga y pega directamente en {@code origin} con rotacion. */
    public static void loadAndPaste(ServerPlayer player, String fileName, BlockPos origin, Rotation rotation) {
        loadAndPaste(player, fileName, origin, rotation, false, false, false, 1);
    }

    /** Carga y pega con transformacion completa (rotacion Y, espejo X/Y/Z, escala). */
    public static void loadAndPaste(ServerPlayer player, String fileName, BlockPos origin, Rotation rotation,
                                    boolean mirrorX, boolean mirrorY, boolean mirrorZ, int scale) {
        File file = new File(schematicsDir(), fileName);
        if (!file.isFile()) {
            player.sendSystemMessage(Component.literal("\u00a7cNo existe el schematic: " + fileName));
            return;
        }
        MinecraftServer server = player.server;
        HolderLookup<Block> lookup = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        SchematicFormat format = SchematicFormat.fromFileName(fileName);
        boolean async = TerraformConfig.GENERAL.asyncSchematicIo.get();

        Runnable job = () -> {
            try {
                SchematicData data = readFormat(file, format, lookup);
                ClipboardManager.Clipboard clip = toClipboard(data);
                server.execute(() -> {
                    ClipboardManager.set(player.getUUID(), clip);
                    sendPreview(player, clip);
                    EditOperations.paste(player, (ServerLevel) player.level(), origin, rotation,
                            mirrorX, mirrorY, mirrorZ, scale,
                            com.fantasticterraform.masks.MaskManager.combinedFor(player));
                });
            } catch (Exception e) {
                server.execute(() -> player.sendSystemMessage(Component.literal(
                        "\u00a7cError al pegar el schematic: " + e.getMessage())));
            }
        };
        if (async) {
            IO_EXECUTOR.submit(job);
        } else {
            job.run();
        }
    }

    private static SchematicData readFormat(File file, SchematicFormat format, HolderLookup<Block> lookup) throws Exception {
        switch (format) {
            case LITEMATICA:
                return LitematicaReader.read(file, lookup);
            case VANILLA:
                return VanillaStructureReader.read(file, lookup);
            case SPONGE:
            default:
                return SpongeSchematicReader.read(file, lookup);
        }
    }

    /** Extrae la forma real de la seleccion a una estructura en memoria. */
    private static SchematicData extract(ServerLevel level, SelectionShape sel) {
        BlockPos origin = sel.getMin();
        BlockPos max = sel.getMax();
        int w = max.getX() - origin.getX() + 1;
        int h = max.getY() - origin.getY() + 1;
        int l = max.getZ() - origin.getZ() + 1;
        SchematicData data = new SchematicData(w, h, l);
        for (BlockPos pos : BlockPos.betweenClosed(origin, max)) {
            if (!sel.contains(pos)) {
                continue; // respeta la forma real: lo de fuera queda como aire.
            }
            int rx = pos.getX() - origin.getX();
            int ry = pos.getY() - origin.getY();
            int rz = pos.getZ() - origin.getZ();
            BlockState state = level.getBlockState(pos);
            data.setState(rx, ry, rz, state);
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                data.blockEntities.put(new BlockPos(rx, ry, rz), be.saveWithFullMetadata());
            }
        }
        return data;
    }

    private static ClipboardManager.Clipboard toClipboard(SchematicData data) {
        List<ClipboardManager.Entry> entries = new ArrayList<>();
        for (int y = 0; y < data.height; y++) {
            for (int z = 0; z < data.length; z++) {
                for (int x = 0; x < data.width; x++) {
                    BlockState state = data.getState(x, y, z);
                    BlockPos rel = new BlockPos(x, y, z);
                    CompoundTag be = data.blockEntities.get(rel);
                    if (state.isAir() && be == null) {
                        continue; // no pegamos aire por defecto.
                    }
                    entries.add(new ClipboardManager.Entry(rel, state, be));
                }
            }
        }
        return new ClipboardManager.Clipboard(entries);
    }

    private static String sanitize(String name) {
        String s = name == null ? "" : name.trim();
        s = s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (s.isEmpty()) {
            s = "schematic_" + System.currentTimeMillis();
        }
        return s;
    }

    /** Envia al cliente la vista previa (fantasma) del portapapeles recien establecido. */
    public static void sendPreview(ServerPlayer player, ClipboardManager.Clipboard clip) {
        if (clip == null || clip.size() == 0) {
            return;
        }
        com.fantasticterraform.network.PacketHandler.sendToClient(player,
                com.fantasticterraform.network.ClipboardPreviewPacket.fromClipboard(clip, player.level()));
    }
}
