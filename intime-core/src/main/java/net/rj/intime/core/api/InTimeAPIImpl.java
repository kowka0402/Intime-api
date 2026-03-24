package net.rj.intime.core.api;

import net.rj.intime.api.InTimeAPI;
import net.rj.intime.core.service.TimeService;
import net.rj.intime.core.util.TimeFormatUtil;

import java.util.UUID;

public final class InTimeAPIImpl implements InTimeAPI {

    private final TimeService timeService;

    public InTimeAPIImpl(TimeService timeService) {
        this.timeService = timeService;
    }

    @Override
    public boolean isInitialized(UUID uuid) {
        return timeService.isInitialized(uuid);
    }

    @Override
    public void initializePlayer(UUID uuid, String lastName, long initialSeconds) {
        timeService.initializePlayer(uuid, lastName, initialSeconds);
    }

    @Override
    public long getRemainingSeconds(UUID uuid) {
        return timeService.getRemainingSeconds(uuid);
    }

    @Override
    public void setRemainingSeconds(UUID uuid, long seconds) {
        timeService.setRemainingSeconds(uuid, seconds);
    }

    @Override
    public void addSeconds(UUID uuid, long seconds) {
        timeService.addSeconds(uuid, seconds);
    }

    @Override
    public void removeSeconds(UUID uuid, long seconds) {
        timeService.removeSeconds(uuid, seconds);
    }

    @Override
    public boolean tryRemoveSeconds(UUID uuid, long seconds) {
        return timeService.tryRemoveSeconds(uuid, seconds);
    }

    @Override
    public boolean hasEnoughSeconds(UUID uuid, long seconds) {
        return timeService.hasEnoughSeconds(uuid, seconds);
    }

    @Override
    public boolean isExpired(UUID uuid) {
        return timeService.isExpired(uuid);
    }

    @Override
    public String formatFixed(long seconds) {
        return TimeFormatUtil.formatFixed(seconds);
    }

    @Override
    public String formatSmart(long seconds) {
        return TimeFormatUtil.formatSmart(seconds);
    }

    @Override
    public String getFormattedFixed(UUID uuid) {
        return TimeFormatUtil.formatFixed(timeService.getRemainingSeconds(uuid));
    }

    @Override
    public String getFormattedSmart(UUID uuid) {
        return TimeFormatUtil.formatSmart(timeService.getRemainingSeconds(uuid));
    }

    @Override
    public void savePlayer(UUID uuid) {
        timeService.savePlayer(uuid);
    }

    @Override
    public void saveAll() {
        timeService.saveAllNow();
    }
}
