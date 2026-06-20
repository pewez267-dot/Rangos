package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.network.MaskUpdatePacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Panel de Máscaras: activar/desactivar y configurar las siete máscaras (se combinan
 * con AND). Los bloques se eligen de listas desplegables, sin escribir.
 */
public final class MasksPanel implements HudPanel {

    private static final MaskManager.MaskSettings STATE = new MaskManager.MaskSettings();

    @Override
    public String title() {
        return "Máscaras";
    }

    @Override
    public void build(TerraformPanelScreen screen, int x, int y, int width, int height) {
        int half = (width - 4) / 2;
        int row = y;

        screen.addButton(x, row, half, 18, "Bloque: " + onOff(STATE.blockActive), () -> {
            STATE.blockActive = !STATE.blockActive;
            sync();
        }, "Solo afecta el bloque exacto elegido a la derecha.");
        screen.addPicker(x + half + 4, row, half, 18, "Bloque",
                () -> STATE.blockId == null ? "minecraft:stone" : STATE.blockId.toString(),
                RegistryLists.blocks(), true, "Bloque exacto de la máscara 'Bloque unico'.", s -> {
                    STATE.blockId = ResourceLocation.tryParse(s);
                    sync();
                });
        row += 22;

        screen.addButton(x, row, half, 18, "Lista (" + STATE.listIds.size() + "): " + onOff(STATE.listActive), () -> {
            STATE.listActive = !STATE.listActive;
            sync();
        }, "Solo afecta los bloques de la lista. Anade con el boton de al lado.");
        screen.addButton(x + half + 4, row, half, 18, "Anadir a lista", () ->
                        screen.openPicker("Anadir a lista", RegistryLists.blocks(), "", true, s -> {
                            ResourceLocation id = ResourceLocation.tryParse(s);
                            if (id != null) {
                                STATE.listIds.add(id);
                                sync();
                            }
                        }),
                "Anade un bloque a la lista de la máscara 'Lista'.");
        row += 22;

        screen.addButton(x, row, half, 18, "Excluir (" + STATE.exclusionIds.size() + "): " + onOff(STATE.exclusionActive), () -> {
            STATE.exclusionActive = !STATE.exclusionActive;
            sync();
        }, "Afecta todo EXCEPTO los bloques de esta lista.");
        screen.addButton(x + half + 4, row, half, 18, "Anadir a excluir", () ->
                        screen.openPicker("Anadir a excluir", RegistryLists.blocks(), "", true, s -> {
                            ResourceLocation id = ResourceLocation.tryParse(s);
                            if (id != null) {
                                STATE.exclusionIds.add(id);
                                sync();
                            }
                        }),
                "Anade un bloque a la lista de exclusion.");
        row += 22;

        screen.addButton(x, row, width, 18, "Limpiar listas", () -> {
            STATE.listIds.clear();
            STATE.exclusionIds.clear();
            sync();
        }, "Vacia las listas de 'Lista' y 'Excluir'.");
        row += 20;

        screen.addButton(x, row, half, 18, "Altura: " + onOff(STATE.heightActive), () -> {
            STATE.heightActive = !STATE.heightActive;
            sync();
        }, "Solo afecta bloques dentro del rango de Y de abajo.");
        row += 20;
        screen.addSlider(x, row, half, 16, "Y min", -64, 320, STATE.heightMin, true,
                "Altura mínima afectada.", v -> {
                    STATE.heightMin = v.intValue();
                    sync();
                });
        screen.addSlider(x + half + 4, row, half, 16, "Y max", -64, 320, STATE.heightMax, true,
                "Altura máxima afectada.", v -> {
                    STATE.heightMax = v.intValue();
                    sync();
                });
        row += 22;

        screen.addButton(x, row, half, 18, "Solo aire: " + onOff(STATE.airOnlyActive), () -> {
            STATE.airOnlyActive = !STATE.airOnlyActive;
            sync();
        }, "Solo coloca donde ahora hay aire.");
        screen.addButton(x + half + 4, row, half, 18, "Cielo: " + onOff(STATE.skyExposedActive), () -> {
            STATE.skyExposedActive = !STATE.skyExposedActive;
            sync();
        }, "Solo afecta bloques con vision directa al cielo.");
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

    private static String onOff(boolean b) {
        return b ? "\u00a7aON" : "\u00a77OFF";
    }

    @Override
    public void renderExtra(TerraformPanelScreen screen, GuiGraphics g, int x, int y, int width, int height) {
    }
}
