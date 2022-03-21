package net.himeki.btrback;

import com.google.gson.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class BtrRecord {
    Btrback plugin;
    JsonObject rootObj;
    File jsonFile;


    public BtrRecord(Btrback plugin) {
        this.plugin = plugin;
        jsonFile = new File(plugin.getRecordsJsonPath());
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(jsonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JsonParser parser = new JsonParser();
        assert br != null;
        this.rootObj = parser.parse(br).getAsJsonObject();
    }

    public boolean addToBackups(String timeStamp) {
        JsonArray backupArray = rootObj.getAsJsonArray("backupSnapshots");
        backupArray.add(timeStamp);
        rootObj.remove("backupSnapshots");
        rootObj.add("backupSnapshots", backupArray);
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(rootObj);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
            writer.write(jsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean addToRollbacks(String timeStamp) {
        JsonArray rollbackArray = rootObj.getAsJsonArray("rollbackSnapshots");
        rollbackArray.add(timeStamp);
        rootObj.remove("rollbackSnapshots");
        rootObj.add("rollbackSnapshots", rollbackArray);
        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(rootObj);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
            writer.write(jsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public ArrayList<String> listBackups(boolean includeIgnored) {
        ArrayList<String> list = new ArrayList<>();
        JsonArray backupArray = rootObj.getAsJsonArray("backupSnapshots");
        JsonArray rollbackArray = rootObj.getAsJsonArray("rollbackSnapshots");
        for (JsonElement timeStamp : backupArray)
            list.add(timeStamp.getAsString());
        if (includeIgnored)
            for (JsonElement timeStamp : rollbackArray)
                list.add(timeStamp.getAsString());
        list.sort((date1,date2)-> {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(date1).compareTo(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(date2));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        });
        Collections.reverse(list);
        return list;
    }

    public boolean removeRecord(String timestamp) {
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
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
            writer.write(jsonString);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
