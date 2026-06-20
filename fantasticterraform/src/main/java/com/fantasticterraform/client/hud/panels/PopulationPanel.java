package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.ClientToolState;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.intelligent.population.PopulationManager;
import com.fantasticterraform.network.PacketHandler;
import com.fantasticterraform.network.PopulateSelectionPacket;

/**
 * Pestaña de Población: añade vegetación, decoración y vetas de mineral sobre el terreno
 * de la selección. Diseño limpio: categorías en dos columnas y una acción principal.
 */
public final class PopulationPanel implements HudPanel {

    @Override
    public String title() {
        return "Población";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 6) / 2;
        int row = y;

        screen.addButton(x, row, width, 20, "\u00a7a\u00a7l\u25b6 POBLAR SELECCIÓN", PopulationPanel::populate,
                "Aplica todas las categorías activas sobre el terreno, según clima y densidad.");
        row += 26;

        screen.addHeader(x, row, width, "VEGETACIÓN");
        row += 13;
        row = toggle(screen, x, row, half, "Árboles", ClientToolState.popTrees, v -> ClientToolState.popTrees = v,
                "Flores", ClientToolState.popFlowers, v -> ClientToolState.popFlowers = v);
        row = toggle(screen, x, row, half, "Hierba", ClientToolState.popGrass, v -> ClientToolState.popGrass = v,
                "Setas", ClientToolState.popMushrooms, v -> ClientToolState.popMushrooms = v);
        row += 6;

        screen.addHeader(x, row, width, "ENTORNO");
        row += 13;
        row = toggle(screen, x, row, half, "Desierto", ClientToolState.popDesert, v -> ClientToolState.popDesert = v,
                "Agua", ClientToolState.popWater, v -> ClientToolState.popWater = v);
        row = toggle(screen, x, row, half, "Rocas", ClientToolState.popRocks, v -> ClientToolState.popRocks = v,
                "Cristales", ClientToolState.popCrystals, v -> ClientToolState.popCrystals = v);
        row += 6;

        screen.addHeader(x, row, width, "SUBSUELO");
        row += 13;
        screen.addButton(x, row, width, 18, "Vetas de mineral: " + onOff(ClientToolState.popOres),
                () -> ClientToolState.popOres = !ClientToolState.popOres,
                "Esparce minerales en la roca por profundidad (carbón/hierro/oro/diamante/esmeralda, con deepslate).");
    }

    private static int toggle(TerraformPanelScreen screen, int x, int row, int half,
                              String leftLabel, boolean leftVal, java.util.function.Consumer<Boolean> leftSet,
                              String rightLabel, boolean rightVal, java.util.function.Consumer<Boolean> rightSet) {
        screen.addButton(x, row, half, 18, leftLabel + ": " + onOff(leftVal),
                () -> leftSet.accept(!leftVal), "Activa/desactiva " + leftLabel.toLowerCase() + ".");
        screen.addButton(x + half + 6, row, half, 18, rightLabel + ": " + onOff(rightVal),
                () -> rightSet.accept(!rightVal), "Activa/desactiva " + rightLabel.toLowerCase() + ".");
        return row + 20;
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
        return b ? "\u00a7aSí" : "\u00a77No";
    }

    @Override
    public String status() {
        return "Activa categorías y pulsa POBLAR SELECCIÓN.";
    }
}
