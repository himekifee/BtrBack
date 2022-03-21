package net.himeki.btrback.util;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface BtrfsUtilJnaInterface extends Library {
    BtrfsUtilJnaInterface INSTANCE = Native.load("btrfsutil", BtrfsUtilJnaInterface.class);
    int btrfs_util_is_subvolume(String path, Object... args);
    void btrfs_util_create_subvolume(String path, Object... args);
    void btrfs_util_create_snapshot(String source, String destination, Object... args);
    void btrfs_util_delete_subvolume(String path, Object... args);
}