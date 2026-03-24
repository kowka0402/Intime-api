package net.rj.intime.core.service;

import net.rj.intime.core.InTimeCorePlugin;
import net.rj.intime.core.database.SQLiteManager;
import net.rj.intime.core.model.PlayerTimeData;
import net.rj.intime.core.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TimeService {

    private final InTimeCorePlugin plugin;
    private final SQLiteManager sqliteManager;
    private final Map<UUID, PlayerTimeData> playerDataMap = new HashMap<>();
    private final AtomicBoolean saveRunning = new AtomicBoolean(false);

    private BukkitTask tickTask;
    private BukkitTask saveTask;

    public TimeService(InTimeCorePlugin plugin, SQLiteManager sqliteManager) {
        this.plugin = plugin;
        this.sqliteManager = sqliteManager;
    }

    public synchronized void loadAllFromDatabase() {
        playerDataMap.clear();

        for (PlayerTimeData data : sqliteManager.loadAll()) {
            data.setDirty(false);
            playerDataMap.put(data.getUuid(), data);
        }
    }

    public void startTickTask() {
        long periodTicks = 20L;
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAllPlayers, periodTicks, periodTicks);
    }

    public void startAutoSaveTask() {
        int saveSeconds = Math.max(10, Math.min(30, plugin.getConfig().getInt("database.save-interval-seconds", 15)));
        long periodTicks = saveSeconds * 20L;
        this.saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveDirtyPlayersAsync, periodTicks, periodTicks);
    }

    public synchronized boolean isInitialized(UUID uuid) {
        PlayerTimeData data = playerDataMap.get(uuid);
        return data != null && data.isInitialized();
    }

    public synchronized void initializePlayer(UUID uuid, String lastName, long initialSeconds) {
        PlayerTimeData data = playerDataMap.get(uuid);
        if (data != null && data.isInitialized()) {
            if (lastName != null && !lastName.isBlank() && !lastName.equals(data.getLastName())) {
                data.setLastName(lastName);
            }
            return;
        }

        PlayerTimeData newData = new PlayerTimeData(uuid, safeName(lastName), Math.max(0L, initialSeconds), true);
        newData.setDirty(true);
        playerDataMap.put(uuid, newData);
    }

    public synchronized long getRemainingSeconds(UUID uuid) {
        PlayerTimeData data = playerDataMap.get(uuid);
        return data == null ? 0L : Math.max(0L, data.getRemainingSeconds());
    }

    public synchronized void setRemainingSeconds(UUID uuid, long seconds) {
        PlayerTimeData data = getOrCreateUnknown(uuid);
        data.setRemainingSeconds(seconds);
        kickIfExpired(data);
    }

    public synchronized void addSeconds(UUID uuid, long seconds) {
        if (seconds <= 0L) {
            return;
        }
        PlayerTimeData data = getOrCreateUnknown(uuid);
        data.addSeconds(seconds);
    }

    public synchronized void removeSeconds(UUID uuid, long seconds) {
        if (seconds <= 0L) {
            return;
        }
        PlayerTimeData data = getOrCreateUnknown(uuid);
        data.removeSeconds(seconds);
        kickIfExpired(data);
    }

    public synchronized boolean tryRemoveSeconds(UUID uuid, long seconds) {
        if (seconds <= 0L) {
            return true;
        }

        PlayerTimeData data = getOrCreateUnknown(uuid);
        if (data.getRemainingSeconds() < seconds) {
            return false;
        }

        data.removeSeconds(seconds);
        kickIfExpired(data);
        return true;
    }

    public synchronized boolean hasEnoughSeconds(UUID uuid, long seconds) {
        return getRemainingSeconds(uuid) >= Math.max(0L, seconds);
    }

    public synchronized boolean isExpired(UUID uuid) {
        return getRemainingSeconds(uuid) <= 0L;
    }

    public synchronized void updateLastName(UUID uuid, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return;
        }

        PlayerTimeData data = getOrCreateUnknown(uuid);
        if (!lastName.equals(data.getLastName())) {
            data.setLastName(lastName);
        }
    }

    public synchronized Optional<UUID> findUuidByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        for (PlayerTimeData data : playerDataMap.values()) {
            if (name.equalsIgnoreCase(data.getLastName())) {
                return Optional.of(data.getUuid());
            }
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(name)) {
                return Optional.of(offlinePlayer.getUniqueId());
            }
        }

        return Optional.empty();
    }

    public synchronized void savePlayer(UUID uuid) {
        PlayerTimeData data = playerDataMap.get(uuid);
        if (data == null) {
            return;
        }

        PlayerTimeData snapshot = data.copy();
        sqliteManager.save(snapshot);
        data.setDirty(false);
    }

    public synchronized void saveAllNow() {
        Map<UUID, PlayerTimeData> snapshot = createSnapshot(true);
        sqliteManager.saveAll(snapshot.values());

        for (PlayerTimeData data : playerDataMap.values()) {
            data.setDirty(false);
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }

        if (saveTask != null) {
            saveTask.cancel();
        }

        saveAllNow();
    }

    private synchronized void tickAllPlayers() {
        for (PlayerTimeData data : playerDataMap.values()) {
            if (!data.isInitialized()) {
                continue;
            }

            if (data.getRemainingSeconds() > 0L) {
                data.setRemainingSeconds(data.getRemainingSeconds() - 1L);
            }

            kickIfExpired(data);
        }
    }

    private void kickIfExpired(PlayerTimeData data) {
        if (data.getRemainingSeconds() > 0L) {
            return;
        }

        Player player = Bukkit.getPlayer(data.getUuid());
        if (player == null || !player.isOnline()) {
            return;
        }

        String message = ChatUtil.color(
                plugin.getConfig().getString(
                        "messages.kicked-expired",
                        "&c당신의 시간이 모두 소진되어 추방되었습니다."
                )
        );
        player.kickPlayer(message);
    }

    private void saveDirtyPlayersAsync() {
        if (!saveRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            Map<UUID, PlayerTimeData> snapshot = createSnapshot(false);
            if (snapshot.isEmpty()) {
                return;
            }

            sqliteManager.saveAll(snapshot.values());

            synchronized (this) {
                for (UUID uuid : snapshot.keySet()) {
                    PlayerTimeData live = playerDataMap.get(uuid);
                    if (live != null) {
                        live.setDirty(false);
                    }
                }
            }
        } finally {
            saveRunning.set(false);
        }
    }

    private synchronized Map<UUID, PlayerTimeData> createSnapshot(boolean includeAll) {
        Map<UUID, PlayerTimeData> snapshot = new HashMap<>();

        for (Map.Entry<UUID, PlayerTimeData> entry : playerDataMap.entrySet()) {
            PlayerTimeData data = entry.getValue();
            if (includeAll || data.isDirty()) {
                snapshot.put(entry.getKey(), data.copy());
            }
        }

        return snapshot;
    }

    private synchronized PlayerTimeData getOrCreateUnknown(UUID uuid) {
        PlayerTimeData data = playerDataMap.get(uuid);
        if (data != null) {
            return data;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName() == null ? "UNKNOWN" : offlinePlayer.getName();

        PlayerTimeData created = new PlayerTimeData(uuid, name, 0L, true);
        created.setDirty(true);
        playerDataMap.put(uuid, created);
        return created;
    }

    private String safeName(String lastName) {
        return (lastName == null || lastName.isBlank()) ? "UNKNOWN" : lastName;
    }
}
