package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.intelligent.population.PopulationManager;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.PopulateSelectionPacket;

/**
 * Pestana de Poblacion: vegetacion, decoracion y vetas de mineral sobre el terreno de la
 * seleccion. Categorias en rejilla compacta; la accion principal al final. Layout 14px.
 */
public final class PopulationPanel implements HudPanel {

    @Override
    public String title() {
        return "Poblacion";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.section(x, row, "VEGETACION");
        row += 11;
        row = toggle(screen, x, row, half, "Arboles", ClientToolState.popTrees, v -> ClientToolState.popTrees = v,
                "Flores", ClientToolState.popFlowers, v -> ClientToolState.popFlowers = v);
        row = toggle(screen, x, row, half, "Hierba", ClientToolState.popGrass, v -> ClientToolState.popGrass = v,
                "Setas", ClientToolState.popMushrooms, v -> ClientToolState.popMushrooms = v);
        row += 2;

        screen.section(x, row, "ENTORNO");
        row += 11;
        row = toggle(screen, x, row, half, "Desierto", ClientToolState.popDesert, v -> ClientToolState.popDesert = v,
                "Agua", ClientToolState.popWater, v -> ClientToolState.popWater = v);
        row = toggle(screen, x, row, half, "Rocas", ClientToolState.popRocks, v -> ClientToolState.popRocks = v,
                "Cristales", ClientToolState.popCrystals, v -> ClientToolState.popCrystals = v);
        row += 2;

        screen.section(x, row, "SUBSUELO");
        row += 11;
        screen.addButton(x, row, width, TerraformPanelScreen.RH, "Vetas de mineral: " + onOff(ClientToolState.popOres),
                () -> ClientToolState.popOres = !ClientToolState.popOres,
                "Esparce minerales en la roca por profundidad (carbon/hierro/oro/diamante/esmeralda).");
        row += TerraformPanelScreen.RS + 2;

        // --- Accion principal (al final, ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7a\u00a7l\u25b6 POBLAR SELECCION", PopulationPanel::populate,
                "Aplica todas las categorias activas sobre el terreno, segun clima y densidad.");
    }

    private static int toggle(TerraformPanelScreen screen, int x, int row, int half,
                              String leftLabel, boolean leftVal, java.util.function.Consumer<Boolean> leftSet,
                              String rightLabel, boolean rightVal, java.util.function.Consumer<Boolean> rightSet) {
        screen.addButton(x, row, half, TerraformPanelScreen.RH, leftLabel + ": " + onOff(leftVal),
                () -> leftSet.accept(!leftVal), "Activa/desactiva " + leftLabel.toLowerCase() + ".");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, rightLabel + ": " + onOff(rightVal),
                () -> rightSet.accept(!rightVal), "Activa/desactiva " + rightLabel.toLowerCase() + ".");
        return row + TerraformPanelScreen.RS;
    }

    private static void populate() {
        int mask = 0;
        mask |= ClientToolState.popTrees ? PopulationManager.TREES : 0;
        mask |= ClientToolState.popFlowers ? PopulationManager.FLOWERS : 0;
        mask |= ClientToolState.popGrass ? PopulationManager.GRASS : 0;
        mask |= ClientToolState.popMushrooms ? PopulationManager.MUSHROOMS : 0;
        mask |= ClientToolState.popDesert ? PopulationManager.DESERT : 0;
        mask |= ClientToolState.popWater ? PopulationManager.WATER : 0;
        mask |= ClientToolState.popRocks ? PopulationManager.ROCKS : 0;
        mask |= ClientToolState.popCrystals ? PopulationManager.CRYSTALS : 0;
        mask |= ClientToolState.popOres ? PopulationManager.ORES : 0;
        PacketHandler.sendToServer(new PopulateSelectionPacket(mask, ClientToolState.genSeed));
    }

    private static String onOff(boolean b) {
        return b ? "Si" : "No";
    }

    @Override
    public String status() {
        return "Activa categorias y pulsa POBLAR SELECCION.";
    }
}
