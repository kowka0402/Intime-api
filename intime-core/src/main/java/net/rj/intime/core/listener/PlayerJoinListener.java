package net.rj.intime.core.listener;

import net.rj.intime.core.InTimeCorePlugin;
import net.rj.intime.core.util.ChatUtil;
import net.rj.intime.core.util.TimeFormatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final InTimeCorePlugin plugin;

    public PlayerJoinListener(InTimeCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getTimeService().isInitialized(player.getUniqueId())) {
            long initialSeconds = Math.max(0L, plugin.getConfig().getLong("time.initial-seconds", 3600L));
            plugin.getTimeService().initializePlayer(player.getUniqueId(), player.getName(), initialSeconds);

            String prefix = ChatUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&6InTime&8] "));
            String message = plugin.getConfig().getString("messages.initial-time-given", "&a초기 시간이 지급되었습니다: &e%time%");
            message = message.replace("%time%", TimeFormatUtil.formatSmart(initialSeconds));
            player.sendMessage(prefix + ChatUtil.color(message));
            return;
        }

        plugin.getTimeService().updateLastName(player.getUniqueId(), player.getName());
    }
}
