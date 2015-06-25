package com.massivecraft.factions.zcore.persist.mongodb;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.persist.MemoryFaction;
import com.massivecraft.factions.zcore.persist.MemoryFactions;
import com.mongodb.*;
import com.mongodb.util.JSON;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MongoFactions extends MemoryFactions {
    // Info on how to persist
    private Gson gson;
    protected Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    private DBCollection collection;

    public void setDb(DB db) {
        collection = db.getCollection("Factions");
    }

    // -------------------------------------------- //
    // CONSTRUCTORS
    // -------------------------------------------- //

    public MongoFactions() {
        this.gson = P.p.gson;
        this.nextId = 1;
        setDb(P.p.getMongo());
        Bukkit.broadcastMessage("LEL");
    }

    public void forceSave() {
        Map<String, MongoFaction> entitiesThatShouldBeSaved = new HashMap<String, MongoFaction>();
        for (Faction entity : this.factions.values()) {
            entitiesThatShouldBeSaved.put(entity.getId(), (MongoFaction) entity);
        }
        this.saveCore(entitiesThatShouldBeSaved);
    }

    private boolean saveCore(Map<String, MongoFaction> entities) {
        collection.drop();
        for (MongoFaction faction : entities.values()) {
            DBObject object = new BasicDBObject();
            object.putAll((DBObject)JSON.parse(gson.toJson(faction)));
            collection.insert(object);
        }
        return true;
    }

    public void load() {
        Map<String, MongoFaction> factions = this.loadCore();
        if (factions == null) {
            return;
        }
        this.factions.putAll(factions);

        super.load();
        P.p.log("Loaded " + factions.size() + " Factions");
    }

    private Map<String, MongoFaction> loadCore() {
        if (collection.count()==0) {
            return new HashMap<String, MongoFaction>();
        }
        HashMap<String,MongoFaction> map = new HashMap<>();
        DBCursor dbCursor = collection.find();
        for (DBObject object : dbCursor) {
            MongoFaction faction = this.gson.fromJson(object.toString(),new TypeToken<MongoFaction>(){}.getType());
            map.put(faction.getId(),faction);
        }
        return map;
/*
        String content = collection.find().toString();
        if (content == null) {
            return null;
        }

        Map<String, MongoFaction> data = this.gson.fromJson(content, new TypeToken<Map<String, MongoFaction>>() {
        }.getType());

        this.nextId = 1;

        return data;
        */
    }

    private Set<String> whichKeysNeedMigration(Set<String> keys) {
        HashSet<String> list = new HashSet<String>();
        for (String value : keys) {
            if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                // Not a valid UUID..
                if (value.matches("[a-zA-Z0-9_]{2,16}")) {
                    // Valid playername, we'll mark this as one for conversion
                    // to UUID
                    list.add(value);
                }
            }
        }
        return list;
    }

    // -------------------------------------------- //
    // ID MANAGEMENT
    // -------------------------------------------- //

    public String getNextId() {
        while (!isIdFree(this.nextId)) {
            this.nextId += 1;
        }
        return Integer.toString(this.nextId);
    }

    public boolean isIdFree(String id) {
        return !this.factions.containsKey(id);
    }

    public boolean isIdFree(int id) {
        return this.isIdFree(Integer.toString(id));
    }

    protected synchronized void updateNextIdForId(int id) {
        if (this.nextId < id) {
            this.nextId = id + 1;
        }
    }

    protected void updateNextIdForId(String id) {
        try {
            int idAsInt = Integer.parseInt(id);
            this.updateNextIdForId(idAsInt);
        } catch (Exception ignored) {}
    }

    @Override
    public Faction generateFactionObject() {
        String id = getNextId();
        Faction faction = new MongoFaction(id);
        updateNextIdForId(id);
        return faction;
    }

    @Override
    public Faction generateFactionObject(String id) {
        return new MongoFaction(id);
    }

    @Override
    public void convertFrom(MemoryFactions old) {
        this.factions.putAll(Maps.transformValues(old.factions, new Function<Faction, MongoFaction>() {
            @Override
            public MongoFaction apply(Faction arg0) {
                return new MongoFaction((MemoryFaction) arg0);
            }
        }));
        this.nextId = old.nextId;
        forceSave();
        Factions.instance = this;
    }
}