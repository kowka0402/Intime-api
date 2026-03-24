package net.rj.intime.api;

import java.util.UUID;

public interface InTimeAPI {

    boolean isInitialized(UUID uuid);

    void initializePlayer(UUID uuid, String lastName, long initialSeconds);

    long getRemainingSeconds(UUID uuid);

    void setRemainingSeconds(UUID uuid, long seconds);

    void addSeconds(UUID uuid, long seconds);

    void removeSeconds(UUID uuid, long seconds);

    boolean tryRemoveSeconds(UUID uuid, long seconds);

    boolean hasEnoughSeconds(UUID uuid, long seconds);

    boolean isExpired(UUID uuid);

    String formatFixed(long seconds);

    String formatSmart(long seconds);

    String getFormattedFixed(UUID uuid);

    String getFormattedSmart(UUID uuid);

    void savePlayer(UUID uuid);

    void saveAll();
}
