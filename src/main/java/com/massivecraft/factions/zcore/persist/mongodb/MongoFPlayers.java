package com.massivecraft.factions.zcore.persist.mongodb;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.persist.MemoryFPlayer;
import com.massivecraft.factions.zcore.persist.MemoryFPlayers;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.bson.BSONObject;

import java.util.HashMap;
import java.util.Map;

public class MongoFPlayers extends MemoryFPlayers {
    // Info on how to persist
    private Gson gson;
    private final DB db;

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public MongoFPlayers() {
        gson = P.p.gson;
        db = P.p.getMongo();
    }

    public void convertFrom(MemoryFPlayers old) {
        this.fPlayers.putAll(Maps.transformValues(old.fPlayers, new Function<FPlayer, MongoFPlayer>() {
            @Override
            public MongoFPlayer apply(FPlayer arg0) {
                return new MongoFPlayer((MemoryFPlayer) arg0);
            }
        }));
        forceSave();
        FPlayers.instance = this;
    }

    public void forceSave() {
        Map<String, MongoFPlayer> entitiesThatShouldBeSaved = new HashMap<>();
        for (FPlayer entity : this.fPlayers.values()) {
            if (((MemoryFPlayer) entity).shouldBeSaved()) {
                entitiesThatShouldBeSaved.put(entity.getId(), (MongoFPlayer) entity);
            }
        }
        this.saveCore(entitiesThatShouldBeSaved);
    }

    private boolean saveCore(Map<String, MongoFPlayer> data) {
        DBCollection players = db.getCollection("Players");
        players.drop();
        for (MongoFPlayer fPlayer : data.values()) {
            BasicDBObject object = new BasicDBObject();
            object.putAll((BSONObject) JSON.parse(this.gson.toJson(fPlayer)));
            players.insert(object);
        }
        return true;
    }

    public void load() {
        Map<String, MongoFPlayer> fplayers = this.loadCore();
        if (fplayers == null) {
            return;
        }
        this.fPlayers.clear();
        this.fPlayers.putAll(fplayers);
        P.p.log("Loaded " + fPlayers.size() + " players");
    }

    private Map<String, MongoFPlayer> loadCore() {
        DBCollection dbCollection = db.getCollection("Players");
        if (dbCollection.count()==0) {
            return new HashMap<>();
        }
        Map<String,MongoFPlayer> map = new HashMap<>();
        for (DBObject object : dbCollection.find()) {
            MongoFPlayer player = this.gson.fromJson(object.toString(),new TypeToken<MongoFPlayer>(){}.getType());
            map.put(player.getId(),player);
        }
        return map;
    }

    @Override
    public FPlayer generateFPlayer(String id) {
        FPlayer player = new MongoFPlayer(id);
        this.fPlayers.put(player.getId(), player);
        return player;
    }
}
