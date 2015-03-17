package com.massivecraft.factions.scoreboards.sidebar;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.scoreboards.FSidebarProvider;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.ListIterator;

public class FInfoSidebar extends FSidebarProvider {
    private final Faction faction;

    public FInfoSidebar(Faction faction) {
        this.faction = faction;
    }

    @Override
    public String getTitle(FPlayer fplayer) {
        return faction.getRelationTo(fplayer).getColor() + faction.getTag();
    }

    @Override
    public List<String> getLines(FPlayer fplayer) {
        List<String> lines = P.p.getConfig().getStringList("scoreboard.finfo");

        ListIterator<String> it = lines.listIterator();
        while (it.hasNext()) {
            it.set(replaceFInfoTags(it.next()));
        }

        return lines;
    }

    private String replaceFInfoTags(String s) {
        boolean raidable = faction.isRaidable();
        FPlayer fLeader = faction.getFPlayerAdmin();
        faction.updateDTR(); // update DTR before fetch, always
        String leader = fLeader == null ? "Server" : fLeader.getName().substring(0, fLeader.getName().length() > 14 ? 13 : fLeader.getName().length());
        s = s.replace("{dtr}", String.valueOf(faction.getDTR())).replace("{maxdtr}", String.valueOf(faction.getMaxDTR()));
        s = s.replace("{online}", String.valueOf(faction.getOnlinePlayers().size())).replace("{members}", String.valueOf(faction.getFPlayers().size()));
        s = s.replace("{leader}", String.valueOf(leader)).replace("{land}", String.valueOf(faction.getLand())).replace("{maxland}", String.valueOf(faction.getMaxLand()));
        s = s.replace("{raidable}", String.valueOf(raidable)).replace("{warps}", String.valueOf(faction.getWarps().size()));
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
