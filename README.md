# Btrback
A minecraft backup plugin which takes advance of btrfs snapshot mechanism

# Usage
1. Change your server directory to a btrfs subvolume and mount root subvolume as `user_subvol_rm_allowed`
2. Put this plugin into your plugins folder
3. run /btrback [backup/rollback]

# Tips
- The state just before you rollback is also snapshoted. However, it will not show up in backup lists. It can be found in `rollbackSnapshots` section of backups.json file. (normal backups in `backupSnapshots` section)

- Rollback requires reboot the server twice. The first reboot aim to restore the backup and the second one launches your server like usual.
