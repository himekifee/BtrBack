package net.himeki.btrback.util;

import java.net.URL;

public class FileUtil {
    public static URL getClassURL(Class<?> clazz) {
        return clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
    }

    public static String getJARFromURL(URL url) {
        if (!url.getProtocol().equals("jar"))
            return null;
        String fileName = url.getFile();
        fileName = fileName.substring(0, fileName.lastIndexOf('!'));
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        return fileName;
    }
}
