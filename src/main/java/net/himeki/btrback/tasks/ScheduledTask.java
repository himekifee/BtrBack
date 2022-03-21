package net.himeki.btrback.tasks;

import net.himeki.btrback.Btrback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScheduledTask implements Runnable {
    Btrback plugin;
    String taskName;

    public ScheduledTask(Btrback plugin, String taskName) {
        this.plugin = plugin;
        this.taskName = taskName;
    }

    @Override
    public void run() {
        if (taskName.equalsIgnoreCase("backup")) {
            new BackupTask(plugin).doBackup(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()), false);
        }
        else if(taskName.equalsIgnoreCase("auto_purge"))
        {
            try {
                new PurgeTask(plugin).doScheduledPurge();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

    }
}
