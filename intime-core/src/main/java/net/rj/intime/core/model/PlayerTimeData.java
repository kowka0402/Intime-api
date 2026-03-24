package net.rj.intime.core.model;

import java.util.UUID;

public final class PlayerTimeData {

    private final UUID uuid;
    private String lastName;
    private long remainingSeconds;
    private boolean initialized;
    private boolean dirty;

    public PlayerTimeData(UUID uuid, String lastName, long remainingSeconds, boolean initialized) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.remainingSeconds = Math.max(0L, remainingSeconds);
        this.initialized = initialized;
        this.dirty = false;
    }

    public PlayerTimeData copy() {
        PlayerTimeData copy = new PlayerTimeData(this.uuid, this.lastName, this.remainingSeconds, this.initialized);
        copy.setDirty(this.dirty);
        return copy;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.dirty = true;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = Math.max(0L, remainingSeconds);
        this.dirty = true;
    }

    public void addSeconds(long seconds) {
        if (seconds <= 0L) {
            return;
        }
        this.remainingSeconds += seconds;
        this.dirty = true;
    }

    public void removeSeconds(long seconds) {
        if (seconds <= 0L) {
            return;
        }
        this.remainingSeconds = Math.max(0L, this.remainingSeconds - seconds);
        this.dirty = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        this.dirty = true;
    }

    public boolean isExpired() {
        return this.remainingSeconds <= 0L;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
