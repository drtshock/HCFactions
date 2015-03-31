package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.zcore.util.TL;
import mkremins.fanciful.FancyMessage;
import org.apache.commons.lang.time.DurationFormatUtils;
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
        }
        if (faction == null) {
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
        String tag = faction.getTag(fme);
        List<String> fshow = P.p.getConfig().getStringList("fshow");
        faction.updateDTR();

        if (!faction.isNormal()) {
            // send header and that's all
            String header = fshow.get(0);
            if (header.contains("{header}")) {
                msg(p.txt.titleize(tag));
            } else {
                msg(p.txt.parse(header.replace("{tag}", tag)));
            }
            return;
        }

        for (String raw : P.p.getConfig().getStringList("fshow")) {
            if (raw.equals("{header}")) {
                refined.add(p.txt.titleize(tag));
                continue;
            }
            raw = raw.replace("{description}", faction.getDescription());
            raw = raw.replace("{joining}", joining);
            raw = raw.replace("{tag}", tag);
            raw = raw.replace("{peaceful?}", faction.isPeaceful() ? Conf.colorNeutral + TL.COMMAND_SHOW_PEACEFUL.toString() : "");
            raw = raw.replace("{land}", String.valueOf(faction.getLand())).replace("{maxland}", String.valueOf(faction.getMaxLand()));
            raw = raw.replace("{dtr}", dtr).replace("{maxdtr}", maxDtr).replace("{raidable}", raidable);
            raw = raw.replace("{createDate}", sdf.format(faction.getFoundedDate()));
            raw = raw.replace("{onlinecount}", String.valueOf(faction.getOnlinePlayers().size()));
            raw = raw.replace("{offlinecount}", String.valueOf(faction.getFPlayers().size() - faction.getOnlinePlayers().size()));
            raw = raw.replace("{factionSize}", String.valueOf(faction.getFPlayers().size()));

            if (faction.hasHome()) {
                Location home = faction.getHome();
                raw = raw.replace("{world}", home.getWorld().getName());
                raw = raw.replace("{x}", String.valueOf(home.getBlockX()));
                raw = raw.replace("{y}", String.valueOf(home.getBlockY()));
                raw = raw.replace("{z}", String.valueOf(home.getBlockZ()));
            } else {
                if (P.p.getConfig().getBoolean("hide-unused-fshow", true)) {
                    // having {x} and {z} is reasonable to determine if this line is home related
                    if (raw.contains("{x}") && raw.contains("{z}")) {
                        // faction has no home and we're hiding unused
                        continue;
                    }
                }
                // if no home exists, use home unset tl for "not set"
                raw = raw.replace("{world},", TL.COMMAND_SHOW_HOME_UNSET.toString()).replace("{x},", "").replace("{y},", "").replace("{z}", "");
            }

            if (faction.isFrozen()) {
                long left = faction.getFreezeLeft();
                raw = raw.replace("{timeleft}", DurationFormatUtils.formatDuration(left, TL.COMMAND_SHOW_FREEZEFORMAT.toString(), true));
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
            int maxAlly = p.getConfig().getBoolean("max-relations.enabled", false) ? p.getConfig().getInt("max-relations." + Relation.ALLY.toString(), -1) : -1;
            int maxEnemy = p.getConfig().getBoolean("max-relations.enabled", false) ? p.getConfig().getInt("max-relations." + Relation.ENEMY.toString(), -1) : -1;

            int allyCount = faction.getRelationCount(Relation.ALLY);
            int enemyCount = faction.getRelationCount(Relation.ENEMY);

            raw = raw.replace("{alliescount}", String.valueOf(allyCount));
            raw = raw.replace("{maxallies}", maxAlly < 0 ? TL.GENERIC_INFINITY.toString() : String.valueOf(maxAlly));
            raw = raw.replace("{enemiescount}", String.valueOf(enemyCount));
            raw = raw.replace("{maxenemies}", maxEnemy < 0 ? TL.GENERIC_INFINITY.toString() : String.valueOf(maxEnemy));

            boolean hide = P.p.getConfig().getBoolean("hide-unused-fshow", true);
            if (raw.contains("{allies}")) {
                if (allyCount == 0 && hide) {
                    continue;
                }
                refined.add(getAllies(faction, raw.replace("{allies}", " ")));
                continue;
            } else if (raw.contains("{enemies}")) {
                if (enemyCount == 0 && hide) {
                    continue;
                }
                refined.add(getEnemies(faction, raw.replace("{enemies}", " ")));
                continue;
            } else if (raw.contains("{online}")) {
                if (faction.getOnlinePlayers().size() == 0 && hide) {
                    continue;
                }
                refined.add(getOnline(faction, raw.replace("{online}", "")));
                continue;
            } else if (raw.contains("{offline}")) {
                List<FancyMessage> offline = getOffline(faction, raw.replace("{offline}", ""), hide);
                if (!hide) {
                    refined.add(offline);
                    continue;
                }
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

    private List<FancyMessage> getAllies(Faction faction, String pre) {
        List<FancyMessage> allies = new ArrayList<FancyMessage>();
        FancyMessage currentAllies = p.txt.parseFancy(pre);
        boolean firstAlly = true;
        for (Faction otherFaction : Factions.getInstance().getAllFactions()) {
            if (otherFaction == faction) {
                continue;
            }
            Relation rel = otherFaction.getRelationTo(faction);
            String s = otherFaction.getTag(fme);
            if (rel.isAlly()) {
                if (firstAlly) {
                    currentAllies.then(s);
                } else {
                    currentAllies.then(", " + s);
                }
                currentAllies.tooltip(getToolTips(otherFaction)).color(fme.getColorTo(otherFaction));
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

    private List<FancyMessage> getEnemies(Faction faction, String pre) {
        List<FancyMessage> enemies = new ArrayList<FancyMessage>();
        FancyMessage currentEnemies = p.txt.parseFancy(pre);
        boolean firstEnemy = true;
        for (Faction otherFaction : Factions.getInstance().getAllFactions()) {
            if (otherFaction == faction) {
                continue;
            }
            Relation rel = otherFaction.getRelationTo(faction);
            String s = otherFaction.getTag(fme);
            if (rel.isEnemy()) {
                if (firstEnemy) {
                    currentEnemies.then(s);
                } else {
                    currentEnemies.then(", " + s);
                }
                currentEnemies.tooltip(getToolTips(otherFaction)).color(fme.getColorTo(otherFaction));
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

    private List<FancyMessage> getOnline(Faction faction, String pre) {
        List<FancyMessage> online = new ArrayList<FancyMessage>();
        FancyMessage currentOnline = p.txt.parseFancy(pre);
        boolean firstOnline = true;
        for (FPlayer p : MiscUtil.rankOrder(faction.getFPlayersWhereOnline(true))) {
            String name = p.getNameAndTitle();
            if (firstOnline) {
                currentOnline.then(name);
            } else {
                currentOnline.then(", " + name);
            }
            currentOnline.tooltip(getToolTips(p)).color(fme.getColorTo(p));
            firstOnline = false;
            if (currentOnline.toJSONString().length() > ARBITRARY_LIMIT) {
                online.add(currentOnline);
                currentOnline = new FancyMessage();
            }
        }
        online.add(currentOnline);
        return online;
    }

    private List<FancyMessage> getOffline(Faction faction, String pre, boolean hide) {
        List<FancyMessage> offline = new ArrayList<FancyMessage>();
        FancyMessage currentOffline = p.txt.parseFancy(pre);
        boolean firstOffline = true;
        for (FPlayer p : MiscUtil.rankOrder(faction.getFPlayers())) {
            String name = p.getNameAndTitle();
            if (!p.isOnline()) {
                if (firstOffline) {
                    currentOffline.then(name);
                } else {
                    currentOffline.then(", " + name);
                }
                currentOffline.tooltip(getToolTips(p)).color(fme.getColorTo(p));
                firstOffline = false;
                if (currentOffline.toJSONString().length() > ARBITRARY_LIMIT) {
                    offline.add(currentOffline);
                    currentOffline = new FancyMessage();
                }
            }
        }
        // if we didnt add any offline players, set hide to true
        hide = firstOffline;
        offline.add(currentOffline);
        return offline;
    }
}