package com.fscrates.animation;

import com.fscrates.animation.CrateAnimation.Style;
import com.fscrates.animation.CrateAnimation.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The modular animation catalogue. Durations are intentionally long (5–9
 * seconds) so the reveal builds tension before paying off.
 */
public final class AnimationRegistry {

    private AnimationRegistry() {}

    private static final Map<String, CrateAnimation> REGISTRY = new LinkedHashMap<>();

    static {
        // ---- Roulette / casino ----
        reg("roulette", "Ruleta cl\u00e1sica", Style.ROULETTE, Theme.CLASSIC, 160,
                "Carrusel horizontal que desacelera hasta detenerse en el premio.");
        reg("roulette_neon", "Ruleta ne\u00f3n", Style.ROULETTE, Theme.NEON, 180,
                "Ruleta con estela de luces de ne\u00f3n.");
        reg("slot_machine", "Tragamonedas", Style.SLOT_MACHINE, Theme.CASINO, 180,
                "Tres carretes verticales que paran uno a uno.");
        reg("slot_jackpot", "Jackpot dorado", Style.SLOT_MACHINE, Theme.CASINO, 200,
                "Tragamonedas con lluvia de monedas al ganar.");

        // ---- Spin / item rain / explosion ----
        reg("spin_basic", "Giro simple", Style.SPIN, Theme.CLASSIC, 140,
                "El cofre gira y estalla mostrando el premio.");
        reg("spin_turbo", "Giro turbo", Style.SPIN, Theme.NEON, 120,
                "Giro r\u00e1pido con destellos de velocidad.");
        reg("item_rain", "Lluvia de items", Style.ITEM_RAIN, Theme.CLASSIC, 170,
                "Los premios caen desde arriba y convergen en el ganador.");
        reg("loot_explosion", "Estallido de bot\u00edn", Style.LOOT_EXPLOSION, Theme.CLASSIC, 150,
                "El premio explota hacia afuera y vuelve al centro.");
        reg("loot_volcano", "Erupci\u00f3n infernal", Style.LOOT_EXPLOSION, Theme.INFERNAL, 170,
                "Erupci\u00f3n de lava y brasas con el premio.");
        reg("confetti_burst", "Estallido de confeti", Style.LOOT_EXPLOSION, Theme.CASINO, 150,
                "Confeti de colores por todas partes.");

        // ---- Beam / reveal ----
        reg("beam_reveal", "Haz de luz", Style.BEAM_REVEAL, Theme.CELESTIAL, 160,
                "Un haz vertical de luz revela el premio.");
        reg("beam_holy", "Luz celestial", Style.BEAM_REVEAL, Theme.CELESTIAL, 180,
                "Rayos divinos descienden sobre el premio.");

        // ---- Orbit / cards ----
        reg("orbit_select", "\u00d3rbita de premios", Style.ORBIT, Theme.SCIFI, 180,
                "Los candidatos orbitan y se eliminan hasta dejar al ganador.");
        reg("card_flip", "Carta revelada", Style.CARD_FLIP, Theme.RPG, 140,
                "Una carta boca abajo se voltea revelando el premio.");
        reg("card_tarot", "Tarot m\u00edstico", Style.CARD_FLIP, Theme.MAGIC, 160,
                "Carta de tarot con energ\u00eda arcana.");

        // ---- Shatter / portal ----
        reg("shatter", "Ruptura", Style.SHATTER, Theme.CLASSIC, 150,
                "El cascar\u00f3n del cofre se hace a\u00f1icos.");
        reg("shatter_ice", "Ruptura de hielo", Style.SHATTER, Theme.NATURE, 160,
                "El cofre se congela y se hace a\u00f1icos.");
        reg("portal_open", "Portal dimensional", Style.PORTAL, Theme.SCIFI, 180,
                "Se abre un portal y el premio emerge.");
        reg("portal_ender", "Portal del End", Style.PORTAL, Theme.MAGIC, 190,
                "Portal de energ\u00eda del End con part\u00edculas moradas.");

        // ---- Magic / summon ----
        reg("summon_circle", "C\u00edrculo de invocaci\u00f3n", Style.SUMMON_CIRCLE, Theme.MAGIC, 200,
                "Un c\u00edrculo m\u00e1gico se carga e invoca el premio.");
        reg("summon_runes", "Runas antiguas", Style.SUMMON_CIRCLE, Theme.ANCIENT, 210,
                "Runas brillantes giran y revelan el bot\u00edn.");
        reg("wave_pulse", "Pulso de energ\u00eda", Style.WAVE_PULSE, Theme.SCIFI, 150,
                "Pulsos conc\u00e9ntricos construyen el reveal.");
        reg("galaxy_swirl", "Remolino gal\u00e1ctico", Style.GALAXY_SWIRL, Theme.CELESTIAL, 200,
                "Un remolino de estrellas se condensa en el premio.");

        // ---- Celebratory ----
        reg("fireworks", "Fuegos artificiales", Style.FIREWORKS, Theme.CASINO, 170,
                "Espect\u00e1culo de fuegos artificiales al revelar.");
        reg("fireworks_mega", "Gran final", Style.FIREWORKS, Theme.CELESTIAL, 220,
                "Final apote\u00f3sico con fuegos y haz de luz combinados.");

        reg("instant", "Instant\u00e1neo", Style.INSTANT, Theme.CLASSIC, 1,
                "Sin animaci\u00f3n: entrega inmediata.");
    }

    private static void reg(String id, String name, Style style, Theme theme, int duration, String desc) {
        REGISTRY.put(id, new CrateAnimation(id, name, style, theme, duration, desc));
    }

    public static CrateAnimation register(String id, String name, Style style, Theme theme,
                                          int duration, String desc) {
        CrateAnimation a = new CrateAnimation(id, name, style, theme, duration, desc);
        REGISTRY.put(id, a);
        return a;
    }

    public static CrateAnimation get(String id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get("roulette"));
    }

    public static boolean exists(String id) {
        return REGISTRY.containsKey(id);
    }

    public static List<CrateAnimation> all() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.values()));
    }

    public static List<String> ids() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.keySet()));
    }

    public static String defaultId() {
        return "roulette";
    }
}
