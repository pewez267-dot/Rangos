package com.fantasticterraform.client;

import com.fantasticterraform.FantasticTerraform;
import com.fantasticterraform.network.BrushApplyPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.SetSelectionPointPacket;
import com.fantasticterraform.particles.client.ClientParticleRenderer;
import com.fantasticterraform.selection.SelectionWand;
import com.fantasticterraform.selection.client.WireframeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Eventos del forge-bus del lado cliente: wireframe, tick de particulas, teclas y la
 * varita. La seleccion funciona por ARRASTRE: click izquierdo fija el ancla y mientras
 * mueves la mira el contorno se previsualiza en vivo; click derecho lo suelta/confirma.
 * El raycasting es manual ({@code clip()} desde la mirada) porque en espectador no se
 * disparan los eventos de bloque.
 */
@Mod.EventBusSubscriber(modid = FantasticTerraform.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private static final double WAND_REACH = 256.0D;

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        WireframeRenderer.onRenderLevel(event);
        com.fantasticterraform.client.ClientGhostRenderer.onRenderLevel(event);
    }

    @SubscribeEvent
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        // Al salir de un mundo, olvidar emisores/ambiente del cliente para no arrastrarlos al siguiente.
        com.fantasticterraform.particles.client.ClientParticleRenderer.clear();
        com.fantasticterraform.ambience.client.ClientAmbiencePlayer.stopAll();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (Keybinds.OPEN_PANELS.consumeClick()) {
            ClientHudController.togglePanelMode();
        }
        while (Keybinds.TOGGLE_WAND.consumeClick()) {
            ClientToolState.wandMode = ClientToolState.wandMode == ClientToolState.WandMode.SELECT
                    ? ClientToolState.WandMode.BRUSH : ClientToolState.WandMode.SELECT;
        }
        // Previsualizacion en vivo: TU posicion es el punto B (tu defines altura y distancia).
        if (ClientDragState.isActive()) {
            BlockPos self = Minecraft.getInstance().player != null
                    ? Minecraft.getInstance().player.blockPosition() : null;
            if (self != null) {
                ClientDragState.updatePreview(self);
            }
        }
        ClientParticleRenderer.tick();
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.level == null) {
            return;
        }
        if (!SelectionWand.isWand(mc.player.getMainHandItem())) {
            return;
        }
        boolean left = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean right = event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        if (!left && !right) {
            return;
        }

        if (ClientToolState.wandMode == ClientToolState.WandMode.BRUSH) {
            // El brush apunta a donde miras.
            BlockPos pos = raycast(mc);
            if (left && pos != null) {
                PacketHandler.sendToServer(new BrushApplyPacket(pos, ClientToolState.brushId, ClientToolState.brushRadius,
                        ClientToolState.brushIntensity, ClientToolState.brushHeight, ClientToolState.brushBlock,
                        ClientToolState.brushFalloff, ClientToolState.brushSecondaryBlock, ClientToolState.brushMix,
                        ClientToolState.brushDepth, ClientToolState.brushHollow));
            }
            event.setCanceled(true);
            return;
        }

        // SELECCION inteligente (SMART): el click izquierdo lanza el flood-fill.
        if (ClientSelectionState.type() == com.fantasticterraform.selection.SelectionType.SMART) {
            BlockPos target = raycast(mc);
            if (left && target != null) {
                PacketHandler.sendToServer(new com.fantasticterraform.network.SmartSelectPacket(
                        target, ClientToolState.smartMaxBlocks, ClientToolState.smartDiagonal));
            }
            event.setCanceled(true);
            return;
        }

        // SELECCION normal: el punto es tu MIRADA (si selectAtLook) o tu POSICION.
        BlockPos look = raycast(mc);
        BlockPos self = (ClientToolState.selectAtLook && look != null) ? look : mc.player.blockPosition();
        boolean multiPoint = ClientSelectionState.type().isMultiPoint();
        if (left) {
            if (multiPoint) {
                // Poligono/freehand: cada click izquierdo anade un vertice en tu posicion.
                PacketHandler.sendToServer(new SetSelectionPointPacket(true, self));
            } else {
                // 2 puntos: fija el ancla (A) en tu posicion y empieza el arrastre; B te seguira.
                ClientDragState.begin(self);
                PacketHandler.sendToServer(new SetSelectionPointPacket(true, self));
            }
        } else {
            if (multiPoint) {
                PacketHandler.sendToServer(new SetSelectionPointPacket(false, self));
            } else {
                // Soltar: confirma B donde estas ahora.
                PacketHandler.sendToServer(new SetSelectionPointPacket(false, self));
                ClientDragState.end();
            }
        }
        event.setCanceled(true);
    }

    private static BlockPos raycast(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Vec3 view = mc.player.getViewVector(1.0F);
        Vec3 end = eye.add(view.x * WAND_REACH, view.y * WAND_REACH, view.z * WAND_REACH);
        ClipContext context = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult hit = mc.level.clip(context);
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }
}
