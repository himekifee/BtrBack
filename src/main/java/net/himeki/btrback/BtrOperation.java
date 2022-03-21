package net.himeki.btrback;

public class BtrOperation {
    BtrfsUtilInterface anInterface = BtrfsUtilInterface.INSTANCE;


    public boolean isSubvol(String path) {
        int ret = anInterface.btrfs_util_is_subvolume(path);
        if (ret != 0)
            return false;
        else return true;
    }

    public boolean createSnapshot(String source, String dest) {
        anInterface.btrfs_util_create_snapshot(source, dest, 0, null, null);
        return isSubvol(dest);
    }

    public boolean deleteSubvol(String path) {
        anInterface.btrfs_util_delete_subvolume(path, 0);
        return !isSubvol(path);
    }
}
