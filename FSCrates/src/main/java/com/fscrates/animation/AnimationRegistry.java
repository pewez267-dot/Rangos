package com.fscrates.animation;

import com.fscrates.animation.CrateAnimation.Style;
import com.fscrates.animation.CrateAnimation.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The modular, practically-unlimited animation catalogue. New animations are
 * added by calling {@link #register} — no other code needs to change. Other
 * mods/addons can also register their own at mod-setup time, making this a
 * lightweight "animation plugin" system.
 *
 * <p>The id is what gets stored in a crate's NBT. {@code combo} animations are
 * built by chaining several base animations, which the client plays in order.
 */
public final class AnimationRegistry {

    private AnimationRegistry() {}

    private static final Map<String, CrateAnimation> REGISTRY = new LinkedHashMap<>();

    static {
        // ---- Classic / casino ----
        reg("roulette", "Ruleta Cl\u00e1sica", Style.ROULETTE, Theme.CLASSIC, 70,
                "Carrusel horizontal que desacelera hasta detenerse en el premio.");
        reg("roulette_neon", "Ruleta Ne\u00f3n", Style.ROULETTE, Theme.NEON, 80,
                "Ruleta con estela de luces de ne\u00f3n.");
        reg("slot_machine", "Tragamonedas", Style.SLOT_MACHINE, Theme.CASINO, 90,
                "Tres carretes verticales al estilo casino.");
        reg("slot_jackpot", "Jackpot Dorado", Style.SLOT_MACHINE, Theme.CASINO, 110,
                "Tragamonedas con lluvia de monedas al ganar.");
        reg("spin_basic", "Giro Simple", Style.SPIN, Theme.CLASSIC, 50,
                "La crate gira y estalla mostrando el premio.");
        reg("spin_turbo", "Giro Turbo", Style.SPIN, Theme.NEON, 40,
                "Giro r\u00e1pido con destellos de velocidad.");

        // ---- Item rain / explosion ----
        reg("item_rain", "Lluvia de Items", Style.ITEM_RAIN, Theme.CLASSIC, 80,
                "Los premios caen desde arriba.");
        reg("loot_explosion", "Explosi\u00f3n de Bot\u00edn", Style.LOOT_EXPLOSION, Theme.CLASSIC, 60,
                "El premio explota hacia afuera desde la crate.");
        reg("loot_volcano", "Erupci\u00f3n Infernal", Style.LOOT_EXPLOSION, Theme.INFERNAL, 75,
                "Erupci\u00f3n de lava y brasas con el premio.");
        reg("confetti_burst", "Estallido de Confeti", Style.LOOT_EXPLOSION, Theme.CASINO, 65,
                "Confeti de colores por todas partes.");

        // ---- Beam / reveal ----
        reg("beam_reveal", "Haz de Luz", Style.BEAM_REVEAL, Theme.CELESTIAL, 70,
                "Un haz vertical de luz revela el premio.");
        reg("beam_rainbow", "Haz Arco\u00edris", Style.BEAM_REVEAL, Theme.NEON, 80,
                "Haz multicolor que cambia de tono.");
        reg("beam_holy", "Luz Celestial", Style.BEAM_REVEAL, Theme.CELESTIAL, 95,
                "Rayos divinos descienden sobre el premio.");

        // ---- Orbit / cards ----
        reg("orbit_select", "\u00d3rbita de Premios", Style.ORBIT, Theme.SCIFI, 85,
                "Los candidatos orbitan hasta elegir uno.");
        reg("card_flip", "Carta Revelada", Style.CARD_FLIP, Theme.RPG, 55,
                "Una carta boca abajo se voltea revelando el premio.");
        reg("card_tarot", "Tarot M\u00edstico", Style.CARD_FLIP, Theme.MAGIC, 70,
                "Carta de tarot con energ\u00eda arcana.");

        // ---- Shatter / portal ----
        reg("shatter", "Ruptura", Style.SHATTER, Theme.CLASSIC, 60,
                "El cascar\u00f3n de la crate se rompe en pedazos.");
        reg("shatter_ice", "Ruptura de Hielo", Style.SHATTER, Theme.NATURE, 70,
                "La crate se congela y se hace a\u00f1icos.");
        reg("portal_open", "Portal Dimensional", Style.PORTAL, Theme.SCIFI, 90,
                "Se abre un portal y el premio emerge.");
        reg("portal_ender", "Portal del End", Style.PORTAL, Theme.MAGIC, 95,
                "Portal de energ\u00eda del End con part\u00edculas moradas.");

        // ---- Magic / summon ----
        reg("summon_circle", "C\u00edrculo de Invocaci\u00f3n", Style.SUMMON_CIRCLE, Theme.MAGIC, 100,
                "Un c\u00edrculo m\u00e1gico se carga e invoca el premio.");
        reg("summon_runes", "Runas Antiguas", Style.SUMMON_CIRCLE, Theme.ANCIENT, 110,
                "Runas brillantes giran y revelan el bot\u00edn.");
        reg("wave_pulse", "Pulso de Energ\u00eda", Style.WAVE_PULSE, Theme.SCIFI, 70,
                "Pulsos conc\u00e9ntricos construyen el reveal.");
        reg("galaxy_swirl", "Remolino Galáctico", Style.GALAXY_SWIRL, Theme.CELESTIAL, 100,
                "Un remolino de estrellas se condensa en el premio.");

        // ---- Celebratory ----
        reg("fireworks", "Fuegos Artificiales", Style.FIREWORKS, Theme.CASINO, 85,
                "Espect\u00e1culo de fuegos artificiales al revelar.");
        reg("fireworks_mega", "Gran Final", Style.FIREWORKS, Theme.CELESTIAL, 120,
                "Final apote\u00f3sico con fuegos y haz de luz combinados.");

        // ---- Instant (skip) ----
        reg("instant", "Instant\u00e1neo", Style.INSTANT, Theme.CLASSIC, 1,
                "Sin animaci\u00f3n: entrega inmediata.");
    }

    private static void reg(String id, String name, Style style, Theme theme, int duration, String desc) {
        REGISTRY.put(id, new CrateAnimation(id, name, style, theme, duration, desc));
    }

    /**
     * Public hook so addons can register extra animations during mod setup.
     * Returns the animation for chaining.
     */
    public static CrateAnimation register(String id, String name, Style style, Theme theme,
                                          int duration, String desc) {
        CrateAnimation a = new CrateAnimation(id, name, style, theme, duration, desc);
        REGISTRY.put(id, a);
        return a;
    }

    public static CrateAnimation get(String id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get("spin_basic"));
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
        return "spin_basic";
    }
}
