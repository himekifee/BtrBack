package net.himeki.btrback.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.auoeke.reflect.Classes;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.*;

public class JVMUtil {
    public static List<String> getProgramArguments(String className) {
        try {
            Class.forName("org.multimc.EntryPoint");

            List<String> mainArgs = Arrays.asList(FabricLoader.getInstance().getLaunchArguments(false));

            // replace MultiMC's entry point with Fabric's
            mainArgs.add(0, className);

            return mainArgs;
        } catch (ClassNotFoundException exception) {
            return Arrays.asList(System.getProperty("sun.java.command").split(" "));
        }
    }

    public static List<String> getVMArguments() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments();
    }

    public static List<String> getArgs() {
        List<String> virtualMachineArguments = new ArrayList<>(JVMUtil.getVMArguments());
        String home = new File(System.getProperty("java.home")).getAbsolutePath();
        List<String> args = new ArrayList<>();

        Set<String> newClassPath = new ObjectOpenHashSet<>();
        for (URL url : Classes.urls(ClassLoader.getSystemClassLoader())) {
            newClassPath.add(url.getFile());
        }

        for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!path.startsWith(home)) {
                newClassPath.add(path);
            }
        }

        args.add(new File(new File(home, "bin"), "java" + JVMUtil.OS.operatingSystem.executableExtension).getAbsolutePath());
        args.addAll(virtualMachineArguments);
        args.add("-cp");
        args.add(String.join(File.pathSeparator, newClassPath));
        return args;
    }

    public enum OS {
        LINUX("linux"),

        MAC_OS("mac"),

        WINDOWS("win", ".exe"),

        OTHER("other");

        public static final OS operatingSystem;
        public static final String architecture;
        public static final String platform;

        public final String string;
        public final String executableExtension;

        OS(String string) {
            this(string, "");
        }

        OS(String string, String executableExtension) {
            this.string = string;
            this.executableExtension = executableExtension;
        }

        static {
            String operatingSystemName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

            operatingSystem = operatingSystemName.contains("linux") ? LINUX
                    : operatingSystemName.contains("mac") ? MAC_OS
                    : operatingSystemName.contains("windows") ? WINDOWS
                    : OTHER;

            switch (System.getProperty("os.arch")) {
                case "x86":
                case "i386":
                case "i686":
                    architecture = "x32";
                    break;
                case "amd64":
                    architecture = "x64";
                    break;
                case "arm":
                    architecture = "arm";
                    break;
                case "aarch64_be":
                case "armv8b":
                case "armv8l":
                case "aarch64":
                    architecture = "aarch64";
                    break;
                default:
                    architecture = "other";
            }

            platform = operatingSystem.string + '-' + architecture;
        }
    }
}
