package net.rj.intime.core.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.rj.intime.core.InTimeCorePlugin;
import net.rj.intime.core.util.TimeFormatUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InTimePlaceholderExpansion extends PlaceholderExpansion {

    private final InTimeCorePlugin plugin;

    public InTimePlaceholderExpansion(InTimeCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "intime";
    }

    @Override
    public @NotNull String getAuthor() {
        return "rj";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        long seconds = plugin.getTimeService().getRemainingSeconds(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "time_seconds" -> String.valueOf(seconds);
            case "time_fixed" -> TimeFormatUtil.formatFixed(seconds);
            case "time_smart" -> TimeFormatUtil.formatSmart(seconds);
            default -> null;
        };
    }
}
