package net.rj.intime.core.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.rj.intime.core.InTimeCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class DisplayNameUtil {

    private DisplayNameUtil() {
    }

    public static String getDisplayName(InTimeCorePlugin plugin, UUID uuid, String fallbackName) {
        String placeholder = plugin.getConfig().getString("display-name.placeholder", "").trim();

        if (placeholder.isEmpty()) {
            return fallbackName;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return fallbackName;
        }

        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            String parsed = PlaceholderAPI.setPlaceholders(online, placeholder);
            if (isUsable(parsed, placeholder)) {
                return parsed;
            }
            return fallbackName;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String parsed = PlaceholderAPI.setPlaceholders(offline, placeholder);
        if (isUsable(parsed, placeholder)) {
            return parsed;
        }

        return fallbackName;
    }

    private static boolean isUsable(String parsed, String originalPlaceholder) {
        if (parsed == null) {
            return false;
        }

        String value = parsed.trim();
        if (value.isEmpty()) {
            return false;
        }

        return !value.equals(originalPlaceholder);
    }
}