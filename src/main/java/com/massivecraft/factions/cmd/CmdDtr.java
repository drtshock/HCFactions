package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.DTRChangeEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;

public class CmdDtr extends FCommand {

    public CmdDtr() {
        super();
        this.aliases.add("dtr");

        this.requiredArgs.add("faction");
        this.requiredArgs.add("value");

        this.permission = Permission.DTR.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Faction targetFaction = this.argAsFaction(0);
        if (targetFaction == null) {
            return;
        }

        Double targetDtr = this.argAsDouble(1);
        String change;
        double max = targetFaction.getMaxDTR();
        double min = targetFaction.getMinDTR();

        if (targetDtr > max) {
            msg(TL.COMMAND_DTR_ERROR_MAX.format(TL.dc.format(max)));
            return;
        } else if (targetDtr < min) {
            msg(TL.COMMAND_DTR_ERROR_MIN.format(TL.dc.format(min)));
            return;
        } else {
            // needs own event call
            DTRChangeEvent changeEvent = new DTRChangeEvent(targetFaction, null, targetFaction.getDTR(), targetDtr);
            Bukkit.getServer().getPluginManager().callEvent(changeEvent);
            if (changeEvent.isCancelled()) {
                return;
            }
            targetFaction.setDTR(changeEvent.getTo());
            change = TL.COMMAND_DTR_SET.format(targetDtr);
            msg(TL.COMMAND_DTR_SUCCESS, change, targetFaction.describeTo(fme));
        }

        for (FPlayer fplayer : targetFaction.getFPlayersWhereOnline(true)) {
            if (fplayer == fme) {
                continue;
            }
            String blame = (fme == null ? TL.GENERIC_SERVERADMIN.toString() : fme.describeTo(fplayer, true));
            fplayer.msg(TL.COMMAND_DTR_FACTION, blame, change);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_DTR_DESCRIPTION;
    }
}
