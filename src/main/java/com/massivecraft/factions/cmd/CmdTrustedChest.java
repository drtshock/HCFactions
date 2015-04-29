package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CmdTrustedChest extends FCommand {

    public Set<UUID> uuidSet = new HashSet<UUID>();

    public CmdTrustedChest() {
        this.aliases.add("trustedchest");
        this.aliases.add("tc");

        this.permission = Permission.TC.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
        senderMustBeMember = false;
    }

    @Override
    public void perform() {
        if (myFaction.isNormal()) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (uuidSet.contains(player.getUniqueId())) {
                    uuidSet.remove(player.getUniqueId());
                    msg(TL.COMMAND_TC_OFF);
                } else {
                    uuidSet.add(player.getUniqueId());
                    msg(TL.COMMAND_TC_ON);
                }
            }
        } else {
            msg(TL.COMMAND_TC_CANT, myFaction.getTag());
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_TC_DESCRIPTION;
    }
}
