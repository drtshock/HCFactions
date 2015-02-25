package com.massivecraft.factions.zcore.persist.json;

import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.zcore.persist.MemoryFPlayer;

public class JSONFPlayer extends MemoryFPlayer {

    public JSONFPlayer(MemoryFPlayer arg0) {
        super(arg0);
    }

    public JSONFPlayer(String id) {
        super(id);
    }

    @Override
    public void remove() {
        ((JSONFPlayers) FPlayers.getInstance()).fPlayers.remove(getId());
    }

    public boolean shouldBeSaved() {
        if (!this.hasFaction()) {
            return false;
        }
        return true;
    }
}
