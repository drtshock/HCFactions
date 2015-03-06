package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.zcore.util.TL;

import mkremins.fanciful.FancyMessage;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class CmdShow extends FCommand {

    private static final int ARBITRARY_LIMIT = 25000;

    public CmdShow() {
        this.aliases.add("show");
        this.aliases.add("who");

        this.optionalArgs.put("faction tag", "yours");

        this.permission = Permission.SHOW.node;
        this.disableOnLock = false;

        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Faction faction = myFaction;
        if (this.argIsSet(0)) {
            faction = this.argAsFaction(0);
        } else if (faction == null) {
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this
        // command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostShow, TL.COMMAND_SHOW_TOSHOW, TL.COMMAND_SHOW_FORSHOW)) {
            return;
        }

        List<Object> refined = new ArrayList<Object>();

        String joining = (faction.getOpen() ? TL.COMMAND_SHOW_UNINVITED.toString() : TL.COMMAND_SHOW_INVITATION.toString());
        String dtr = dc.format(faction.getDTR());
        String maxDtr = dc.format(faction.getMaxDTR());
        String raidable = faction.isRaidable() ? TL.RAIDABLE_TRUE.toString() : TL.RAIDABLE_FALSE.toString();

        if (fme != null) {
            fme.updateDTR();
        }
        faction.updateDTR();

        // we'll send title now, rest soon :)
        msg(p.txt.titleize(faction.getTag(fme)));

        if (!faction.isNormal()) {
            return;
        }

        for (String raw : P.p.getConfig().getStringList("fshow")) {
            raw = raw.replace("{description}", faction.getDescription());
            raw = raw.replace("{joining}", joining);
            raw = raw.replace("{peaceful?}", faction.isPeaceful() ? Conf.colorNeutral + TL.COMMAND_SHOW_PEACEFUL.toString() : "");
            raw = raw.replace("{land}", String.valueOf(faction.getLand())).replace("{maxland}", String.valueOf(faction.getMaxLand()));
            raw = raw.replace("{dtr}", dtr).replace("{maxdtr}", maxDtr).replace("{raidable}", raidable);

            if (faction.hasHome()) {
                Location home = faction.getHome();
                raw = raw.replace("{world}", home.getWorld().getName());
                raw = raw.replace("{x}", String.valueOf(home.getBlockX()));
                raw = raw.replace("{y}", String.valueOf(home.getBlockY()));
                raw = raw.replace("{z}", String.valueOf(home.getBlockZ()));
            } else {
                // if no home exists, use home unset tl for "not set"
                raw = raw.replace("{world},", TL.COMMAND_SHOW_HOME_UNSET.toString()).replace("{x},", "").replace("{y},", "").replace("{z}", "");
            }

            if (faction.isFrozen()) {
                long left = faction.getFreezeLeft();
                raw = raw.replace("{timeleft}", DurationFormatUtils.formatDuration(left, "mm:ss", true));
            } else {
                // faction is not frozen, so we ignore this raw line
                if (raw.contains("{timeleft}")) {
                    continue;
                }
            }

            if (!faction.isPermanent()) {
                // faction is not permanent, so we ignore this raw line
                if (raw.contains("permanent")) {
                    continue;
                }
            }


            // if line involves economy variables, check if we economy should be used
            if (raw.contains("{value}") || raw.contains("{refund}") || raw.contains("{balance}")) {
                if (Econ.shouldBeUsed()) {
                    double value = Econ.calculateTotalLandValue(faction.getLand());
                    double refund = value * Conf.econClaimRefundMultiplier;
                    if (value > 0) {
                        String stringValue = Econ.moneyString(value);
                        String stringRefund = (refund > 0.0) ? (TL.COMMAND_SHOW_DEPRECIATED.format(Econ.moneyString(refund))) : "";
                        raw = raw.replace("{value}", stringValue).replace("{refund}", stringRefund);
                    }
                    if (Conf.bankEnabled) {
                        raw = raw.replace("{balance}", Econ.moneyString(Econ.getBalance(faction.getAccountId())));
                    }
                } else {
                    // line involves economy, but economy is disabled, so we ignore this raw line
                    continue;
                }
            }

            if (raw.contains("{allies}")) {
                raw = raw.replace("{allies}", "");
                refined.add(getAllies(faction, raw));
                continue;
            } else if (raw.contains("{enemies}")) {
                raw = raw.replace("{enemies}", "");
                refined.add(getEnemies(faction, raw));
                continue;
            } else if (raw.contains("{online}")) {
                raw = raw.replace("{online}", "");
                refined.add(getOnline(faction, raw));
                continue;
            } else if (raw.contains("{offline}")) {
                raw = raw.replace("{offline}", "");
                refined.add(getOffline(faction, raw));
                continue;
            }
            // finally, we add the send-able message to our output list
            refined.add(p.txt.parse(raw));
        }

        for (Object out : refined) {
            if (out instanceof String) {
                msg((String) out);
            } else if (out instanceof List<?>) {
                sendFancyMessage((List<FancyMessage>) out);
            }
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_SHOW_COMMANDDESCRIPTION;
    }

    /**
     * Gets list of allies to this faction
     * @param faction faction to analyze
     * @param pre     title of fancy message
     * @return list of fancy messages
     */
    private List<FancyMessage> getAllies(Faction faction, String pre) {
        List<FancyMessage> allies = new ArrayList<FancyMessage>();
        FancyMessage currentAllies = new FancyMessage(pre).color(ChatColor.GOLD);
        boolean firstAlly = true;
        for (Faction otherFaction : Factions.getInstance().getAllFactions()) {
            if (otherFaction == faction) {
                continue;
            }

            Relation rel = otherFaction.getRelationTo(faction);
            String s = otherFaction.getTag(fme);
            if (rel.isAlly()) {
                if (firstAlly) {
                    currentAllies.then(s).tooltip(getToolTips(otherFaction));
                } else {
                    currentAllies.then(", " + s).tooltip(getToolTips(otherFaction));
                }
                firstAlly = false;
                if (currentAllies.toJSONString().length() > ARBITRARY_LIMIT) {
                    allies.add(currentAllies);
                    currentAllies = new FancyMessage();
                }
            } 
        }
        allies.add(currentAllies);
        return allies;
    }

    /**
     * Gets a list of enemies to this faction
     * @param faction faction to analyze
     * @param pre     title of fancy message
     * @return list of fancy messages
     */
    private List<FancyMessage> getEnemies(Faction faction, String pre) {
        List<FancyMessage> enemies = new ArrayList<FancyMessage>();
        FancyMessage currentEnemies = new FancyMessage(pre).color(ChatColor.GOLD);
        boolean firstEnemy = true;
        for (Faction otherFaction : Factions.getInstance().getAllFactions()) {
            if (otherFaction == faction) {
                continue;
            }
            Relation rel = otherFaction.getRelationTo(faction);
            String s = otherFaction.getTag(fme);
            if (rel.isEnemy()) {
                if (firstEnemy) {
                    currentEnemies.then(s).tooltip(getToolTips(otherFaction));
                } else {
                    currentEnemies.then(", " + s).tooltip(getToolTips(otherFaction));
                }
                firstEnemy = false;
                if (currentEnemies.toJSONString().length() > ARBITRARY_LIMIT) {
                    enemies.add(currentEnemies);
                    currentEnemies = new FancyMessage();
                }
            }
        }
        enemies.add(currentEnemies);
        return enemies;
    }

    /**
     * Gets list of online faction members
     * @param faction faction to analyze
     * @param pre     title of fancy message
     * @return list of fancy messages
     */
    private List<FancyMessage> getOnline(Faction faction, String pre) {
        List<FancyMessage> online = new ArrayList<FancyMessage>();
        FancyMessage currentOnline = new FancyMessage(pre).color(ChatColor.GOLD);
        boolean firstOnline = true;
        for (FPlayer p : MiscUtil.rankOrder(faction.getFPlayers())) {
            String name = p.getNameAndTitle();
            if (p.isOnline()) {
                if (firstOnline) {
                    currentOnline.then(name).tooltip(getToolTips(p));
                } else {
                    currentOnline.then(", " + name).tooltip(getToolTips(p));
                }
                firstOnline = false;
                if (currentOnline.toJSONString().length() > ARBITRARY_LIMIT) {
                    online.add(currentOnline);
                    currentOnline = new FancyMessage();
                }
            } 
        }
        online.add(currentOnline);
        return online;
    }

    /**
     * Gets offline faction members
     * @param faction faction to analyze
     * @param pre     title of fancy message
     * @return list of fancy messages
     */
    private List<FancyMessage> getOffline(Faction faction, String pre) {
        List<FancyMessage> offline = new ArrayList<FancyMessage>();
        FancyMessage currentOffline = new FancyMessage(pre).color(ChatColor.GOLD);
        boolean firstOffline = true;
        for (FPlayer p : MiscUtil.rankOrder(faction.getFPlayers())) {
            String name = p.getNameAndTitle();
            if (!p.isOnline()) {
                if (firstOffline) {
                    currentOffline.then(name).tooltip(getToolTips(p));
                } else {
                    currentOffline.then(", " + name).tooltip(getToolTips(p));
                }
                firstOffline = false;
                if (currentOffline.toJSONString().length() > ARBITRARY_LIMIT) {
                    offline.add(currentOffline);
                    currentOffline = new FancyMessage();
                }
            }
        }
        offline.add(currentOffline);
        return offline;
    }
}