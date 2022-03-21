package net.himeki.btrback.tasks;

import net.himeki.btrback.BtrOperation;
import net.himeki.btrback.Btrback;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.JarFile;

public class RollbackTask {
    Btrback plugin;

    public RollbackTask(Btrback plugin) {
        this.plugin = plugin;
    }

    public boolean doRollbackStageOne(String snapshotName) {                     //stage one of rollback process, including taking latest snapshot and replace serve jar
        plugin.reloadConfig();
        String serverJarPath = guessServerJarPath();
        if (!new File(serverJarPath).exists()) {
            Bukkit.getLogger().warning("Cannot find proper server jar file.");
            return false;
        }
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
        if (!new BackupTask(plugin).doBackup(timeStamp, true)) {
            Bukkit.getLogger().warning("Cannot back up the latest server state. Rollback canceled.");
            return false;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(plugin.getRootDir() + "/rollback.tmp"));
            writer.write(snapshotName);
            writer.close();
            Bukkit.getLogger().info("Saved rollback.tmp for rollback.");
        } catch (IOException e) {
            e.printStackTrace();
            new BtrOperation().deleteSubvol(plugin.getBackupsDir() + timeStamp);
            Bukkit.getLogger().warning("Cannot save temp rollback file. Rollback canceled.");
            return false;
        }

        try {
            Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
            getFileMethod.setAccessible(true);
            File pluginJarFile = (File) getFileMethod.invoke(plugin);
            Files.copy(pluginJarFile.toPath(), Paths.get(serverJarPath), StandardCopyOption.REPLACE_EXISTING);
            Bukkit.getLogger().info("Replaced server jar file.");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("Cannot replace server jar with plugin jar.");
            return false;
        }
        return true;
    }

    public boolean doRollbackStageTwo() {                             //stage two of rollback process, instantiate by Rollback class which is the main class of the replaced jar
        try {
            BufferedReader reader = new BufferedReader(new FileReader("rollback.tmp"));
            String timeStamp = reader.readLine();
            String PWD = Paths.get(".").toAbsolutePath().normalize().toString();
            String parentDir = Paths.get("..").toAbsolutePath().normalize().toString();
            BtrOperation operation = new BtrOperation();
            if (!operation.deleteSubvol(PWD)) {
                System.out.println("Cannot delete current server subvolume.");
                return false;
            }
            if (!operation.createSnapshot(parentDir + "/btrbackups/" + timeStamp, PWD)) {
                System.out.println("Cannot restore snapshot " + timeStamp + ".");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public String guessServerJarPath() {
        boolean isPaper = false;
        isPaper = Bukkit.getServer().getVersion().contains("Paper");
        if (isPaper) {
            //server is paper spigot. get server jar by walk through server directory
            File[] files = new File(plugin.getRootDir()).listFiles();
            for (File file : files) {
                if (file.getName().contains(".jar")) {
                    // Open the JAR file
                    String mainClassName = null;
                    try {
                        mainClassName = new JarFile(file).getManifest().getMainAttributes().getValue("Main-Class");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (mainClassName.equalsIgnoreCase("io.papermc.paperclip.Paperclip")) {
                        return file.getPath();
                    }
                }
            }

        } else {
            //server is bukkit/spigot. return server jar by getting url from server jar
            return Bukkit.getServer().getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        }
        return "";
    }
}
