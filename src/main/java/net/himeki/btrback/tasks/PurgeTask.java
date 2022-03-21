package net.himeki.btrback.tasks;

import net.himeki.btrback.BtrOperation;
import net.himeki.btrback.BtrRecord;
import net.himeki.btrback.Btrback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;

public class PurgeTask {
    Btrback plugin;

    public PurgeTask(Btrback plugin) {
        this.plugin = plugin;
    }

    public boolean doPurge(String timestamp) {
        BtrOperation operation = new BtrOperation();
        ArrayList<String> snapshotList = new BtrRecord(plugin).listBackups(false);
        snapshotList.sort(Comparator.naturalOrder());
        int index = snapshotList.indexOf(timestamp);
        for (int i = 0; i < index; i++) {                               //remove snapshots in list by order(timestamp)
            if (!operation.deleteSubvol(plugin.getBackupsDir() + "/" + snapshotList.get(i)))
                return false;
            if (!new BtrRecord(plugin).removeRecord(snapshotList.get(i)))
                return false;
        }
        return true;
    }

    public boolean doScheduledPurge() throws ParseException {
        long targetInterval = plugin.getConfig().getLong("auto-purge.period.hours") * 3600000
                + plugin.getConfig().getLong("auto-purge.period.days") * 3600000 * 24
                + plugin.getConfig().getLong("auto-purge.period.weeks") * 3600000 * 24 * 7
                + plugin.getConfig().getLong("auto-purge.period.months") * 3600000 * 24 * 31;
        ArrayList<String> backupsList = new BtrRecord(plugin).listBackups(false);
        for (String i : backupsList) {
            long time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(i).getTime();
            if (System.currentTimeMillis() - time > targetInterval)
                return doPurge(i);
        }
        return true;
    }
}
