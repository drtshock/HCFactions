package com.massivecraft.factions.zcore.persist.mongodb;

import com.massivecraft.factions.zcore.persist.MemoryFaction;

public class MongoFaction extends MemoryFaction {

    public MongoFaction(MemoryFaction arg0) {
        super(arg0);
    }

    public MongoFaction() {
    }

    public MongoFaction(String id) {
        super(id);
    }
}
