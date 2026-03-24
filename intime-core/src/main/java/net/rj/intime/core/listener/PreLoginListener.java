package net.rj.intime.core.listener;

import net.rj.intime.core.InTimeCorePlugin;
import net.rj.intime.core.util.ChatUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public final class PreLoginListener implements Listener {

    private final InTimeCorePlugin plugin;

    public PreLoginListener(InTimeCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getTimeService().isInitialized(event.getUniqueId())) {
            return;
        }

        if (!plugin.getTimeService().isExpired(event.getUniqueId())) {
            return;
        }

        String raw = plugin.getConfig().getString("messages.login-denied-expired", "&c당신의 시간이 모두 소진되어 접속할 수 없습니다.");
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatUtil.color(raw));
    }
}
