package app.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncSchedule {
    private static final Logger log = LoggerFactory.getLogger(SyncSchedule.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final SyncService syncService;

    public SyncSchedule(SyncService syncService) {
        this.syncService = syncService;
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::run, 0, 30, TimeUnit.MINUTES);
    }

    private void run() {
        try {
            syncService.syncFromDate(LocalDate.now().minusDays(1));
        } catch (Exception e) {
            log.error("Scheduled sync failed", e);
        }
    }

    public void stop() {
        scheduler.shutdown();
    }
}
