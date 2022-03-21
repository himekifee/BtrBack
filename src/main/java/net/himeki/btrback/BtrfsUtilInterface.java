package net.himeki.btrback;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface BtrfsUtilInterface extends Library {
    BtrfsUtilInterface INSTANCE = (BtrfsUtilInterface) Native.load("btrfsutil", BtrfsUtilInterface.class);
    int btrfs_util_is_subvolume(String path, Object... args);
    void btrfs_util_create_subvolume(String path, Object... args);
    void btrfs_util_create_snapshot(String source, String destination, Object... args);
    void btrfs_util_delete_subvolume(String path, Object... args);
}