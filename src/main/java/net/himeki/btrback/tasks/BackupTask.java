package net.himeki.btrback.tasks;

import net.himeki.btrback.BtrBack;
import net.himeki.btrback.BtrRecord;
import net.himeki.btrback.util.BtrfsUtil;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class BackupTask {

    public static boolean doBackup(String timeStamp, Boolean isBeforeRollback, MinecraftServer server) {
        if (!isBeforeRollback)
            server.saveAll(false, true, true);
        if (!BtrfsUtil.isSubVol(BtrBack.rootDir)) {
            if (!isBeforeRollback)
                BtrBack.LOGGER.error("The server root is not in a btrfs subvolume. Backup canceled.");
            else
                System.err.println("The server root is not in a btrfs subvolume. Backup canceled.");
            return false;
        }
        if (!BtrfsUtil.createSnapshot(BtrBack.rootDir, BtrBack.backupsDir.resolve(timeStamp))) {
            if (!isBeforeRollback)
                BtrBack.LOGGER.error("Cannot create snapshot " + timeStamp);
            else
                System.err.println("Cannot create snapshot " + timeStamp);
            return false;
        }
        if (!isBeforeRollback) {
            if (!BtrRecord.addToBackups(timeStamp)) {
                BtrfsUtil.deleteSubVol(BtrBack.backupsDir.resolve(timeStamp));
                BtrBack.LOGGER.error("Cannot write the record to json file, backup canceled.");
                return false;
            }
        } else {
            if (!BtrRecord.addToRollbacks(timeStamp)) {
                BtrfsUtil.deleteSubVol(BtrBack.backupsDir.resolve(timeStamp));
                System.err.println("Cannot write the record to json file, backup canceled.");
                return false;
            }
        }
        if (!isBeforeRollback) {
            BtrBack.LOGGER.info(Formatting.GREEN + "Snapshot " + timeStamp + " has been created.");
            server.getPlayerManager().broadcast(new LiteralText("Snapshot " + timeStamp + " has been created.").formatted(Formatting.BLUE), MessageType.CHAT, UUID.randomUUID());
        } else System.out.println("Snapshot " + timeStamp + " has been created.");
        return true;
    }
}
