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

/** Panel de Particulas: crear emisores persistentes. Configuracion primero, accion al final. Layout 14px. */
public final class ParticlesPanel implements HudPanel {

    private static final String[] CURVES = {"Constante", "Pulso", "Rampa", "Parpadeo"};
    private static final String[] SHAPES = {"Punto", "Anillo", "Cono", "Esfera", "Disco"};

    @Override
    public String title() {
        return "Particulas";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int third = (width - 8) / 3;
        int row = y;

        // --- Emisor ---
        screen.section(x, row, "EMISOR");
        row += 11;
        screen.addRow(x, row, width, "Particula", screen.addPicker(x, row, 220, TerraformPanelScreen.RH,
                () -> ClientToolState.particleType, RegistryLists.particles(), false,
                "Tipo de particula (todas las del juego + mods).", s -> ClientToolState.particleType = s));
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Tasa/s", 1, 200, ClientToolState.particleRate, false,
                "Particulas emitidas por segundo.", v -> ClientToolState.particleRate = v);
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Radio vis.", 4, 128, ClientToolState.particleRadius, false,
                "Distancia a la que el emisor es visible.", v -> ClientToolState.particleRadius = v);
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.RH,
                "Duracion: " + (ClientToolState.particleDuration < 0 ? "infinita" : "60s"),
                () -> ClientToolState.particleDuration = ClientToolState.particleDuration < 0 ? 1200L : -1L,
                "Alterna duracion infinita / 60s (1200 ticks).");
        row += TerraformPanelScreen.RS + 2;

        // --- Color (solo dust) ---
        screen.section(x, row, "COLOR (solo particulas 'dust')");
        row += 11;
        screen.addSlider(x, row, third, TerraformPanelScreen.RH, "R", 0, 1, ClientToolState.particleR, false,
                "Componente rojo.", v -> ClientToolState.particleR = v.floatValue());
        screen.addSlider(x + third + 4, row, third, TerraformPanelScreen.RH, "G", 0, 1, ClientToolState.particleG, false,
                "Componente verde.", v -> ClientToolState.particleG = v.floatValue());
        screen.addSlider(x + 2 * (third + 4), row, third, TerraformPanelScreen.RH, "B", 0, 1, ClientToolState.particleB, false,
                "Componente azul.", v -> ClientToolState.particleB = v.floatValue());
        row += TerraformPanelScreen.RS + 2;

        // --- Emision y forma ---
        screen.section(x, row, "EMISION Y FORMA");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Curva: " + CURVES[clamp(ClientToolState.particleCurve, CURVES.length)],
                () -> ClientToolState.particleCurve = (ClientToolState.particleCurve + 1) % CURVES.length,
                "Como varia la emision en el tiempo: Constante, Pulso, Rampa o Parpadeo.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Forma: " + SHAPES[clamp(ClientToolState.particleShape, SHAPES.length)],
                () -> ClientToolState.particleShape = (ClientToolState.particleShape + 1) % SHAPES.length,
                "Forma del emisor: Punto, Anillo, Cono, Esfera o Disco.");
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Radio forma", 1, 24, ClientToolState.particleShapeRadius, false,
                "Radio del anillo/cono/esfera/disco.", v -> ClientToolState.particleShapeRadius = v);
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Altura cono", 1, 24, ClientToolState.particleShapeHeight, false,
                "Altura del cono.", v -> ClientToolState.particleShapeHeight = v);
        row += TerraformPanelScreen.RS + 2;

        // --- Acciones ---
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "\u00a7cEliminar emisor mas cercano", ParticlesPanel::removeNearest,
                "Borra el emisor de particulas mas cercano a ti.");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7a\u00a7l\u25b6 CREAR EMISOR AQUI", ParticlesPanel::create,
                "Crea el emisor de particulas en tu posicion con los ajustes de arriba.");
    }

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
    public String status() {
        return "Configura el emisor y pulsa CREAR EMISOR AQUI (se coloca en tu posicion).";
    }
}
