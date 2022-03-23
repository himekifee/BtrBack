package net.himeki.btrback.util;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.himeki.btrback.config.BtrBackConfig;
import net.himeki.btrback.tasks.BackupTask;
import net.himeki.btrback.tasks.PurgeTask;
import net.minecraft.server.MinecraftServer;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;


// Thanks to https://github.com/Szum123321/textile_backup
public class BackupScheduler {
    private long lastBackup;
    private long lastPurge;
    private int backupInterval = 0;
    private int purgeInterval = 0;      // This is the interval to indicate which backup to purge, not period between purge task runs. Period is always 1 hour
    private final ReentrantLock backupLock = new ReentrantLock();
    private final ReentrantLock purgeLock = new ReentrantLock();

    public boolean reloadConfig() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.getConfigHolder(BtrBackConfig.class);
        if (!holder.load())
            return false;
        var config = holder.getConfig();
        backupInterval = 0;
        backupInterval += config.backupDays * 60 * 60 * 24;
        backupInterval += config.backupHours * 60 * 60;
        backupInterval += config.backupMinutes * 60;

        purgeInterval = 0;
        purgeInterval += config.autoPurgeDays * 60 * 60 * 24;
        purgeInterval += config.autoPurgeHours * 60 * 60;
        purgeInterval += config.autoPurgeMinutes * 60;
        return true;
    }

    public BackupScheduler() {
        reloadConfig();
        var now = Instant.now().getEpochSecond();
        lastBackup = now;
        lastPurge = now;
    }

    public void updateLastBackup() {
        lastBackup = Instant.now().getEpochSecond();
    }

    public void updateLastPurge() {
        lastPurge = Instant.now().getEpochSecond();
    }

    public void tick(MinecraftServer server) {
        if (backupInterval < 1 || purgeInterval < 1) return;

        long now = Instant.now().getEpochSecond();

        if (backupLock.tryLock())
            if (lastBackup + backupInterval < now) {
                CompletableFuture.runAsync(() -> {
                    BackupTask.doBackup(new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date()), false, server);
                    backupLock.unlock();
                });
            }

        if (purgeLock.tryLock())
            if (lastPurge + 60 * 60 < now) {
                CompletableFuture.runAsync(() -> {
                    PurgeTask.doScheduledPurge(purgeInterval);
                    purgeLock.unlock();
                });
            }
    }
}