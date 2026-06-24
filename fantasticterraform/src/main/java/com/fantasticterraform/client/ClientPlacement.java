package com.fantasticterraform.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Calcula el ORIGEN de pegado de schematics/portapapeles, compartido por el fantasma de
 * previsualizacion y por el pegado real, de modo que el resultado caiga EXACTAMENTE donde
 * se ve el fantasma. El origen = ancla (tu posicion, o el bloque que miras si
 * {@code pasteAtLook}) desplazada por el offset X/Y/Z del HUD.
 */
public final class ClientPlacement {

    private static final double REACH = 256.0D;

    private ClientPlacement() {
    }

    /** Punto de anclaje antes de aplicar el desplazamiento. */
    public static BlockPos anchor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return BlockPos.ZERO;
        }
        if (ClientToolState.pasteAtLook && mc.level != null) {
            Vec3 eye = mc.player.getEyePosition(1.0F);
            Vec3 view = mc.player.getViewVector(1.0F);
            Vec3 end = eye.add(view.x * REACH, view.y * REACH, view.z * REACH);
            ClipContext ctx = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);
            BlockHitResult hit = mc.level.clip(ctx);
            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getBlockPos();
            }
        }
        return mc.player.blockPosition();
    }

    /** Origen final de pegado = ancla + desplazamiento del HUD. */
    public static BlockPos origin() {
        return anchor().offset(ClientToolState.pasteOffsetX, ClientToolState.pasteOffsetY, ClientToolState.pasteOffsetZ);
    }
}
