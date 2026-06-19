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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Eventos del forge-bus del lado cliente: wireframe, tick de particulas, teclas y
 * raycasting manual de la varita (en modo espectador los eventos de bloque no se
 * disparan, asi que se hace clip() desde la mirada del jugador).
 */
@Mod.EventBusSubscriber(modid = FantasticTerraform.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {

    private static final double WAND_REACH = 256.0D;

    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        WireframeRenderer.onRenderLevel(event);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (Keybinds.OPEN_PANELS.consumeClick()) {
            ClientHudController.togglePanelMode();
        }
        while (Keybinds.TOGGLE_WAND.consumeClick()) {
            ClientToolState.wandMode = ClientToolState.wandMode == ClientToolState.WandMode.SELECT
                    ? ClientToolState.WandMode.BRUSH : ClientToolState.WandMode.SELECT;
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
        BlockPos pos = raycast(mc);
        if (pos == null) {
            return;
        }
        if (ClientToolState.wandMode == ClientToolState.WandMode.SELECT) {
            PacketHandler.sendToServer(new SetSelectionPointPacket(left, pos));
        } else if (left) {
            PacketHandler.sendToServer(new BrushApplyPacket(pos, ClientToolState.brushId, ClientToolState.brushRadius,
                    ClientToolState.brushIntensity, ClientToolState.brushHeight, ClientToolState.brushBlock));
        }
        event.setCanceled(true);
    }

    private static BlockPos raycast(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Vec3 view = mc.player.getViewVector(1.0F);
        Vec3 end = eye.add(view.x * WAND_REACH, view.y * WAND_REACH, view.z * WAND_REACH);
        ClipContext context = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult hit = mc.level.clip(context);
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }
}
