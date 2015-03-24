package com.massivecraft.factions.scoreboards;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.zcore.util.TL;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.text.DecimalFormat;
import java.util.List;

public abstract class FSidebarProvider {
    
    private DecimalFormat dc = new DecimalFormat(TL.GENERIC_DECIMALFORMAT.toString());
    
    public abstract String getTitle(FPlayer fplayer);

    public abstract List<String> getLines(FPlayer fplayer);

    public String replaceTags(FPlayer fplayer, String s) {
        Faction faction = fplayer.getFaction();
        boolean raidable = faction.isRaidable();
        FPlayer fLeader = faction.getFPlayerAdmin();
        faction.updateDTR(); // update DTR before fetch, always
        String leader = fLeader == null ? "Server" : fLeader.getName().substring(0, fLeader.getName().length() > 14 ? 13 : fLeader.getName().length());
        if(Econ.isSetup()) {
            s = s.replace("{balance}", String.valueOf(Econ.getFriendlyBalance(fplayer.getPlayer().getUniqueId())));
        }
        s = s.replace("{name}", fplayer.getName());
        s = s.replace("{faction}", !faction.isNone() ? faction.getTag() : TL.GENERIC_FACTIONLESS.toString());
        s = s.replace("{dtr}", dc.format(faction.getDTR())).replace("{maxdtr}", dc.format(faction.getMaxDTR()));
        s = s.replace("{online}", String.valueOf(faction.getOnlinePlayers().size())).replace("{members}", String.valueOf(faction.getFPlayers().size()));
        s = s.replace("{leader}", String.valueOf(leader)).replace("{land}", String.valueOf(faction.getLand())).replace("{maxland}", String.valueOf(faction.getMaxLand()));
        s = s.replace("{raidable}", raidable ? TL.RAIDABLE_TRUE.toString() : TL.RAIDABLE_FALSE.toString());
        s = s.replace("{warps}", String.valueOf(faction.getWarps().size())).replace("{totalOnline}", String.valueOf(Bukkit.getServer().getOnlinePlayers().size()));
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
