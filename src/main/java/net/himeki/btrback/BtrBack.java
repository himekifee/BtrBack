package net.himeki.btrback;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.himeki.btrback.command.BtrCommand;
import net.himeki.btrback.config.BtrBackConfig;
import net.himeki.btrback.util.BackupScheduler;
import net.himeki.btrback.util.BtrfsUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;


public class BtrBack implements ModInitializer {
    public static boolean serverInSubVol = false;
    public static final Path rootDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    public static final Path parentDir = rootDir.getParent().toAbsolutePath();
    public static final Path backupsDir = parentDir.resolve("btrbackups");
    public static final Path recordsJsonPath = backupsDir.resolve("backups.json");
    public static final Logger LOGGER = LogManager.getLogger();
    public static BackupScheduler scheduler;


    public void loadSchedule() {
        scheduler = new BackupScheduler();

        LOGGER.info("Scheduled backup task.");
        LOGGER.info("Scheduled auto purge task.");
    }

    public static boolean reloadSchedule() {
        return scheduler.reloadConfig();
    }

    @Override
    public void onInitialize() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.register(BtrBackConfig.class, Toml4jConfigSerializer::new);
        if (!holder.load()) {
            LOGGER.error("Error loading mod config, mod will exit immediately.");
            return;
        }

        if (!BtrRecord.checkDirectoryAndRecord()) {
            LOGGER.error("Error checking directory and record, mod will exit immediately.");
            return;
        }

        if (!BtrfsUtil.isSubVol(rootDir)) {
            LOGGER.error("Server is not in a btrfs subvolume, mod will exit immediately.");
            return;
        }
        serverInSubVol = true;
        loadSchedule();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> BtrCommand.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(scheduler::tick);
    }

    public static boolean reload() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.getConfigHolder(BtrBackConfig.class);
        holder.load();
        return reloadSchedule();
    }
}
