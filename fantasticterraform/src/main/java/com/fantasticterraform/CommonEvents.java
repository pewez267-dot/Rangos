package com.fantasticterraform;

import com.fantasticterraform.ambience.AmbienceManager;
import com.fantasticterraform.brushes.BrushManager;
import com.fantasticterraform.core.EditorCommand;
import com.fantasticterraform.core.EditorModeManager;
import com.fantasticterraform.editing.BlockChangeQueue;
import com.fantasticterraform.editing.ClipboardManager;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.particles.ParticleEmitterManager;
import com.fantasticterraform.selection.SelectionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Eventos del forge-bus (servidor/comun): registro de comandos, procesamiento de la
 * cola por ticks, deteccion de cambio de chunk para particulas/ambiente, restauracion
 * al desconectar y carga de datos persistidos al iniciar el servidor.
 */
@Mod.EventBusSubscriber(modid = FantasticTerraform.MOD_ID)
public final class CommonEvents {

    private static final Map<UUID, Long> LAST_CHUNK = new ConcurrentHashMap<>();

    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EditorCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BlockChangeQueue.tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        long chunkKey = player.chunkPosition().toLong();
        Long previous = LAST_CHUNK.get(player.getUUID());
        if (previous == null || previous != chunkKey) {
            LAST_CHUNK.put(player.getUUID(), chunkKey);
            ParticleEmitterManager.get().updateForPlayer(player);
            AmbienceManager.get().updateForPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        UUID id = player.getUUID();
        EditorModeManager.get().forceRestore(player);
        SelectionManager.remove(player);
        MaskManager.remove(id);
        BrushManager.remove(id);
        ClipboardManager.remove(id);
        ParticleEmitterManager.get().forgetPlayer(id);
        AmbienceManager.get().forgetPlayer(id);
        LAST_CHUNK.remove(id);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ParticleEmitterManager.get().loadAll();
        AmbienceManager.get().loadAll();
    }
}
