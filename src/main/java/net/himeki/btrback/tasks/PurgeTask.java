package net.himeki.btrback.tasks;

import net.himeki.btrback.BtrBack;
import net.himeki.btrback.BtrRecord;
import net.himeki.btrback.util.BtrfsUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;

public class PurgeTask {

    public static boolean doPurge(String timestamp) {
        ArrayList<String> snapshotList = BtrRecord.listBackups(false);
        snapshotList.sort(Comparator.naturalOrder());
        int index = snapshotList.indexOf(timestamp);
        for (int i = 0; i < index; i++) {                               //remove snapshots in list by order(timestamp)
            if (!BtrfsUtil.deleteSubVol(BtrBack.backupsDir.resolve(snapshotList.get(i))))
                return false;
            if (!BtrRecord.removeRecord(snapshotList.get(i)))
                return false;
        }
        return true;
    }

    public static boolean doScheduledPurge(int targetInterval) {
        ArrayList<String> backupsList = BtrRecord.listBackups(false);
        for (String i : backupsList) {
            long time = 0;
            try {
                time = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").parse(i).getTime() / 1000;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (Instant.now().getEpochSecond() - time > targetInterval)
                return doPurge(i);
        }
        return true;
    }
}
