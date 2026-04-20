package me.wobble.wauctionhouse.util;

import me.wobble.wauctionhouse.WAuctionHouse;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ChatUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ChatUtil() {
    }

    public static Component mm(String text) {
        return MINI_MESSAGE.deserialize(text == null ? "" : text);
    }

    public static Component message(WAuctionHouse plugin, String path) {
        String prefix = plugin.getMessagesConfig().getString("prefix", "");
        String text = plugin.getMessagesConfig().getString(path, "");
        return mm(prefix + text);
    }

    public static Component message(WAuctionHouse plugin, String path, String... replacements) {
        String prefix = plugin.getMessagesConfig().getString("prefix", "");
        String text = plugin.getMessagesConfig().getString(path, "");

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }

        return mm(prefix + text);
    }
}
