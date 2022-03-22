package net.himeki.btrback.tasks;

import net.himeki.btrback.util.BtrfsUtil;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RollbackTask {


    public static boolean doRollbackStageOne(String snapshotName, MinecraftServer server) {                     // Stage one of rollback process sequence, including taking a rescue snapshot and call stage two
        Thread rollbackHook = new Thread(() -> {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date());
            BackupTask.doBackup(timeStamp, true, null);
            if (doRollbackStageTwo(snapshotName))
                System.out.println("Successfully restored snapshot " + snapshotName + ", exiting...");
        });
        Runtime.getRuntime().addShutdownHook(rollbackHook);
        server.stop(false);
        return true;
    }

    public static boolean doRollbackStageTwo(String snapshotName) {                             // Stage two of rollback process, restore target snapshot
        Path PWD = Paths.get(".").toAbsolutePath().normalize();
        Path parentDir = Paths.get("..").toAbsolutePath().normalize();

        if (!BtrfsUtil.deleteSubVol(PWD)) {
            System.err.println("Cannot delete current server subvolume.");
            return false;
        }
        if (!BtrfsUtil.createSnapshot(parentDir.resolve("btrbackups").resolve(snapshotName), PWD)) {
            System.err.println("Cannot restore snapshot " + snapshotName + ".");
            return false;
        }
        return true;
    }
}
