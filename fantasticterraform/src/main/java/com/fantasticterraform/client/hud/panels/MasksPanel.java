package com.fantasticterraform.client.hud.panels;

import com.fantasticterraform.client.RegistryLists;
import com.fantasticterraform.client.hud.HudPanel;
import com.fantasticterraform.client.hud.TerraformPanelScreen;
import com.fantasticterraform.masks.MaskManager;
import com.fantasticterraform.network.MaskUpdatePacket;
import com.fantasticterraform.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;

/**
 * Panel de Mascaras: activar/configurar las siete mascaras (se combinan con AND).
 * Los bloques se eligen de listas desplegables. Layout denso de 14px.
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

        screen.section(x, row, "BLOQUES");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Bloque unico: " + onOff(STATE.blockActive), () -> {
            STATE.blockActive = !STATE.blockActive;
            sync();
        }, "Solo afecta el bloque exacto elegido a la derecha.");
        screen.addPicker(x + half + 4, row, half, TerraformPanelScreen.RH,
                () -> STATE.blockId == null ? "minecraft:stone" : STATE.blockId.toString(),
                RegistryLists.blocks(), true, "Bloque exacto de la mascara 'Bloque unico'.", s -> {
                    STATE.blockId = ResourceLocation.tryParse(s);
                    sync();
                });
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Lista (" + STATE.listIds.size() + "): " + onOff(STATE.listActive), () -> {
            STATE.listActive = !STATE.listActive;
            sync();
        }, "Solo afecta los bloques de la lista.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Anadir a lista", () ->
                        screen.openPicker("Anadir a lista", RegistryLists.blocks(), "", true, s -> {
                            ResourceLocation id = ResourceLocation.tryParse(s);
                            if (id != null) {
                                STATE.listIds.add(id);
                                sync();
                            }
                        }),
                "Anade un bloque a la lista de la mascara 'Lista'.");
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Excluir (" + STATE.exclusionIds.size() + "): " + onOff(STATE.exclusionActive), () -> {
            STATE.exclusionActive = !STATE.exclusionActive;
            sync();
        }, "Afecta todo EXCEPTO los bloques de esta lista.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Anadir a excluir", () ->
                        screen.openPicker("Anadir a excluir", RegistryLists.blocks(), "", true, s -> {
                            ResourceLocation id = ResourceLocation.tryParse(s);
                            if (id != null) {
                                STATE.exclusionIds.add(id);
                                sync();
                            }
                        }),
                "Anade un bloque a la lista de exclusion.");
        row += TerraformPanelScreen.RS + 2;

        screen.section(x, row, "ALTURA Y EXPOSICION");
        row += 11;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Altura: " + onOff(STATE.heightActive), () -> {
            STATE.heightActive = !STATE.heightActive;
            sync();
        }, "Solo afecta bloques dentro del rango de Y.");
        row += TerraformPanelScreen.RS;
        screen.addSlider(x, row, half, TerraformPanelScreen.RH, "Y min", -64, 320, STATE.heightMin, true,
                "Altura minima afectada.", v -> {
                    STATE.heightMin = v.intValue();
                    sync();
                });
        screen.addSlider(x + half + 4, row, half, TerraformPanelScreen.RH, "Y max", -64, 320, STATE.heightMax, true,
                "Altura maxima afectada.", v -> {
                    STATE.heightMax = v.intValue();
                    sync();
                });
        row += TerraformPanelScreen.RS;
        screen.addButton(x, row, half, TerraformPanelScreen.RH, "Solo aire: " + onOff(STATE.airOnlyActive), () -> {
            STATE.airOnlyActive = !STATE.airOnlyActive;
            sync();
        }, "Solo coloca donde ahora hay aire.");
        screen.addButton(x + half + 4, row, half, TerraformPanelScreen.RH, "Cielo: " + onOff(STATE.skyExposedActive), () -> {
            STATE.skyExposedActive = !STATE.skyExposedActive;
            sync();
        }, "Solo afecta bloques con vision directa al cielo.");
        row += TerraformPanelScreen.RS + 2;

        // --- Accion destructiva (unica de ancho completo) ---
        screen.addButton(x, row, width, TerraformPanelScreen.ACTION_H, "\u00a7cLimpiar listas", () -> {
            STATE.listIds.clear();
            STATE.exclusionIds.clear();
            sync();
        }, "Vacia las listas de 'Lista' y 'Excluir'.");
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
        return b ? "ON" : "OFF";
    }

    @Override
    public String status() {
        return "Las mascaras activas se combinan (AND) y limitan donde actuan los brushes y operaciones.";
    }
}
