package net.himeki.btrback;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class BtrRecord {
    static JsonObject rootObj;
    static File jsonFile;

    public static boolean checkDirectoryAndRecord() {
        File btrBackFolder = BtrBack.backupsDir.toFile();
        if (!btrBackFolder.exists())
            if (!btrBackFolder.mkdir())     //Create backup folders if not present
                return false;

        jsonFile = BtrBack.recordsJsonPath.toFile();
        if (!jsonFile.exists())                             //Create records json file
        {
            try (JsonWriter writer = new JsonWriter(new FileWriter(jsonFile))) {
                writer.setIndent("  "); // https://stackoverflow.com/questions/28758743/writing-pretty-print-json-output-to-fileoutput-stream-with-gson-jsonwriter
                writer.beginObject();
                writer.name("backupSnapshots");
                writer.beginArray();
                writer.endArray();
                writer.name("rollbackSnapshots");
                writer.beginArray();
                writer.endArray();
                writer.endObject();
            } catch (IOException e) {
                e.printStackTrace();
                BtrBack.LOGGER.error("Failed to create backups json file.");
                return false;
            }
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (br != null)
            rootObj = JsonParser.parseReader(br).getAsJsonObject();
        else return false;
        return true;
    }

    public static boolean addToBackups(String timeStamp) {
        JsonArray backupArray = rootObj.getAsJsonArray("backupSnapshots");
        backupArray.add(timeStamp);
        rootObj.remove("backupSnapshots");
        rootObj.add("backupSnapshots", backupArray);
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(rootObj);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile))) {
            writer.write(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean addToRollbacks(String timeStamp) {
        JsonArray rollbackArray = rootObj.getAsJsonArray("rollbackSnapshots");
        rollbackArray.add(timeStamp);
        rootObj.remove("rollbackSnapshots");
        rootObj.add("rollbackSnapshots", rollbackArray);
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(rootObj);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile))) {
            writer.write(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static ArrayList<String> listBackups(boolean includeIgnored) {
        ArrayList<String> list = new ArrayList<>();
        JsonArray backupArray = rootObj.getAsJsonArray("backupSnapshots");
        JsonArray rollbackArray = rootObj.getAsJsonArray("rollbackSnapshots");
        for (JsonElement timeStamp : backupArray)
            list.add(timeStamp.getAsString());
        if (includeIgnored)
            for (JsonElement timeStamp : rollbackArray)
                list.add(timeStamp.getAsString());
        list.sort((date1, date2) -> {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").parse(date1).compareTo(new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").parse(date2));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        });
        Collections.reverse(list);
        return list;
    }

    public static boolean removeRecord(String timestamp) {
        JsonArray backupArray = rootObj.getAsJsonArray("backupSnapshots");
        JsonArray rollbackArray = rootObj.getAsJsonArray("rollbackSnapshots");
        boolean a = backupArray.remove(new JsonPrimitive(timestamp));
        boolean b = rollbackArray.remove(new JsonPrimitive(timestamp));
        if (!a && !b)
            return false;
        rootObj.remove("backupSnapshots");
        rootObj.add("backupSnapshots", backupArray);
        rootObj.remove("rollbackSnapshots");
        rootObj.add("rollbackSnapshots", rollbackArray);
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(rootObj);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile))) {
            writer.write(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
