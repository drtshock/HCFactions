package com.massivecraft.factions.scoreboards.sidebar;

import com.massivecraft.factions.*;
import com.massivecraft.factions.scoreboards.FSidebarProvider;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class FMapSidebar extends FSidebarProvider {

    @Override
    public String getTitle(FPlayer fplayer) {
        return ChatColor.BOLD + "     N     ";
    }

    @Override
    public List<String> getLines(FPlayer fplayer) {
        String box = "\u25A0"; // solid square
        List<String> lines = new ArrayList<>();
        Faction center = Board.getInstance().getFactionAt(fplayer.getLastStoodAt());
        int halfWidth = 9 / 2;
        int halfHeight = 9 / 2;
        FLocation topLeft = fplayer.getLastStoodAt().getRelative(-halfWidth, -halfHeight);
        int width = halfWidth * 2 + 1;
        int height = halfHeight * 2 + 1;
        for (int dz = 0; dz < height; dz++) {
            String row = "";
            for (int dx = 0; dx < width; dx++) {
                if (dx == halfWidth && dz == halfHeight) {
                    row += ChatColor.AQUA + box;
                } else {
                    FLocation flocationHere = topLeft.getRelative(dx, dz);
                    Faction here = Board.getInstance().getFactionAt(flocationHere);
                    Relation relation = fplayer.getFaction().getRelationTo(here);
                    if (here.isNone()) {
                        row += ChatColor.GRAY + box;
                    } else if (here.isSafeZone()) {
                        row += Conf.colorPeaceful + box;
                    } else if (here.isWarZone()) {
                        row += Conf.colorWar + box;
                    } else if (here == fplayer.getFaction() || here == center || relation.isAtLeast(Relation.ALLY)
                            || (Conf.showNeutralFactionsOnMap && relation.equals(Relation.NEUTRAL)) ||
                            (Conf.showEnemyFactionsOnMap && relation.equals(Relation.ENEMY))) {
                        row += here.getColorTo(fplayer.getFaction()) + box;
                    } else {
                        row += ChatColor.GRAY + box;
                    }
                }
            }
            lines.add(row);
        }
        return lines;
    }
}
