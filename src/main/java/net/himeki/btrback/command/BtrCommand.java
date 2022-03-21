package net.himeki.btrback.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.himeki.btrback.BtrBack;
import net.himeki.btrback.BtrRecord;
import net.himeki.btrback.tasks.BackupTask;
import net.himeki.btrback.tasks.PurgeTask;
import net.himeki.btrback.tasks.RollbackTask;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BtrCommand {

    private static boolean hasPermission(ServerCommandSource serverCommandSource, String permission) throws CommandSyntaxException {
        if (serverCommandSource.hasPermissionLevel(4))
            return true;
        if (serverCommandSource.getEntity() instanceof ServerPlayerEntity) {
            var user = LuckPermsProvider.get().getPlayerAdapter(ServerPlayerEntity.class).getUser(serverCommandSource.getPlayer());
            var permissionData = user.getCachedData().permissionData();
            return permissionData.get(user.getQueryOptions()).checkPermission(permission).asBoolean();
        }
        return true;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        var root = dispatcher.register(literal("btrback")
                .then(literal("backup")
                        .requires(serverCommandSource -> {
                            try {
                                return hasPermission(serverCommandSource, "btrback.backup");
                            } catch (CommandSyntaxException e) {
                                e.printStackTrace();
                            }
                            return false;
                        })
                        .executes(ctx -> {
                            if (BtrBack.serverInSubVol) {
                                String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date());
                                if (!BackupTask.doBackup(timestamp, false, ctx.getSource().getServer())) {
                                    ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "Backup failed. Check console for details."), false);
                                    return -1;
                                } else {
                                    ctx.getSource().sendFeedback(new LiteralText(Formatting.GREEN + "Successfully created snapshot " + timestamp), false);
                                    return 1;
                                }
                            } else {
                                ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "Server not in a btrfs subvolume, plugin will not work."), false);
                                return -1;
                            }
                        }))
                .then(literal("reload")
                        .requires(serverCommandSource -> {
                            try {
                                return hasPermission(serverCommandSource, "btrback.reload");
                            } catch (CommandSyntaxException e) {
                                e.printStackTrace();
                            }
                            return false;
                        })
                        .executes(ctx -> {
                            if (BtrBack.reloadSchedule()) {
                                ctx.getSource().sendFeedback(new LiteralText(Formatting.GREEN + "Configuration reloaded."), false);
                                return 1;
                            } else {
                                ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "Failed to reload configuration."), false);
                                return -1;
                            }
                        }))
                .then(literal("rollback")
                        .then(argument("timestamp", StringArgumentType.string())
                                .suggests(new BtrBackRecordsSuggestionProvider())
                                .requires(serverCommandSource -> {
                                    try {
                                        return hasPermission(serverCommandSource, "btrback.rollback");
                                    } catch (CommandSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                    return false;
                                })
                                .executes(ctx -> {
                                    ArrayList<String> backupsList = BtrRecord.listBackups(false);
                                    String arg = ctx.getArgument("timestamp", String.class);

                                    if (backupsList.contains(arg))                                                              //Valid timeStamp, do rollback
                                    {
                                        if (RollbackTask.doRollbackStageOne(arg, ctx.getSource().getServer())) {
                                            ctx.getSource().sendFeedback(new LiteralText(Formatting.GREEN + "Successfully done stage one, waiting for the server to be shut down."), false);
                                            ctx.getSource().getServer().stop(false);
                                            return 1;
                                        } else {
                                            ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "Failed to finish stage one. Check console logs for details."), false);
                                            return -1;
                                        }
                                    } else if (arg.equalsIgnoreCase("list")) {
                                        ctx.getSource().sendFeedback(new LiteralText(Formatting.GREEN + "Valid backups are listed below:"), false);
                                        for (String entry : backupsList) {
                                            ctx.getSource().sendFeedback(new LiteralText(entry), false);
                                        }
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "No such backup."), false);
                                        return -1;
                                    }
                                })))
                .then(literal("purge")
                        .then(argument("timestamp", StringArgumentType.string())
                                .suggests(new BtrBackRecordsSuggestionProvider())
                                .requires(serverCommandSource -> {
                                    try {
                                        return hasPermission(serverCommandSource, "btrback.purge");
                                    } catch (CommandSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                    return false;
                                })
                                .executes(ctx -> {
                                    ArrayList<String> backupsList = BtrRecord.listBackups(false);
                                    String arg = ctx.getArgument("timestamp", String.class);

                                    if (backupsList.contains(arg)) {
                                        if (PurgeTask.doPurge(arg)) {
                                            ctx.getSource().sendFeedback(new LiteralText(Formatting.GREEN + "Successfully purged backups before " + arg), false);
                                            return 1;
                                        } else
                                            ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "Failed to purge."), false);
                                        return -1;
                                    } else {
                                        ctx.getSource().sendFeedback(new LiteralText(Formatting.RED + "No such backup."), false);
                                        return -1;
                                    }
                                })))
        );
    }

    private static class BtrBackRecordsSuggestionProvider implements SuggestionProvider<ServerCommandSource> {

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
            ArrayList<String> backupsList = BtrRecord.listBackups(false);
            for (String backup : backupsList)
                builder.suggest(backup);
            return builder.buildFuture();
        }
    }
}
