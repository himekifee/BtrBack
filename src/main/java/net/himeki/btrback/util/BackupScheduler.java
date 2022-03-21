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


// Thanks to https://github.com/Szum123321/textile_backup
public class BackupScheduler {
    private BtrBackConfig config;

    private boolean scheduled;
    private long nextBackup = -1;
    private long nextPurge = -1;
    private int backupInterval = 0;
    private int purgeInterval = 0;      // This is the interval to indicate which backup to purge, not period between purge task runs. Period is always 1 hour
    private boolean backupInProgress = false;
    private boolean purgeInProgress = false;

    public boolean reloadConfig() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.getConfigHolder(BtrBackConfig.class);
        holder.load();
        config = holder.getConfig();
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

    public BackupScheduler(BtrBackConfig config) {
        this.config = config;
        reloadConfig();
        var now = Instant.now().getEpochSecond();
        nextBackup = now + backupInterval;
        nextPurge = now + 60 * 60;
    }

    public void tick(MinecraftServer server) {
        if (backupInterval < 1 && purgeInterval < 1) return;

        long now = Instant.now().getEpochSecond();

        if (!backupInProgress)
            if (nextBackup <= now) {
                backupInProgress = true;
                BackupTask.doBackup(new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date()), false, server);
                nextBackup = now + backupInterval;
                backupInProgress = false;
            }

        if (!purgeInProgress)
            if (nextPurge <= now) {
                purgeInProgress = true;
                PurgeTask.doScheduledPurge(purgeInterval);
                nextPurge = now + 60 * 60;
                purgeInProgress = false;
            }
    }
}