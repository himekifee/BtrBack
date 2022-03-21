package net.himeki.btrback.util;

import java.nio.file.Path;

public class BtrfsUtil {
    static BtrfsUtilJnaInterface buInterface = BtrfsUtilJnaInterface.INSTANCE;


    public static boolean isSubVol(Path path) {
        int ret = buInterface.btrfs_util_is_subvolume(path.toString());
        return ret == 0;
    }

    public static boolean createSnapshot(Path source, Path dest) {
        buInterface.btrfs_util_create_snapshot(source.toString(), dest.toString(), 0, null, null);
        return isSubVol(dest);
    }

    public static boolean deleteSubVol(Path path) {
        buInterface.btrfs_util_delete_subvolume(path.toString(), 0);
        return !isSubVol(path);
    }
}
