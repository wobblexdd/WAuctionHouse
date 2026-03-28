package me.wobble.wobbleauction.util;

import me.wobble.wobbleauction.WobbleAuction;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {

    private SoundUtil() {
    }

    public static void playSuccess(WobbleAuction plugin, Player player) {
        play(plugin, player, "sounds.success", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    public static void playError(WobbleAuction plugin, Player player) {
        play(plugin, player, "sounds.error", Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    public static void playClick(WobbleAuction plugin, Player player) {
        play(plugin, player, "sounds.click", Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private static void play(WobbleAuction plugin, Player player, String path, Sound fallback, float volume, float pitch) {
        if (!plugin.getConfig().getBoolean("sounds.enabled", true)) {
            return;
        }

        String soundName = plugin.getConfig().getString(path, fallback.name());

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            sound = fallback;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
