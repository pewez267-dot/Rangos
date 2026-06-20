package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.network.CreateParticleEmitterPacket;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.RemoveParticleEmitterPacket;
import com.fantasticterraform.particles.client.ClientParticleRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Panel de Partículas: crear emisores persistentes eligiendo el tipo de una lista. */
public final class ParticlesPanel implements HudPanel {

    @Override
    public String title() {
        return "Partículas";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 6) / 2;
        int row = y;

        screen.addButton(x, row, width, 20, "\u00a7a\u00a7l\u25b6 CREAR EMISOR AQUÍ", ParticlesPanel::create,
                "Crea el emisor de partículas en tu posición con los ajustes de abajo.");
        row += 26;

        screen.addHeader(x, row, width, "EMISOR");
        row += 13;
        screen.addPicker(x, row, width, 18, "Partícula", () -> ClientToolState.particleType,
                RegistryLists.particles(), false,
                "Tipo de partícula (todas las del juego + mods).",
                s -> ClientToolState.particleType = s);
        row += 20;
        screen.addSlider(x, row, half, 16, "Tasa/s", 1, 200, ClientToolState.particleRate, false,
                "Partículas emitidas por segundo.", v -> ClientToolState.particleRate = v);
        screen.addSlider(x + half + 6, row, half, 16, "Radio vis.", 4, 128, ClientToolState.particleRadius, false,
                "Distancia a la que el emisor es visible.", v -> ClientToolState.particleRadius = v);
        row += 18;
        screen.addButton(x, row, width, 18,
                "Duración: \u00a7f" + (ClientToolState.particleDuration < 0 ? "infinita" : "60s"), () ->
                        ClientToolState.particleDuration = ClientToolState.particleDuration < 0 ? 1200L : -1L,
                "Alterna duración infinita / 60s (1200 ticks).");
        row += 24;

        screen.addHeader(x, row, width, "COLOR (solo partículas 'dust')");
        row += 13;
        int third = (width - 10) / 3;
        screen.addSlider(x, row, third, 16, "R", 0, 1, ClientToolState.particleR, false,
                "Componente rojo.", v -> ClientToolState.particleR = v.floatValue());
        screen.addSlider(x + third + 4, row, third, 16, "G", 0, 1, ClientToolState.particleG, false,
                "Componente verde.", v -> ClientToolState.particleG = v.floatValue());
        screen.addSlider(x + 2 * (third + 4), row, third, 16, "B", 0, 1, ClientToolState.particleB, false,
                "Componente azul.", v -> ClientToolState.particleB = v.floatValue());
        row += 24;

        screen.addHeader(x, row, width, "EMISIÓN Y FORMA");
        row += 13;
        screen.addButton(x, row, half, 18, "Curva: \u00a7f" + CURVES[clamp(ClientToolState.particleCurve, CURVES.length)],
                () -> ClientToolState.particleCurve = (ClientToolState.particleCurve + 1) % CURVES.length,
                "Cómo varía la emisión en el tiempo: Constante, Pulso, Rampa o Parpadeo.");
        screen.addButton(x + half + 6, row, half, 18, "Forma: \u00a7f" + SHAPES[clamp(ClientToolState.particleShape, SHAPES.length)],
                () -> ClientToolState.particleShape = (ClientToolState.particleShape + 1) % SHAPES.length,
                "Forma del emisor: Punto, Anillo, Cono, Esfera o Disco.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Radio forma", 1, 24, ClientToolState.particleShapeRadius, false,
                "Radio del anillo/cono/esfera/disco.", v -> ClientToolState.particleShapeRadius = v);
        screen.addSlider(x + half + 6, row, half, 16, "Altura cono", 1, 24, ClientToolState.particleShapeHeight, false,
                "Altura del cono.", v -> ClientToolState.particleShapeHeight = v);
        row += 24;

        screen.addButton(x, row, width, 18, "Eliminar emisor más cercano", ParticlesPanel::removeNearest,
                "Borra el emisor de partículas más cercano a ti.");
    }

    private static final String[] CURVES = {"Constante", "Pulso", "Rampa", "Parpadeo"};
    private static final String[] SHAPES = {"Punto", "Anillo", "Cono", "Esfera", "Disco"};

    private static int clamp(int v, int len) {
        return (v >= 0 && v < len) ? v : 0;
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
                1.0F, ClientToolState.particleRadius, ClientToolState.particleDuration,
                ClientToolState.particleCurve, ClientToolState.particleShape,
                ClientToolState.particleShapeRadius, ClientToolState.particleShapeHeight));
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
    }
}
