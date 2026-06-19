package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.CreateParticleEmitterPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.RemoveParticleEmitterPacket;
import com.fantasticterraform.particles.client.ClientParticleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Particulas: crear emisores persistentes en la posicion del jugador. */
public final class ParticlesPanel implements HudPanel {

    @Override
    public String title() {
        return "Particulas";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;
        screen.addEditBox(x, row, width, 16, ClientToolState.particleType, s -> ClientToolState.particleType = s);
        row += 20;
        screen.addSlider(x, row, width, 16, "Tasa/s", 1, 200, ClientToolState.particleRate, false,
                v -> ClientToolState.particleRate = v);
        row += 18;
        screen.addSlider(x, row, width, 16, "Radio vis.", 4, 128, ClientToolState.particleRadius, false,
                v -> ClientToolState.particleRadius = v);
        row += 20;
        screen.addSlider(x, row, half, 16, "R", 0, 1, ClientToolState.particleR, false,
                v -> ClientToolState.particleR = v.floatValue());
        screen.addSlider(x + half + 4, row, half, 16, "G", 0, 1, ClientToolState.particleG, false,
                v -> ClientToolState.particleG = v.floatValue());
        row += 18;
        screen.addSlider(x, row, half, 16, "B", 0, 1, ClientToolState.particleB, false,
                v -> ClientToolState.particleB = v.floatValue());
        screen.addEditBox(x + half + 4, row, half, 16, String.valueOf(ClientToolState.particleDuration), s -> {
            try {
                ClientToolState.particleDuration = Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                ClientToolState.particleDuration = -1L;
            }
        });
        row += 22;
        screen.addButton(x, row, half, 18, "Crear aqui", ParticlesPanel::create);
        screen.addButton(x + half + 4, row, half, 18, "Eliminar cercano", ParticlesPanel::removeNearest);
    }

    private static void create() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        PacketHandler.sendToServer(new CreateParticleEmitterPacket(
                mc.player.getX(), mc.player.getY() + 1.0D, mc.player.getZ(),
                ClientToolState.particleType, ClientToolState.particleRate,
                0.0D, 0.02D, 0.0D,
                ClientToolState.particleR, ClientToolState.particleG, ClientToolState.particleB,
                1.0F, ClientToolState.particleRadius, ClientToolState.particleDuration));
    }

    private static void removeNearest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String id = ClientParticleRenderer.nearestId(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (id != null) {
            PacketHandler.sendToServer(new RemoveParticleEmitterPacket(id));
        }
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Tipo: ej. minecraft:flame, minecraft:dust (usa color).", x, y - 12);
    }
}
