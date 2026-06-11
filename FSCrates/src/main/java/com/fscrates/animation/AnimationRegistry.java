package com.fscrates.animation;

import com.fscrates.animation.CrateAnimation.Style;
import com.fscrates.animation.CrateAnimation.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The animation catalogue. Every entry shows the reward as a spinning roulette
 * (horizontal) or slot reel (vertical); the theme adds a coloured light beam,
 * accent particles and a themed win sound. This guarantees ALL of them work.
 */
public final class AnimationRegistry {

    private AnimationRegistry() {}

    private static final Map<String, CrateAnimation> REGISTRY = new LinkedHashMap<>();

    static {
        reg("roulette", "Ruleta cl\u00e1sica", Style.ROULETTE, Theme.CLASSIC, 150,
                "Carrusel horizontal de items que desacelera hasta el premio.");
        reg("roulette_fast", "Ruleta r\u00e1pida", Style.ROULETTE, Theme.CLASSIC, 110,
                "Como la cl\u00e1sica pero m\u00e1s corta.");
        reg("roulette_casino", "Ruleta casino", Style.ROULETTE, Theme.CASINO, 170,
                "Ruleta con confeti y fuegos al ganar.");
        reg("roulette_neon", "Ruleta ne\u00f3n", Style.ROULETTE, Theme.NEON, 170,
                "Ruleta con haz de luz de ne\u00f3n.");
        reg("roulette_infernal", "Ruleta infernal", Style.ROULETTE, Theme.INFERNAL, 170,
                "Ruleta con llamas y haz rojo.");
        reg("roulette_celestial", "Ruleta celestial", Style.ROULETTE, Theme.CELESTIAL, 180,
                "Ruleta con haz de luz divino y destellos.");
        reg("roulette_arcane", "Ruleta arcana", Style.ROULETTE, Theme.MAGIC, 180,
                "Ruleta con energ\u00eda m\u00e1gica y haz morado.");
        reg("roulette_ancient", "Ruleta ancestral", Style.ROULETTE, Theme.ANCIENT, 180,
                "Ruleta con runas y haz dorado.");
        reg("roulette_nature", "Ruleta natural", Style.ROULETTE, Theme.NATURE, 160,
                "Ruleta con esporas y p\u00e9talos.");

        reg("roulette_jackpot", "Ruleta jackpot", Style.ROULETTE, Theme.CASINO, 150,
                "Ruleta horizontal con fuegos artificiales al ganar.");
        reg("roulette_void", "Ruleta del vac\u00edo", Style.ROULETTE, Theme.MAGIC, 160,
                "Ruleta horizontal con energ\u00eda del End.");

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
