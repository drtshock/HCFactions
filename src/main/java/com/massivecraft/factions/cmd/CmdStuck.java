package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.P;
import com.massivecraft.factions.integration.Essentials;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class CmdStuck extends FCommand {

    private final static Random random = new Random();

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
        final Location sentAt = player.getLocation();
        final FLocation chunk = fme.getLastStoodAt();
        final long delay = P.p.getConfig().getLong("hcf.fstuck.delay", 60);
        final int radius = P.p.getConfig().getInt("hcf.fstuck.radius", 10);

        if (P.p.getStuckMap().containsKey(player.getUniqueId())) {
            long wait = P.p.getTimers().get(player.getUniqueId()) - System.currentTimeMillis();
            String time = DurationFormatUtils.formatDuration(wait, TL.COMMAND_STUCK_TIMEFORMAT.toString(), true);
            msg(TL.COMMAND_STUCK_EXISTS, time);
        } else {

            // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
            if (!payForCommand(Conf.econCostStuck, TL.COMMAND_STUCK_TOSTUCK.format(fme.getName()), TL.COMMAND_STUCK_FORSTUCK.format(fme.getName()))) {
                return;
            }

            final int id = Bukkit.getScheduler().runTaskLater(P.p, new BukkitRunnable() {

                @Override
                public void run() {
                    if (!P.p.getStuckMap().containsKey(player.getUniqueId())) {
                        return;
                    }

                    // check for world difference or radius exceeding
                    World world = chunk.getWorld();
                    if (world.getUID() != player.getWorld().getUID() || sentAt.distance(player.getLocation()) > radius) {
                        msg(TL.COMMAND_STUCK_OUTSIDE.format(radius));
                        P.p.getTimers().remove(player.getUniqueId());
                        P.p.getStuckMap().remove(player.getUniqueId());
                        return;
                    }

                    // pseudo random coord to get us out of most traps, +- 8 on the x and z
                    Location location = sentAt.add(random.nextInt(16) - 8, 1, random.nextInt(16) - 8);

                    // essentials safe teleport will take care of us :)
                    if (Essentials.safeTeleport(player, location)) {
                        Location loc = player.getLocation(); // players new location
                        msg(TL.COMMAND_STUCK_TELEPORT, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                        P.p.getTimers().remove(player.getUniqueId());
                        P.p.getStuckMap().remove(player.getUniqueId());
                    } else {
                        List<Integer> blackList = P.p.getConfig().getIntegerList("hcf.fstuck.black-list");
                        int cx = FLocation.chunkToBlock((int) chunk.getX());
                        int cz = FLocation.chunkToBlock((int) chunk.getZ());

                        for (int x = cx; x < cx + 16; x++) {
                            z:
                            for (int z = cz; z < cz + 16; z++) {
                                Block block = world.getHighestBlockAt(x, z);
                                Location loc = block.getLocation();

                                // make sure blocks under and above are okay
                                for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + 3; y++) {
                                    Block check = world.getBlockAt(x, y, z);
                                    if (blackList.contains(check.getTypeId())) {
                                        continue z; // skip this coordinate
                                    }
                                }

                                player.teleport(loc);
                                msg(TL.COMMAND_STUCK_TELEPORT, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                                P.p.getTimers().remove(player.getUniqueId());
                                P.p.getStuckMap().remove(player.getUniqueId());
                                return;
                            }
                        }
                        P.p.debug("Failed to teleport [" + fme.getName() + "] safely");
                        msg(TL.COMMAND_STUCK_NOTELEPORT.toString());
                        P.p.getTimers().remove(player.getUniqueId());
                        P.p.getStuckMap().remove(player.getUniqueId());
                    }
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
