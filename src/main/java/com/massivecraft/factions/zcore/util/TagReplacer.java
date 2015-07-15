package com.massivecraft.factions.zcore.util;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Relation;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.itsmonkey.hcfessentials.utils.CooldownUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Link between config and in-game messages<br>
 * Changes based on faction / player<br>
 * Interfaces the config lists with {} variables to plugin
 */
public enum TagReplacer {

    /**
     * Fancy variables, used by f show
     */
    ALLIES_LIST(TagType.FANCY, "{allies}"),
    ONLINE_LIST(TagType.FANCY, "{online}"),
    ENEMIES_LIST(TagType.FANCY, "{enemies}"),
    OFFLINE_LIST(TagType.FANCY, "{offline}"),

    /**
     * Player variables, require a player
     */
    PLAYER_GROUP(TagType.PLAYER, "{group}"),
    LAST_SEEN(TagType.PLAYER, "{last-seen}"),
    PLAYER_BALANCE(TagType.PLAYER, "{player-balance}"),
    PLAYER_KILLS(TagType.PLAYER, "{player-kills}"),
    PLAYER_DEATHS(TagType.PLAYER, "{player-deaths}"),

    /**
     * HCFEssentials variables.
     */
    HCFE_ENDERPEARLTIME(TagType.PLAYER, "{hcfe-enderpearltime}"),
    HCFE_PVPTIMER(TagType.PLAYER, "{hcfe-pvptimer}"),
    HCFE_COMBATTAGTIME(TagType.PLAYER, "{hcfe-combattagtime}"),

    /**
     * Faction variables, require at least a player
     */
    DTR(TagType.FACTION, "{dtr}"),
    DTR_SYM(TagType.FACTION, "{dtr-sym}"),
    HOME_X(TagType.FACTION, "{x}"),
    HOME_Y(TagType.FACTION, "{y}"),
    HOME_Z(TagType.FACTION, "{z}"),
    LAND(TagType.FACTION, "{land}"),
    WARPS(TagType.FACTION, "{warps}"),
    COLOR(TagType.FACTION, "{color}"),
    HEADER(TagType.FACTION, "{header}"),
    LEADER(TagType.FACTION, "{leader}"),
    JOINING(TagType.FACTION, "{joining}"),
    MAX_DTR(TagType.FACTION, "{max-dtr}"),
    FACTION(TagType.FACTION, "{faction}"),
    PLAYER_NAME(TagType.FACTION, "{name}"),
    HOME_WORLD(TagType.FACTION, "{world}"),
    MAX_LAND(TagType.FACTION, "{max-land}"),
    RAIDABLE(TagType.FACTION, "{raidable}"),
    PEACEFUL(TagType.FACTION, "{peaceful}"),
    PERMANENT(TagType.FACTION, "permanent"), // no braces needed
    TIME_LEFT(TagType.FACTION, "{time-left}"),
    LAND_VALUE(TagType.FACTION, "{land-value}"),
    DESCRIPTION(TagType.FACTION, "{description}"),
    CREATE_DATE(TagType.FACTION, "{create-date}"),
    LAND_REFUND(TagType.FACTION, "{land-refund}"),
    BANK_BALANCE(TagType.FACTION, "{faction-balance}"),
    ALLIES_COUNT(TagType.FACTION, "{allies-count}"),
    ENEMIES_COUNT(TagType.FACTION, "{enemies-count}"),
    ONLINE_COUNT(TagType.FACTION, "{online-count}"),
    OFFLINE_COUNT(TagType.FACTION, "{offline-count}"),
    FACTION_SIZE(TagType.FACTION, "{faction-size}"),
    FACTION_KILLS(TagType.FACTION, "{faction-kills}"),
    FACTION_DEATHS(TagType.FACTION, "{faction-deaths}"),

    /**
     * General variables, require no faction or player
     */
    MAX_WARPS(TagType.GENERAL, "{max-warps}"),
    MAX_ALLIES(TagType.GENERAL, "{max-allies}"),
    MAX_ENEMIES(TagType.GENERAL, "{max-enemies}"),
    FACTIONLESS(TagType.GENERAL, "{factionless}"),
    TOTAL_ONLINE(TagType.GENERAL, "{total-online}"),
    FACTION_MAX(TagType.GENERAL, "{faction-max}");

    private TagType type;
    private String tag;

    protected enum TagType {
        FANCY(0), PLAYER(1), FACTION(2), GENERAL(3);
        public int id;

        TagType(int id) {
            this.id = id;
        }
    }

    TagReplacer(TagType type, String tag) {
        this.type = type;
        this.tag = tag;
    }

    /**
     * Protected access to this generic server related variable
     *
     * @return value for this generic server related variable<br>
     */
    protected String getValue() {
        switch (this) {
            case TOTAL_ONLINE:
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case FACTIONLESS:
                return String.valueOf(Factions.getInstance().getNone().getFPlayersWhereOnline(true).size());
            case MAX_ALLIES:
                if (P.p.getConfig().getBoolean("max-relations.enabled", true)) {
                    return String.valueOf(P.p.getConfig().getInt("max-relations.ally", 2));
                }
                return TL.GENERIC_INFINITY.toString();
            case MAX_ENEMIES:
                if (P.p.getConfig().getBoolean("max-relations.enabled", true)) {
                    return String.valueOf(P.p.getConfig().getInt("max-relations.enemy", 10));
                }
                return TL.GENERIC_INFINITY.toString();
            case MAX_WARPS:
                return String.valueOf(P.p.getConfig().getInt("max-warps", 5));
            case FACTION_MAX:
                return Conf.factionMemberLimit == 0 ? TL.GENERIC_INFINITY.toString() : String.valueOf(Conf.factionMemberLimit);
        }
        return null;
    }

    /**
     * Gets the value for this (as in the instance this is called from) variable!
     *
     * @param fac Target faction
     * @param fp  Target player (can be null)
     *
     * @return the value for this enum!
     */
    protected String getValue(Faction fac, FPlayer fp) {
        if (this.type == TagType.GENERAL) {
            return getValue();
        }

        String none = TL.NONE.toString();

        boolean minimal = P.p.getConfig().getBoolean("minimal-show", false);
        if (fp != null) {
            switch (this) {
                case HEADER:
                    return P.p.txt.titleize(fac.getTag(fp));
                case PLAYER_NAME:
                    return fp.getName();
                case COLOR:
                    return fac.getRelationTo(fp).getColor().toString();
                case FACTION:
                    return !fac.isNone() ? fac.getTag(fp) : TL.WILDERNESS.toString();
                case LAST_SEEN:
                    long lastSeen = System.currentTimeMillis() - fp.getLastLoginTime();
                    String humanized = DurationFormatUtils.formatDurationWords(lastSeen, true, true) + TL.COMMAND_STATUS_AGOSUFFIX;
                    return fp.isOnline() ? ChatColor.GREEN + TL.COMMAND_STATUS_ONLINE.toString() : (lastSeen < 432000000 ? ChatColor.YELLOW + humanized : ChatColor.RED + humanized);
                case PLAYER_GROUP:
                    return P.p.getPrimaryGroup(Bukkit.getOfflinePlayer(UUID.fromString(fp.getId())));
                case PLAYER_BALANCE:
                    return Econ.isSetup() ? Econ.getFriendlyBalance(fp) : TL.ECON_OFF.format("balance");
                case PLAYER_KILLS:
                    return String.valueOf(fp.getKills());
                case PLAYER_DEATHS:
                    return String.valueOf(fp.getDeaths());
                case HCFE_COMBATTAGTIME:
                    return CooldownUtils.combatTag.containsKey(fp.getPlayer().getUniqueId()) ? String.valueOf(CooldownUtils.combatTag.get(fp.getPlayer().getUniqueId())) : none;
                case HCFE_ENDERPEARLTIME:
                    return CooldownUtils.enderpearl.containsKey(fp.getPlayer().getUniqueId()) ? String.valueOf(CooldownUtils.combatTag.get(fp.getPlayer().getUniqueId())) : none;
                case HCFE_PVPTIMER:
                    return CooldownUtils.pvpTimer.containsKey(fp.getPlayer().getUniqueId()) ? String.valueOf(CooldownUtils.combatTag.get(fp.getPlayer().getUniqueId())) : none;
            }

        }
        switch (this) {
            case DESCRIPTION:
                return fac.getDescription();
            case FACTION:
                return fac.getTag();
            case JOINING:
                return (fac.getOpen() ? TL.COMMAND_SHOW_UNINVITED.toString() : TL.COMMAND_SHOW_INVITATION.toString());
            case PEACEFUL:
                return fac.isPeaceful() ? Conf.colorNeutral + TL.COMMAND_SHOW_PEACEFUL.toString() : "";
            case PERMANENT:
                return fac.isPermanent() ? "permanent" : "{notPermanent}";
            case LAND:
                return String.valueOf(fac.getLand());
            case MAX_LAND:
                return String.valueOf(fac.getMaxLand());
            case COLOR:
                return Relation.MEMBER.getColor().toString(); // relation to own faction is always member
            case LEADER:
                FPlayer fAdmin = fac.getFPlayerAdmin();
                return fAdmin == null ? "Server" : fAdmin.getName().substring(0, fAdmin.getName().length() > 14 ? 13 : fAdmin.getName().length());
            case WARPS:
                return String.valueOf(fac.getWarps().size());
            case DTR:
                fac.updateDTR();
                return TL.dc.format(fac.getDTR());
            case DTR_SYM:
                if (fac.getDTR() == fac.getMaxDTR()) {
                    return TL.COMMAND_SHOW_DTRSYM_MAX.toString();
                } else if (fac.isFrozen()) {
                    return TL.COMMAND_SHOW_DTRSYM_FROZEN.toString();
                } else if (fac.isRaidable()) {
                    return TL.COMMAND_SHOW_DTRSYM_RAIDABLE.toString();
                } else {
                    return TL.COMMAND_SHOW_DTRSYM_REGEN.toString();
                }
            case MAX_DTR:
                return TL.dc.format(fac.getMaxDTR());
            case FACTION_KILLS:
                return String.valueOf(fac.getKills());
            case FACTION_DEATHS:
                return String.valueOf(fac.getDeaths());
            case CREATE_DATE:
                return TL.sdf.format(fac.getFoundedDate());
            case RAIDABLE:
                return fac.isRaidable() ? TL.RAIDABLE_TRUE.toString() : TL.RAIDABLE_FALSE.toString();
            case HOME_WORLD:
                return fac.hasHome() ? fac.getHome().getWorld().getName() : minimal ? null : "{ig}";
            case HOME_X:
                return fac.hasHome() ? String.valueOf(fac.getHome().getBlockX()) : minimal ? null : "{ig}";
            case HOME_Y:
                return fac.hasHome() ? String.valueOf(fac.getHome().getBlockY()) : minimal ? null : "{ig}";
            case HOME_Z:
                return fac.hasHome() ? String.valueOf(fac.getHome().getBlockZ()) : minimal ? null : "{ig}";
            case TIME_LEFT:
                return fac.isFrozen() ? DurationFormatUtils.formatDuration(fac.getFreezeLeft(), TL.COMMAND_SHOW_FREEZEFORMAT.toString(), true) : "{notFrozen}";
            case LAND_VALUE:
                return Econ.shouldBeUsed() ? Econ.moneyString(Econ.calculateTotalLandValue(fac.getLand())) : minimal ? null : TL.ECON_OFF.format("value");
            case LAND_REFUND:
                return Econ.shouldBeUsed() ? Econ.moneyString(Econ.calculateTotalLandRefund(fac.getLand())) : minimal ? null : TL.ECON_OFF.format("refund");
            case BANK_BALANCE:
                if (Econ.shouldBeUsed()) {
                    return Conf.bankEnabled ? Econ.moneyString(Econ.getBalance(fac.getAccountId())) : minimal ? null : TL.ECON_OFF.format("balance");
                }
                return minimal ? null : TL.ECON_OFF.format("balance");
            case ALLIES_COUNT:
                return String.valueOf(fac.getRelationCount(Relation.ALLY));
            case ENEMIES_COUNT:
                return String.valueOf(fac.getRelationCount(Relation.ENEMY));
            case ONLINE_COUNT:
                return String.valueOf(fac.getOnlinePlayers().size());
            case OFFLINE_COUNT:
                return String.valueOf(fac.getFPlayers().size() - fac.getOnlinePlayers().size());
            case FACTION_SIZE:
                return String.valueOf(fac.getFPlayers().size());
        }
        return this.tag; // variable exists, is either a fancy message or something we missed
    }

    /**
     * Returns a list of all the variables we can use for this type<br>
     *
     * @param type the type we want
     *
     * @return a list of all the variables with this type
     */
    protected static List<TagReplacer> getByType(TagType type) {
        List<TagReplacer> tagReplacers = new ArrayList<TagReplacer>();
        for (TagReplacer tagReplacer : TagReplacer.values()) {
            if (type == TagType.FANCY) {
                if (tagReplacer.type == TagType.FANCY) {
                    tagReplacers.add(tagReplacer);
                }
            } else if (tagReplacer.type.id >= type.id) {
                tagReplacers.add(tagReplacer);
            }
        }
        return tagReplacers;
    }

    /**
     * @param original raw line with variables
     * @param value    what to replace var in raw line with
     *
     * @return the string with the new value
     */
    public String replace(String original, String value) {
        return original.replace(tag, value);
    }

    /**
     * @param toSearch raw line with variables
     *
     * @return if the raw line contains this enums variable
     */
    public boolean contains(String toSearch) {
        return toSearch.contains(tag);
    }

    /**
     * Gets the tag associated with this enum that we should replace
     *
     * @return the {....} variable that is located in config
     */
    public String getTag() {
        return this.tag;
    }
}
