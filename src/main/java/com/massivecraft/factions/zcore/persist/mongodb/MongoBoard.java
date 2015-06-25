package com.massivecraft.factions.zcore.persist.mongodb;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.persist.MemoryBoard;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import java.util.Map.Entry;


public class MongoBoard extends MemoryBoard {
    private final DBCollection dbCollection;
    public MongoBoard() {
        dbCollection = P.p.getMongo().getCollection("Board");
    }

    // -------------------------------------------- //
    // Persistance
    // -------------------------------------------- //

    public BasicDBList dumpAsSaveFormat() {
        /*
        Map<String, Map<String, String>> worldCoordIds = new HashMap<String, Map<String, String>>();

        String worldName, coords;
        String id;

        for (Entry<FLocation, String> entry : flocationIds.entrySet()) {
            worldName = entry.getKey().getWorldName();
            coords = entry.getKey().getCoordString();
            id = entry.getValue();
            if (!worldCoordIds.containsKey(worldName)) {
                worldCoordIds.put(worldName, new TreeMap<String, String>());
            }

            worldCoordIds.get(worldName).put(coords, id);
        }

        return worldCoordIds;
        */
        BasicDBList list = new BasicDBList();
        for (Entry<FLocation, String> entry : flocationIds.entrySet()) {
            list.put(entry.getValue(),entry.getKey().toString());
        }
        return list;
    }

    public void loadFromSaveFormat(BasicDBList list) {
        try {
            for (String key : list.keySet()) {
                FLocation fLocation = FLocation.fromString(list.get(key).toString());
                flocationIds.put(fLocation, key);
            }
        } catch (NullPointerException ex) {
            ex.getSuppressed();
        }
        /*
        flocationIds.clear();

        String worldName;
        String[] coords;
        int x, z;
        String factionId;

        for (Entry<String, Map<String, String>> entry : worldCoordIds.entrySet()) {
            worldName = entry.getKey();
            Bukkit.broadcastMessage("1: " + entry.getKey());
            Bukkit.broadcastMessage("2: " + entry.getValue().keySet());
            Bukkit.broadcastMessage("3: " + entry.getValue().values());
            for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
                coords = entry2.getKey().trim().split("[,\\s]+");
                x = Integer.parseInt(coords[0]);
                z = Integer.parseInt(coords[1]);
                factionId = entry2.getValue();
                flocationIds.put(new FLocation(worldName, x, z), factionId);
            }
        }
        */
    }

    public boolean forceSave() {
        dbCollection.dropIndexes();
        dbCollection.insert(new BasicDBObject("Data",dumpAsSaveFormat()));
        return true;
    }

    public boolean load() {
        if (dbCollection.count() == 0) {
            return false;
        }
        P.p.log("Loading board from database");
        loadFromSaveFormat((BasicDBList) dbCollection.findOne().get("Data"));
        P.p.log("Loaded " + flocationIds.size() + " board locations");

        return true;
    }

    @Override
    public void convertFrom(MemoryBoard old) {
        this.flocationIds = old.flocationIds;
        forceSave();
        Board.instance = this;
    }
}
