package net.himeki.btrback;

import net.himeki.btrback.tasks.BackupTask;
import net.himeki.btrback.tasks.PurgeTask;
import net.himeki.btrback.tasks.RollbackTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BtrCommand implements CommandExecutor {
    Btrback plugin;

    public BtrCommand(Btrback plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args.length) {                      // Start from 0
            case 1:
                if (args[0].equalsIgnoreCase("backup")) {
                    if (sender.hasPermission("btrback.backup")) {
                        if (plugin.isServerInSubvol()) {
                            BackupTask aTask = new BackupTask(plugin);
                            String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
                            if (!aTask.doBackup(timestamp, false))
                                sender.sendMessage(ChatColor.RED + "Backup failed. Check console for details.");
                            else sender.sendMessage(ChatColor.GREEN + "Successfully created snapshot " + timestamp);

                        } else
                            sender.sendMessage(ChatColor.RED + "Server not in a btrfs subvolume, plugin will not work.");
                    } else sender.sendMessage(ChatColor.RED + "You don't have the permission to backup.");
                }
                if (args[0].equalsIgnoreCase("rollback")) {
                    sender.sendMessage("Usage: /btrback rollback [list/timestamps]");
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("btrback.reload"))
                        if (plugin.reloadSchedule())

                            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");

                        else sender.sendMessage(ChatColor.RED + "Failed to reload configuration.");
                    else sender.sendMessage(ChatColor.RED + "You don't have the permission to reload the plugin.");
                }
            case 2:
                if (args[0].equalsIgnoreCase("rollback")) {
                    if (sender.hasPermission("btrback.rollback")) {
                        ArrayList<String> backupsList = new BtrRecord(plugin).listBackups(false);

                        if (backupsList.contains(args[1]))                                                              //Valid timeStamp, do rollback
                        {
                            if (new RollbackTask(plugin).doRollbackStageOne(args[1])) {
                                sender.sendMessage(ChatColor.GREEN + "Successfully done stage one, shutting the server. Please start it to complete stage two.");
                                Bukkit.shutdown();
                            } else
                                sender.sendMessage(ChatColor.RED + "Failed to finish stage one. Check console logs for details.");
                        } else {
                            if (args[1].equalsIgnoreCase("list")) {
                                sender.sendMessage(ChatColor.GREEN + "Valid backups are listed below:");
                                for (String a : backupsList) {
                                    sender.sendMessage(a);
                                }
                            }
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have the permission to rollback.");
                    }
                }
                if (args[0].equalsIgnoreCase("purge")) {
                    if (sender.hasPermission("btrback.purge")) {
                        ArrayList<String> backupsList = new BtrRecord(plugin).listBackups(false);
                        if (backupsList.contains(args[1])) {
                            if (new PurgeTask(plugin).doPurge(args[1])) {
                                sender.sendMessage(ChatColor.GREEN + "Successfully purged backups before " + args[1]);
                            } else sender.sendMessage(ChatColor.RED + "Failed to purge.");
                        } else sender.sendMessage(ChatColor.RED + "Target backup is not in the list.");
                    } else sender.sendMessage(ChatColor.RED + "You don't have the permission to purge.");
                }
        }
        return true;
    }
}
