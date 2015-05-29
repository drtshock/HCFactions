package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Essentials;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.SpiralTask;
import com.massivecraft.factions.zcore.StuckRequest;
import com.massivecraft.factions.zcore.util.TL;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class CmdStuck extends FCommand {

    public CmdStuck() {
        super();

        this.aliases.add("stuck");
        this.aliases.add("halp!"); // halp! c:

        this.permission = Permission.STUCK.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        final Player player = fme.getPlayer();
        final long delay = P.p.getConfig().getLong("hcf.fstuck.delay", 60);

        if (P.p.getStuckRequestMap().containsKey(player.getUniqueId())) {
            if(fme.isInOwnTerritory()) {
                msg(TL.COMMAND_STUCK_INOWNZONE);
                return; // don't waste cpu cycles if they're not really stuck
            }
            StuckRequest request = P.p.getStuckRequestMap().get(player.getUniqueId());
            String time = DurationFormatUtils.formatDuration(request.getRemainingTime(), TL.COMMAND_STUCK_TIMEFORMAT.toString(), true);
            msg(TL.COMMAND_STUCK_EXISTS, time);
        } else {
            // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
            if (!payForCommand(Conf.econCostStuck, TL.COMMAND_STUCK_TOSTUCK.format(fme.getName()), TL.COMMAND_STUCK_FORSTUCK.format(fme.getName()))) {
                return;
            }

            StuckRequest request = new StuckRequest(player, delay, System.currentTimeMillis());
            int taskId = Bukkit.getScheduler().runTaskLater(P.p, request, delay*20).getTaskId();
            request.setTaskid(taskId);

            P.p.getStuckRequestMap().put(player.getUniqueId(), request);

            String time = DurationFormatUtils.formatDuration(request.getRemainingTime(), TL.COMMAND_STUCK_TIMEFORMAT.toString(), true);
            msg(TL.COMMAND_STUCK_START, time);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_STUCK_DESCRIPTION;
    }
}


