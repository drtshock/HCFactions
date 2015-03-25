package com.massivecraft.factions.cmd;


import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;

public class CmdThaw extends FCommand {

    public CmdThaw() {
        super();

        this.aliases.add("thaw");
        this.aliases.add("unfreeze");

        this.optionalArgs.put("faction tag", "yours");

        this.permission = Permission.THAW.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
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

        if(!faction.isFrozen()) {
            msg(TL.COMMAND_THAW_NOTFROZEN, faction.getTag());
            return;
        }

        faction.thaw();

        if (!faction.isFrozen()) {
            msg(TL.COMMAND_THAW_SUCCESS, faction.getTag());
        } else {
            msg(TL.COMMAND_THAW_FAILURE, faction.getTag());
            return;
        }

        for (FPlayer fplayer : faction.getFPlayersWhereOnline(true)) {
            if (fplayer == fme) {
                continue;
            }
            String blame = (fme == null ? TL.GENERIC_SERVERADMIN.toString() : fme.describeTo(fplayer, true));
            fplayer.msg(TL.COMMAND_THAW_FACTION, blame);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_THAW_DESCRIPTION;
    }
}
