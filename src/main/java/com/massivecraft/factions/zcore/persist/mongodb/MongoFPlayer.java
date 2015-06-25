package com.massivecraft.factions.zcore.persist.mongodb;

import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.zcore.persist.MemoryFPlayer;

public class MongoFPlayer extends MemoryFPlayer {

    public MongoFPlayer(MemoryFPlayer arg0) {
        super(arg0);
    }

    public MongoFPlayer(String id) {
        super(id);
    }

    @Override
    public void remove() {
        ((MongoFPlayers) FPlayers.getInstance()).fPlayers.remove(getId());
    }

    public boolean shouldBeSaved() {
        return this.hasFaction();
    }
}
