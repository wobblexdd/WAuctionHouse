package me.wobble.wobbleauction.util;

import java.util.HashMap;
import java.util.Map;

public final class TextStyleUtil {

    private static final Map<Character, Character> SMALL_CAPS = new HashMap<>();

    static {
        SMALL_CAPS.put('a', 'ᴀ');
        SMALL_CAPS.put('b', 'ʙ');
        SMALL_CAPS.put('c', 'ᴄ');
        SMALL_CAPS.put('d', 'ᴅ');
        SMALL_CAPS.put('e', 'ᴇ');
        SMALL_CAPS.put('f', 'ꜰ');
        SMALL_CAPS.put('g', 'ɢ');
        SMALL_CAPS.put('h', 'ʜ');
        SMALL_CAPS.put('i', 'ɪ');
        SMALL_CAPS.put('j', 'ᴊ');
        SMALL_CAPS.put('k', 'ᴋ');
        SMALL_CAPS.put('l', 'ʟ');
        SMALL_CAPS.put('m', 'ᴍ');
        SMALL_CAPS.put('n', 'ɴ');
        SMALL_CAPS.put('o', 'ᴏ');
        SMALL_CAPS.put('p', 'ᴘ');
        SMALL_CAPS.put('q', 'ǫ');
        SMALL_CAPS.put('r', 'ʀ');
        SMALL_CAPS.put('s', 's');
        SMALL_CAPS.put('t', 'ᴛ');
        SMALL_CAPS.put('u', 'ᴜ');
        SMALL_CAPS.put('v', 'ᴠ');
        SMALL_CAPS.put('w', 'ᴡ');
        SMALL_CAPS.put('x', 'x');
        SMALL_CAPS.put('y', 'ʏ');
        SMALL_CAPS.put('z', 'ᴢ');
    }

    private TextStyleUtil() {
    }

    public static String smallCaps(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (char c : input.toCharArray()) {
            char lower = Character.toLowerCase(c);
            builder.append(SMALL_CAPS.getOrDefault(lower, c));
        }

        return builder.toString();
    }

    public static String hint(String text) {
        return "<gray><italic>" + smallCaps(text) + "</italic></gray>";
    }

    public static String label(String key, String value) {
        return "<gray>" + key + ":</gray> <yellow>" + value + "</yellow>";
    }
}
