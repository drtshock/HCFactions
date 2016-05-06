package com.massivecraft.factions.zcore.util;


import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.util.MiscUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.massivecraft.factions.zcore.util.TagReplacer.TagType;

public class TagUtil {

    private static final int ARBITRARY_LIMIT = 25000;

    /**
     * Replaces all variables in a plain raw line for a faction
     *
     * @param faction for faction
     * @param line    raw line from config with variables to replace for
     * @return clean line
     */
    public static String parsePlain(Faction faction, String line) {
        for (TagReplacer tagReplacer : TagReplacer.getByType(TagType.FACTION)) {
            if (tagReplacer.contains(line)) {
                line = tagReplacer.replace(line, tagReplacer.getValue(faction, null));
            }
        }
        return line;
    }

    /**
     * Replaces all variables in a plain raw line for a player
     *
     * @param fplayer for player
     * @param line    raw line from config with variables to replace for
     * @return clean line
     */
    public static String parsePlain(FPlayer fplayer, String line) {
        for (TagReplacer tagReplacer : TagReplacer.getByType(TagType.PLAYER)) {
            if (tagReplacer.contains(line)) {
                line = tagReplacer.replace(line, tagReplacer.getValue(fplayer.getFaction(), fplayer));
            }
        }
        return line;
    }

    /**
     * Replaces all variables in a plain raw line for a faction, using relations from fplayer
     *
     * @param faction for faction
     * @param fplayer from player
     * @param line    raw line from config with variables to replace for
     * @return clean line
     */
    public static String parsePlain(Faction faction, FPlayer fplayer, String line) {
        for (TagReplacer tagReplacer : TagReplacer.getByType(TagType.PLAYER)) {
            if (tagReplacer.contains(line)) {
                String value = tagReplacer.getValue(faction, fplayer);
                if (value != null) {
                    line = tagReplacer.replace(line, value);
                } else {
                    return null; // minimal show, entire line to be ignored
                }
            }
        }
        return line;
    }

    /**
     * Scan a line and parse the fancy variable into a fancy list
     *
     * @param faction for faction (viewers faction)
     * @param fme     for player (viewer)
     * @param line    fancy message prefix
     * @return
     */
    public static List<TextComponent> parseFancy(Faction faction, FPlayer fme, String line) {
        for (TagReplacer tagReplacer : TagReplacer.getByType(TagType.FANCY)) {
            if (tagReplacer.contains(line)) {
                String clean = line.replace(tagReplacer.getTag(), ""); // remove tag
                return getFancy(faction, fme, tagReplacer, clean);
            }
        }
        return null;
    }

    /**
     * Checks if a line has fancy variables
     *
     * @param line raw line from config with variables
     * @return if the line has fancy variables
     */
    public static boolean hasFancy(String line) {
        for (TagReplacer tagReplacer : TagReplacer.getByType(TagReplacer.TagType.FANCY)) {
            if (tagReplacer.contains(line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lets get fancy.
     *
     * @param target Faction to get relate from
     * @param fme    Player to relate to
     * @param prefix First part of the fancy message
     * @return list of fancy messages to send
     */
    protected static List<TextComponent> getFancy(Faction target, FPlayer fme, TagReplacer type, String prefix) {
        List<TextComponent> components = new ArrayList<>();
        boolean minimal = P.p.getConfig().getBoolean("minimal-show", false);

        switch (type) {
            case ALLIES_LIST:
                TextComponent allies = TextUtil.toFancy(prefix);

                boolean firstAlly = true;

                for (Faction otherFaction : Factions.getInstance().getAllFactions()) {
                    if (otherFaction == target) {
                        continue;
                    }

                    String s = otherFaction.getTag(fme);

                    if (otherFaction.getRelationTo(target).isAlly()) {
                        TextComponent next = new TextComponent(firstAlly ? s : ", " + s);
                        next.setColor(TextUtil.toColor(fme.getColorTo(otherFaction)));
                        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, toComponent(tipFaction(otherFaction))));
                        allies.addExtra(next);

                        firstAlly = false;

                        if (next.toLegacyText().length() > ARBITRARY_LIMIT) {
                            components.add(allies);
                            allies = new TextComponent("");
                        }
                    }
                }

                components.add(allies);
                return firstAlly && minimal ? null : components; // we must return here and not outside the switch
            case ENEMIES_LIST:
                TextComponent enemies = TextUtil.toFancy(prefix);
                boolean firstEnemy = true;

                for (Faction otherFaction : Factions.getInstance().getAllFactions()) {
                    if (otherFaction == target) {
                        continue;
                    }

                    String s = otherFaction.getTag(fme);

                    if (otherFaction.getRelationTo(target).isEnemy()) {
                        TextComponent next = new TextComponent(firstEnemy ? s : ", " + s);
                        next.setColor(TextUtil.toColor(fme.getColorTo(otherFaction)));
                        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, toComponent(tipFaction(otherFaction))));
                        enemies.addExtra(next);

                        firstEnemy = false;

                        if (next.toLegacyText().length() > ARBITRARY_LIMIT) {
                            components.add(enemies);
                            enemies = new TextComponent("");
                        }
                    }
                }

                components.add(enemies);
                return firstEnemy && minimal ? null : components; // we must return here and not outside the switch
            case ONLINE_LIST:
                TextComponent online = TextUtil.toFancy(prefix);
                boolean firstOnline = true;

                for (FPlayer p : MiscUtil.rankOrder(target.getFPlayersWhereOnline(true))) {
                    if (P.p.getConfig().getBoolean("hcf.omit-leader", false) && target.getFPlayerAdmin().getName().equals(p.getName())) {
                        continue;
                    }

                    String name = p.getNameAndTitle();
                    TextComponent next = new TextComponent(firstOnline ? name : ", " + name);
                    next.setColor(TextUtil.toColor(fme.getColorTo(p)));
                    next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, toComponent(tipPlayer(p))));
                    online.addExtra(next);

                    firstOnline = false;

                    if (next.toLegacyText().length() > ARBITRARY_LIMIT) {
                        components.add(online);
                        online = new TextComponent("");
                    }
                }

                components.add(online);
                return firstOnline && minimal ? null : components; // we must return here and not outside the switch
            case OFFLINE_LIST:
                TextComponent offline = TextUtil.toFancy(prefix);
                boolean firstOffline = true;

                for (FPlayer p : MiscUtil.rankOrder(target.getFPlayers())) {
                    if (P.p.getConfig().getBoolean("hcf.omit-leader", false) && target.getFPlayerAdmin().getName().equals(p.getName())) {
                        continue;
                    }

                    String name = p.getNameAndTitle();

                    if (!p.isOnline()) {
                        TextComponent next = new TextComponent(firstOffline ? name : ", " + name);
                        next.setColor(TextUtil.toColor(fme.getColorTo(p)));
                        next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, toComponent(tipPlayer(p))));
                        offline.addExtra(next);

                        firstOffline = false;

                        if (next.toLegacyText().length() > ARBITRARY_LIMIT) {
                            components.add(offline);
                            offline = new TextComponent("");
                        }
                    }
                }

                components.add(offline);
                return firstOffline && minimal ? null : components; // we must return here and not outside the switch
        }

        return null;
    }

    /**
     * Parses tooltip variables from config
     * <br>
     * Supports variables for factions only (type 2)
     *
     * @param faction faction to tooltip for
     * @return list of tooltips for a fancy message
     */
    private static List<String> tipFaction(Faction faction) {
        return P.p.getConfig().getStringList("tooltips.list").stream().map(line -> ChatColor.translateAlternateColorCodes('&', TagUtil.parsePlain(faction, line))).collect(Collectors.toList());
    }

    /**
     * Parses tooltip variables from config
     * <br>
     * Supports variables for players and factions (types 1 and 2)
     *
     * @param fplayer player to tooltip for
     * @return list of tooltips for a fancy message
     */
    private static List<String> tipPlayer(FPlayer fplayer) {
        return P.p.getConfig().getStringList("tooltips.show").stream().map(line -> ChatColor.translateAlternateColorCodes('&', TagUtil.parsePlain(fplayer, line))).collect(Collectors.toList());
    }

    private static BaseComponent[] toComponent(List<String> list) {
        String single = "";

        for (String tip : list) {
            single += tip + "\n";
        }

        return new ComponentBuilder(single).create();
    }
}
