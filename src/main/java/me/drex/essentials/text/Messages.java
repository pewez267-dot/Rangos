package me.drex.essentials.text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.drex.essentials.EssentialsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configurable message/lang system.
 * Messages are stored in config/essentials/messages.json. Each command requests a key with a
 * built-in default, so missing keys never cause errors; defaults are written back to the file.
 *
 * Formatting: legacy '&' color codes (e.g. &a, &c, &l) and placeholders written as {name}.
 */
public final class Messages {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, String> MESSAGES = new LinkedHashMap<>();
    private static Path file;
    private static String prefix = "&8[&6Essentials&8] &r";

    private Messages() {
    }

    public static void load(Path configDir) {
        file = configDir.resolve("messages.json");
        MESSAGES.clear();
        if (Files.exists(file)) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        MESSAGES.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                if (MESSAGES.containsKey("prefix")) {
                    prefix = MESSAGES.get("prefix");
                }
            } catch (Exception e) {
                EssentialsMod.LOGGER.error("Failed to read messages.json, using defaults", e);
            }
        }
        // ensure prefix present
        MESSAGES.putIfAbsent("prefix", prefix);
        prefix = MESSAGES.get("prefix");
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> entry : MESSAGES.entrySet()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
            Files.writeString(file, GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            EssentialsMod.LOGGER.error("Failed to write messages.json", e);
        }
    }

    private static String raw(String key, String def) {
        String value = MESSAGES.get(key);
        if (value == null) {
            MESSAGES.put(key, def);
            value = def;
        }
        return value;
    }

    public static Component get(String key, String def) {
        return LegacyText.parse(raw(key, def));
    }

    public static Component get(String key, String def, Map<String, String> placeholders) {
        String value = raw(key, def);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return LegacyText.parse(value);
    }

    /** Build a placeholder map quickly: of("name", value, "name2", value2, ...). */
    public static Map<String, String> of(String... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }

    public static Component prefixed(String key, String def) {
        return LegacyText.parse(prefix).copy().append(get(key, def));
    }

    public static Component prefixed(String key, String def, Map<String, String> placeholders) {
        return LegacyText.parse(prefix).copy().append(get(key, def, placeholders));
    }

    public static MutableComponent prefix() {
        return LegacyText.parse(prefix).copy();
    }

    /** Minimal legacy color/format parser (&-codes and section sign). */
    public static final class LegacyText {
        public static MutableComponent parse(String input) {
            MutableComponent result = Component.empty();
            StringBuilder current = new StringBuilder();
            Style style = Style.EMPTY;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if ((c == '&' || c == '\u00a7') && i + 1 < input.length()) {
                    char code = Character.toLowerCase(input.charAt(i + 1));
                    ChatFormatting formatting = byCode(code);
                    if (formatting != null) {
                        if (current.length() > 0) {
                            result.append(Component.literal(current.toString()).setStyle(style));
                            current.setLength(0);
                        }
                        if (formatting == ChatFormatting.RESET) {
                            style = Style.EMPTY;
                        } else if (formatting.isColor()) {
                            style = Style.EMPTY.withColor(formatting);
                        } else {
                            style = style.applyFormat(formatting);
                        }
                        i++;
                        continue;
                    }
                }
                current.append(c);
            }
            if (current.length() > 0) {
                result.append(Component.literal(current.toString()).setStyle(style));
            }
            return result;
        }

        private static ChatFormatting byCode(char code) {
            for (ChatFormatting formatting : ChatFormatting.values()) {
                if (formatting.getChar() == code) {
                    return formatting;
                }
            }
            return null;
        }
    }
}
