package com.fscrates.client.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class CrateStyles {
    public static final String AUTO = "";
    private static final Map<String, Style> STYLES = new LinkedHashMap<String, Style>();

    private CrateStyles() {
    }

    private static ResourceLocation rl(String path) {
        return new ResourceLocation("fscrates", "block/" + path);
    }

    private static void reg(String id, String display, String model, float scale) {
        STYLES.put(id, new Style(id, display, CrateStyles.rl(model), null, null, scale));
    }

    private static void regLid(String id, String display, String model, String lidModel, float[] hinge, float scale) {
        STYLES.put(id, new Style(id, display, CrateStyles.rl(model), CrateStyles.rl(lidModel), hinge, scale));
    }

    private static void regCine(String id, String display, String model, String lidModel, float[] hinge, float scale) {
        STYLES.put(id, new Style(id, display, CrateStyles.rl(model), CrateStyles.rl(lidModel), hinge, scale, true));
    }

    public static Style get(String id) {
        return id == null ? null : STYLES.get(id);
    }

    public static Collection<Style> all() {
        return STYLES.values();
    }

    public static List<String> cycleIds() {
        ArrayList<String> ids = new ArrayList<String>();
        ids.add(AUTO);
        ids.addAll(STYLES.keySet());
        return ids;
    }

    public static String displayName(String id) {
        Style s = CrateStyles.get(id);
        return s != null ? s.display : "\u00a77Auto (por rareza)";
    }

    static {
        CrateStyles.regLid("classic_common", "\u00a7fCofre Cl\u00e1sico Com\u00fan", "crate_common", "crate_common_lid", new float[]{0.5f, 0.55052f, 0.79445f}, 1.9301f);
        CrateStyles.regLid("classic_rare", "\u00a7bCofre Cl\u00e1sico Raro", "crate_rare", "crate_rare_lid", new float[]{0.5f, 0.57405f, 0.81105f}, 1.583f);
        CrateStyles.regLid("classic_epic", "\u00a7dCofre Cl\u00e1sico \u00c9pico", "crate_epic", "crate_epic_lid", new float[]{0.5f, 0.52543f, 0.91443f}, 1.7538f);
        CrateStyles.regLid("classic_legendary", "\u00a76Cofre Cl\u00e1sico Legendario", "crate_legendary", "crate_legendary_lid", new float[]{0.47306f, 0.3235f, 0.71127f}, 1.8966f);
        CrateStyles.regLid("classic_mythic", "\u00a7cCofre Cl\u00e1sico M\u00edtico", "crate_mythic", "crate_mythic_lid", new float[]{0.5f, 0.45352f, 0.84924f}, 1.7909f);
        CrateStyles.regLid("crate_lvl1", "\u00a77Cofre de Madera", "crate_lvl1", "crate_lvl1_lid", new float[]{0.5f, 0.5625f, 1.125f}, 0.8299f);
        CrateStyles.regLid("crate_lvl2", "\u00a7eCofre Dorado", "crate_lvl2", "crate_lvl2_lid", new float[]{0.5f, 0.60625f, 1.21609f}, 0.7613f);
        CrateStyles.regLid("crate_lvl3", "\u00a7bCofre de Diamante", "crate_lvl3", "crate_lvl3_lid", new float[]{0.5f, 0.70865f, 1.19063f}, 0.8f);
        CrateStyles.regLid("crate_lvl4", "\u00a7dCofre Arcano", "crate_lvl4", "crate_lvl4_lid", new float[]{0.5f, 0.98539f, 1.59062f}, 0.6047f);
        CrateStyles.regLid("elite_icechest", "\u00a7bCofre de Hielo", "elite_icechest", "elite_icechest_lid", new float[]{0.5f, 0.66601f, 0.90869f}, 0.724f);
        CrateStyles.regLid("elite_lavachest", "\u00a7cCofre de Lava", "elite_lavachest", "elite_lavachest_lid", new float[]{0.5f, 0.5f, 0.90625f}, 0.8122f);
        CrateStyles.regLid("elite_naturechest", "\u00a7aCofre de la Naturaleza", "elite_naturechest", "elite_naturechest_lid", new float[]{0.5f, 0.71014f, 0.79987f}, 0.5935f);
        CrateStyles.regLid("elite_windchest", "\u00a7fCofre del Viento", "elite_windchest", "elite_windchest_lid", new float[]{0.5f, 0.5f, 1.00241f}, 0.7433f);
        CrateStyles.regLid("elite_lovechest", "\u00a7dCofre del Amor", "elite_lovechest", "elite_lovechest_lid", new float[]{0.5f, 0.76326f, 0.72751f}, 1.2242f);
        CrateStyles.regLid("dedou_1", "\u00a7fCofre Rareza Com\u00fan", "dedou_1", "dedou_1_lid", new float[]{0.5f, 0.47396f, 1.17708f}, 1.0615f);
        CrateStyles.regLid("dedou_2", "\u00a7aCofre Rareza Raro", "dedou_2", "dedou_2_lid", new float[]{0.5f, 0.47396f, 1.17708f}, 1.0615f);
        CrateStyles.regLid("dedou_3", "\u00a75Cofre Rareza \u00c9pico", "dedou_3", "dedou_3_lid", new float[]{0.5f, 0.47396f, 1.17708f}, 1.0615f);
        CrateStyles.regLid("dedou_4", "\u00a76Cofre Rareza Legendario", "dedou_4", "dedou_4_lid", new float[]{0.5f, 0.19102f, 0.77289f}, 2.6339f);
        CrateStyles.regLid("dedou_5", "\u00a7bCofre Rareza Divino", "dedou_5", "dedou_5_lid", new float[]{0.5f, 0.47396f, 1.17708f}, 1.0615f);
        CrateStyles.regLid("blackgold_1", "\u00a7eCofre Black & Gold I", "blackgold_1", "blackgold_1_lid", new float[]{0.5f, 0.62725f, 1.20228f}, 0.8047f);
        CrateStyles.regLid("blackgold_2", "\u00a7eCofre Black & Gold II", "blackgold_2", "blackgold_2_lid", new float[]{0.5f, 0.49331f, 1.08036f}, 0.6561f);
        CrateStyles.regLid("blackgold_3", "\u00a7eCofre Black & Gold III", "blackgold_3", "blackgold_3_lid", new float[]{0.5f, 0.47524f, 0.97674f}, 0.8524f);
        CrateStyles.regLid("greek_1b", "\u00a7eCaja Griega I\u00b7A", "greek_1b", "greek_1b_lid", new float[]{0.5f, 0.60938f, 1.3125f}, 0.7946f);
        CrateStyles.regLid("greek_1g", "\u00a7eCaja Griega I\u00b7B", "greek_1g", "greek_1g_lid", new float[]{0.5f, 0.60938f, 1.3125f}, 0.7946f);
        CrateStyles.regLid("greek_1i", "\u00a7eCaja Griega I\u00b7C", "greek_1i", "greek_1i_lid", new float[]{0.5f, 0.60938f, 1.3125f}, 0.7946f);
        CrateStyles.regLid("greek_2b", "\u00a76Caja Griega II\u00b7A", "greek_2b", "greek_2b_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.8939f);
        CrateStyles.regLid("greek_2g", "\u00a76Caja Griega II\u00b7B", "greek_2g", "greek_2g_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.8939f);
        CrateStyles.regLid("greek_2i", "\u00a76Caja Griega II\u00b7C", "greek_2i", "greek_2i_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.8939f);
        CrateStyles.regLid("greek_3b", "\u00a7bCaja Griega III\u00b7A", "greek_3b", "greek_3b_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.8492f);
        CrateStyles.regLid("greek_3g", "\u00a7bCaja Griega III\u00b7B", "greek_3g", "greek_3g_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.8492f);
        CrateStyles.regLid("greek_3i", "\u00a7bCaja Griega III\u00b7C", "greek_3i", "greek_3i_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.8492f);
        CrateStyles.regLid("greek_4b", "\u00a7dCaja Griega IV\u00b7A", "greek_4b", "greek_4b_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.7077f);
        CrateStyles.regLid("greek_4g", "\u00a7dCaja Griega IV\u00b7B", "greek_4g", "greek_4g_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.7077f);
        CrateStyles.regLid("greek_4i", "\u00a7dCaja Griega IV\u00b7C", "greek_4i", "greek_4i_lid", new float[]{0.5f, 0.70664f, 1.22222f}, 0.7077f);
        CrateStyles.regLid("toffy_explosive", "\u00a7cCofre Explosivo", "toffy_explosive", "toffy_explosive_lid", new float[]{0.5f, 0.6875f, 0.9375f}, 1.0222f);
        CrateStyles.regLid("toffy_inhabitant", "\u00a7dCofre Habitante", "toffy_inhabitant", "toffy_inhabitant_lid", new float[]{0.5f, 0.8125f, 0.98331f}, 0.9601f);
        CrateStyles.regLid("toffy_owl", "\u00a7eCofre B\u00faho", "toffy_owl", "toffy_owl_lid", new float[]{0.5f, 0.53791f, 1.3125f}, 0.7935f);
        CrateStyles.regLid("toffy_piano", "\u00a7bCofre Piano", "toffy_piano", "toffy_piano_lid", new float[]{0.5f, 0.53612f, 1.3125f}, 1.1403f);
        CrateStyles.regLid("aquatic_4", "\u00a73Cofre Acu\u00e1tico I", "aquatic_4", "aquatic_4_lid", new float[]{0.5f, 0.48629f, 1.07094f}, 0.8553f);
        CrateStyles.regLid("aquatic_5", "\u00a73Cofre Acu\u00e1tico II", "aquatic_5", "aquatic_5_lid", new float[]{0.5f, 0.53366f, 0.94789f}, 0.7697f);
        CrateStyles.regLid("aquatic_6", "\u00a73Cofre Acu\u00e1tico III", "aquatic_6", "aquatic_6_lid", new float[]{0.5f, 0.79824f, 1.04167f}, 0.7565f);
        CrateStyles.regLid("pirate_1", "\u00a76Cofre Pirata I", "pirate_1", "pirate_1_lid", new float[]{0.5f, 0.34574f, 0.99701f}, 1.3646f);
        CrateStyles.regLid("pirate_2", "\u00a76Cofre Pirata II", "pirate_2", "pirate_2_lid", new float[]{0.5f, 0.51704f, 1.20171f}, 0.7242f);
        CrateStyles.regLid("pirate_3", "\u00a76Cofre Pirata III", "pirate_3", "pirate_3_lid", new float[]{0.5f, 0.3447f, 1.21401f}, 1.0862f);
        CrateStyles.regLid("pirate_4", "\u00a76Cofre Pirata IV", "pirate_4", "pirate_4_lid", new float[]{0.5f, 0.53f, 1.09375f}, 0.9524f);
        CrateStyles.regLid("crates1_lvl1", "\u00a77Cofre Nivel I", "crates1_lvl1", "crates1_lvl1_lid", new float[]{0.5f, 0.5f, 1.0625f}, 1.15f);
        CrateStyles.regLid("crates1_lvl2", "\u00a7eCofre Nivel II", "crates1_lvl2", "crates1_lvl2_lid", new float[]{0.5f, 0.5625f, 1.0625f}, 0.8659f);
        CrateStyles.regLid("crates1_lvl3", "\u00a7bCofre Nivel III", "crates1_lvl3", "crates1_lvl3_lid", new float[]{0.5f, 0.6875f, 1.06228f}, 0.8762f);
        CrateStyles.regLid("crates1_lvl4", "\u00a7dCofre Nivel IV", "crates1_lvl4", "crates1_lvl4_lid", new float[]{0.5f, 0.76875f, 0.9375f}, 0.7863f);
        CrateStyles.regLid("toro_minotaur", "\u00a74Cofre Jefe Minotauro", "toro_minotaur", "toro_minotaur_lid", new float[]{0.5f, 0.41391f, 1.00589f}, 1.0612f);
        CrateStyles.regLid("toro_soulknight", "\u00a78Cofre Jefe Caballero de Almas", "toro_soulknight", "toro_soulknight_lid", new float[]{0.5f, 0.58036f, 1.16741f}, 0.7206f);
        CrateStyles.regLid("toro_xi", "\u00a75Cofre Jefe Xi", "toro_xi", "toro_xi_lid", new float[]{0.5f, 0.58036f, 1.22835f}, 0.9436f);
        CrateStyles.regLid("toro_slimy", "\u00a72Cofre Jefe Slimy", "toro_slimy", "toro_slimy_lid", new float[]{0.5f, 0.4625f, 0.95312f}, 1.2778f);
        CrateStyles.regCine("cine_common", "\u00a7fCofre Cinem\u00e1tico Com\u00fan", "cine_common", "cine_common_lid", new float[]{0.5f, 0.67241f, 1.25646f}, 0.6662f);
        CrateStyles.regCine("cine_rare", "\u00a7bCofre Cinem\u00e1tico Raro", "cine_rare", "cine_rare_lid", new float[]{0.5f, 0.66102f, 1.22988f}, 0.6777f);
        CrateStyles.regCine("cine_epic", "\u00a75Cofre Cinem\u00e1tico \u00c9pico", "cine_epic", "cine_epic_lid", new float[]{0.5f, 0.65f, 1.20594f}, 0.6961f);
        CrateStyles.regCine("cine_legendary", "\u00a76Cofre Cinem\u00e1tico Legendario", "cine_legendary", "cine_legendary_lid", new float[]{0.5f, 0.24127f, 1.21127f}, 0.6961f);
        CrateStyles.regCine("cine_mythical", "\u00a7dCofre Cinem\u00e1tico M\u00edtico", "cine_mythical", "cine_mythical_lid", new float[]{0.5f, 0.51332f, 1.28281f}, 0.6829f);
        CrateStyles.regCine("cine_ultimate", "\u00a7cCofre Cinem\u00e1tico Definitivo", "cine_ultimate", "cine_ultimate_lid", new float[]{0.5f, 0.47963f, 1.10953f}, 0.8267f);
    }

    public static final class Style {
        public final String id;
        public final String display;
        public final ResourceLocation base;
        public final ResourceLocation lid;
        public final float[] hinge;
        public final float scale;
        public final boolean cinematic;

        public Style(String id, String display, ResourceLocation base, ResourceLocation lid, float[] hinge, float scale) {
            this(id, display, base, lid, hinge, scale, false);
        }

        public Style(String id, String display, ResourceLocation base, ResourceLocation lid, float[] hinge, float scale, boolean cinematic) {
            this.id = id;
            this.display = display;
            this.base = base;
            this.lid = lid;
            this.hinge = hinge;
            this.scale = scale;
            this.cinematic = cinematic;
        }

        public boolean hasLid() {
            return this.lid != null;
        }

        public boolean isCinematic() {
            return this.cinematic;
        }
    }
}

