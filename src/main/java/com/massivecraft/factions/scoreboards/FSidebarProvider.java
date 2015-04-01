package com.massivecraft.factions.scoreboards;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.util.TagUtil;

import java.util.List;

public abstract class FSidebarProvider {

    public abstract String getTitle(FPlayer fplayer);

    public abstract List<String> getLines(FPlayer fplayer);

    public String replaceTags(FPlayer fplayer, String s) {
        return P.p.txt.parse(TagUtil.parsePlain(fplayer, s));
    }
}
