package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.BitSet;
import java.util.List;

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
        final FLocation chunk = fme.getLastStoodAt();
        final long delay = P.p.getConfig().getLong("hcf.fstuck.delay", 60);

        if (P.p.getStuckMap().containsKey(player.getUniqueId())) {
            long wait = P.p.getTimers().get(player.getUniqueId()) - System.currentTimeMillis();
            String time = DurationFormatUtils.formatDuration(wait, TL.COMMAND_STUCK_TIMEFORMAT.toString(), true);
            msg(TL.COMMAND_STUCK_EXISTS, time);
        } else {
            final List<Integer> blackList = P.p.getConfig().getIntegerList("hcf.fstuck.black-list");

            final int id = Bukkit.getScheduler().runTaskLater(P.p, new BukkitRunnable() {

                @Override
                public void run() {
                    if (!P.p.getStuckMap().containsKey(player.getUniqueId())) {
                        return;
                    }
                    World world = chunk.getWorld();
                    int cx = (int) chunk.getX() << 4;
                    int cz = (int) chunk.getZ() << 4;

                    // Max iterations worse case: 16x16x4 (1024 times)
                    x : for (int x = cx; x < cx + 16; x++) {
                        z : for (int z = cz; z < cz + 16; z++) {
                            Block block = world.getHighestBlockAt(x, z);
                            Location loc = block.getLocation();

                            // make sure blocks under and above are okay
                            for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + 3; y++) {
                                Block check = world.getBlockAt(x, y, z);
                                if (blackList.contains(check.getTypeId())) {
                                    continue z; // skip this coordinate
                                }
                            }
                            P.p.debug("Teleported [" + fme.getName() + "] to x=" + loc.getBlockX() + ", y=" + loc.getBlockY() + ", z=" + loc.getBlockZ());
                            player.teleport(loc);
                            msg(TL.COMMAND_STUCK_TELEPORT, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                            P.p.getTimers().remove(player.getUniqueId());
                            P.p.getStuckMap().remove(player.getUniqueId());
                            return;
                        }
                    }
                    msg(TL.COMMAND_STUCK_NOTELEPORT.toString());
                }
            }, delay * 20).getTaskId();

            P.p.getTimers().put(player.getUniqueId(), System.currentTimeMillis() + (delay * 1000));
            long wait = P.p.getTimers().get(player.getUniqueId()) - System.currentTimeMillis();
            String time = DurationFormatUtils.formatDuration(wait, TL.COMMAND_STUCK_TIMEFORMAT.toString(), true);
            msg(TL.COMMAND_STUCK_START, time);
            P.p.getStuckMap().put(player.getUniqueId(), id);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_STUCK_DESCRIPTION;
    }
}
