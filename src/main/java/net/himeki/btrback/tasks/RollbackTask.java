package net.himeki.btrback.tasks;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.himeki.btrback.BtrBack;
import net.himeki.btrback.util.BtrfsUtil;
import net.himeki.btrback.util.JVMUtil;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RollbackTask {


    public static boolean doRollbackStageOne(String snapshotName, MinecraftServer server) {                     // Stage one of rollback process sequence, including taking a rescue snapshot and replacing serve jar
        BtrBack.reload();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date());
        if (!BackupTask.doBackup(timeStamp, true, server)) {
            BtrBack.LOGGER.error("Cannot back up the latest server state. Rollback canceled.");
            return false;
        }
        List<String> programArguments = JVMUtil.getProgramArguments(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? "net.fabricmc.loader.launch.knot.KnotClient"
                : "net.fabricmc.loader.launch.knot.KnotServer");
        List<String> args = JVMUtil.getArgs();
        args.addAll(programArguments);
        RollbackConfig config = new RollbackConfig(snapshotName, args, ProcessHandle.current().pid());
        try {
            FileOutputStream outputStream = new FileOutputStream("rollback.tmp");
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(config);
            so.flush();
            bo.writeTo(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BtrBack.LOGGER.info("Saved rollback.tmp for rollback.");

        try {
            File modJarFile = new File(BtrBack.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (modJarFile.exists()) {
                // Logic copied from GrossFabricHacks Re-launcher

                // Release lock on log file
                LogManager.shutdown();
                // Add self to class path
                var argList = JVMUtil.getArgs();
                var cp = argList.get(argList.size() - 1);
                cp += ":" + modJarFile;
                argList.set(argList.size() - 1, cp);
                argList.add("net.himeki.btrback.Rollback");


                Thread launchBackHook = new Thread(() -> {
                    try {
                        System.out.println("Start calling rollback launcher...");
                        new ProcessBuilder(argList).inheritIO().start();
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
                Runtime.getRuntime().addShutdownHook(launchBackHook);
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean doRollbackStageTwo() {                             //stage two of rollback process, instantiate by Rollback class which is the main class of the replaced jar
        try {
            FileInputStream inputStream = new FileInputStream("rollback.tmp");
            ByteArrayInputStream bis = new ByteArrayInputStream(inputStream.readAllBytes());
            ObjectInputStream objectInputStream = new ObjectInputStream(bis);
            RollbackConfig config = (RollbackConfig) objectInputStream.readObject();

            Long pid = config.pid;
            Path PWD = Paths.get(".").toAbsolutePath().normalize();
            Path parentDir = Paths.get("..").toAbsolutePath().normalize();

            if (!BtrfsUtil.deleteSubVol(PWD)) {
                System.err.println("Cannot delete current server subvolume.");
                return false;
            }
            if (!BtrfsUtil.createSnapshot(parentDir.resolve("btrbackups").resolve(config.timeStamp), PWD)) {
                System.err.println("Cannot restore snapshot " + config.timeStamp + ".");
                return false;
            }

            Thread launchBackHook = new Thread(() -> {
                try {
                    System.out.println("Start calling minecraft launcher...");
                    new ProcessBuilder(config.args).inheritIO().directory(PWD.toFile()).start();
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
            Runtime.getRuntime().addShutdownHook(launchBackHook);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    public record RollbackConfig(String timeStamp, List<String> args,
                                 Long pid) implements Serializable {

        public String getTimeStamp() {
            return timeStamp;
        }

        public List<String> getArgs() {
            return args;
        }

        public Long getPid() {
            return pid;
        }
    }
}
