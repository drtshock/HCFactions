package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.util.TL;

public class FRoleCommand extends FCommand {

    public Role targetRole;

    public FRoleCommand() {
        super();
        this.requiredArgs.add("player name");

        this.permission = Permission.ROLE_ANY.node; // TODO: FIX
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        FPlayer them = super.argAsFPlayer(0);
        if (them == null) {
            return;
        }

        Faction targetFaction = them.getFaction();

        boolean sameFaction = myFaction == them.getFaction();


        // factions.role.<role>.any
        boolean permAny = P.perms.has(sender, Permission.from(targetRole).any());

        if (targetFaction != myFaction && !permAny) {
            msg(TL.COMMAND_ROLE_NOTMEMBER, them.describeTo(fme, true));
            return;
        }

        if (sameFaction && fme.getRole().isAtMost(Role.NORMAL)) {
            // cant set a role. Same faction and role in faction isn't mod or admin
            return;
        }

        if (targetRole == Role.UNTRUSTED) {
            // must be admin to set mod to untrusted

            if (them.getRole() == Role.MODERATOR) {

            }
        } else if (targetRole == Role.NORMAL) {
            // must be admin to set mod to normal
        } else if (targetRole == Role.MODERATOR) {

            if (them.getRole() == Role.MODERATOR) {
                them.setRole(Role.NORMAL); // demote to normal
            } else {

            }
            // must be admin to set anyone to mod
        } else if (targetRole == Role.ADMIN) {
            // must be admin to set new admin
        }

        /**
         * Admin can set other admin, can set mod, can set normal, can set untrusted
         * Mod can set normal, can set untrusted
         */

        //boolean permAny = Permission.valueOf("ROLE_" + targetRole.name() + "_ANY").has(sender, false);
        if (them.getFaction() != myFaction && !permAny) {
            msg(TL.valueOf("COMMAND_ROLE_" + targetRole.name() + "_NOTMEMBER"), them.describeTo(fme, true));
            return;
        }

        if (fme != null && fme.hasFaction() && fme.getRole().isAtMost(Role.NORMAL)) {
            // you're not a mod or admin
            return;
        }

        if (fme != null && fme.getRole() != Role.ADMIN && !permAny) {
            msg(TL.COMMAND_ADMIN_NOTADMIN);
            return;
        }

        if (them == fme && !permAny) {
            msg(TL.COMMAND_ADMIN_TARGETSELF);
            return;
        }


    }

    @Override
    public TL getUsageTranslation() {
        return null;
    }
}
