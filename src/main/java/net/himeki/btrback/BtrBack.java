package net.himeki.btrback;

import com.google.gson.stream.JsonWriter;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;


public class BtrBack implements ModInitializer {
    public static boolean serverInSubVol = false;
    public static Path rootDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    public static Path parentDir = rootDir.getParent().toAbsolutePath();
    public static Path backupsDir = parentDir.resolve("btrbackups");
    public static Path recordsJsonPath = backupsDir.resolve("backups.json");
    public static final Logger LOGGER = LogManager.getLogger();
    private static BackupScheduler scheduler;


    public boolean loadSchedule() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.getConfigHolder(BtrBackConfig.class);
        holder.load();

        scheduler = new BackupScheduler(holder.getConfig());

        LOGGER.info("Scheduled backup task.");
        LOGGER.info("Scheduled auto purge task.");
        return true;
    }

    public static boolean reloadSchedule() {
        return scheduler.reloadConfig();
    }

    @Override
    public void onInitialize() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.register(BtrBackConfig.class, Toml4jConfigSerializer::new);
        holder.load();
        File btrbackFolder = backupsDir.toFile();
        if (!btrbackFolder.exists())
            btrbackFolder.mkdir();                          //Create backup folders if not present
        File jsonFile = recordsJsonPath.toFile();
        if (!jsonFile.exists())                             //Create records json file
        {
            try {
                JsonWriter writer = new JsonWriter(new FileWriter(jsonFile));
                writer.beginObject();
                writer.name("backupSnapshots");
                writer.beginArray();
                writer.endArray();
                writer.name("rollbackSnapshots");
                writer.beginArray();
                writer.endArray();
                writer.endObject();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Failed to create backups json file.");
            }
        }
        if (!BtrfsUtil.isSubVol(rootDir)) {
            LOGGER.error("Server is not in a btrfs subvolume, mod will exit immediately.");
            return;
        }
        serverInSubVol = true;
        loadSchedule();
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            BtrCommand.register(dispatcher);
        });
        ServerTickEvents.END_SERVER_TICK.register(scheduler::tick);
    }

    public static void reload() {
        ConfigHolder<BtrBackConfig> holder = AutoConfig.getConfigHolder(BtrBackConfig.class);
        holder.load();
    }
}
