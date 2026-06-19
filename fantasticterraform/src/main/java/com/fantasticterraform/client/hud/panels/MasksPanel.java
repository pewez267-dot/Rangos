package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.network.MaskUpdatePacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Panel de Mascaras: activar/desactivar y configurar las siete mascaras. Se combinan
 * con AND al aplicar edicion, brushes o terreno. Cada cambio se sincroniza al servidor.
 */
public final class MasksPanel implements HudPanel {

    private static final MaskManager.MaskSettings STATE = new MaskManager.MaskSettings();

    @Override
    public String title() {
        return "Mascaras";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addButton(x, row, half, 18, "Bloque: " + onOff(STATE.blockActive), () -> {
            STATE.blockActive = !STATE.blockActive;
            sync();
        });
        screen.addEditBox(x + half + 4, row, half, 16, STATE.blockId == null ? "minecraft:stone" : STATE.blockId.toString(),
                s -> {
                    STATE.blockId = ResourceLocation.tryParse(s.trim());
                    sync();
                });
        row += 22;

        screen.addButton(x, row, half, 18, "Lista: " + onOff(STATE.listActive), () -> {
            STATE.listActive = !STATE.listActive;
            sync();
        });
        screen.addEditBox(x + half + 4, row, half, 16, joinIds(STATE), s -> {
            STATE.listIds.clear();
            parseInto(s, STATE.listIds);
            sync();
        });
        row += 22;

        screen.addButton(x, row, half, 18, "Excluir: " + onOff(STATE.exclusionActive), () -> {
            STATE.exclusionActive = !STATE.exclusionActive;
            sync();
        });
        screen.addEditBox(x + half + 4, row, half, 16, "", s -> {
            STATE.exclusionIds.clear();
            parseInto(s, STATE.exclusionIds);
            sync();
        });
        row += 22;

        screen.addButton(x, row, half, 18, "Altura: " + onOff(STATE.heightActive), () -> {
            STATE.heightActive = !STATE.heightActive;
            sync();
        });
        row += 20;
        screen.addSlider(x, row, half, 16, "Y min", -64, 320, STATE.heightMin, true, v -> {
            STATE.heightMin = v.intValue();
            sync();
        });
        screen.addSlider(x + half + 4, row, half, 16, "Y max", -64, 320, STATE.heightMax, true, v -> {
            STATE.heightMax = v.intValue();
            sync();
        });
        row += 22;

        screen.addButton(x, row, half, 18, "Solo aire: " + onOff(STATE.airOnlyActive), () -> {
            STATE.airOnlyActive = !STATE.airOnlyActive;
            sync();
        });
        screen.addButton(x + half + 4, row, half, 18, "Cielo: " + onOff(STATE.skyExposedActive), () -> {
            STATE.skyExposedActive = !STATE.skyExposedActive;
            sync();
        });
    }

    private static void sync() {
        MaskManager.MaskSettings copy = new MaskManager.MaskSettings();
        copy.blockActive = STATE.blockActive;
        copy.blockId = STATE.blockId;
        copy.listActive = STATE.listActive;
        copy.listIds.addAll(STATE.listIds);
        copy.exclusionActive = STATE.exclusionActive;
        copy.exclusionIds.addAll(STATE.exclusionIds);
        copy.heightActive = STATE.heightActive;
        copy.heightMin = STATE.heightMin;
        copy.heightMax = STATE.heightMax;
        copy.airOnlyActive = STATE.airOnlyActive;
        copy.skyExposedActive = STATE.skyExposedActive;
        PacketHandler.sendToServer(new MaskUpdatePacket(copy));
    }

    private static void parseInto(String s, java.util.List<ResourceLocation> target) {
        for (String part : s.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse(part.trim());
            if (id != null) {
                target.add(id);
            }
        }
    }

    private static String joinIds(MaskManager.MaskSettings s) {
        StringBuilder sb = new StringBuilder();
        for (ResourceLocation id : s.listIds) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id.toString());
        }
        return sb.toString();
    }

    private static String onOff(boolean b) {
        return b ? "\u00a7aON" : "\u00a77OFF";
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
        screen.drawLabel(g, "Las mascaras activas se combinan con AND.", x, y - 12);
    }
}
