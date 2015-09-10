package com.massivecraft.factions.zcore;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Essentials;
import com.massivecraft.factions.util.SpiralTask;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class StuckRequest implements Runnable {

    private Location sentAt;
    private FLocation chunk;
    private Player player;
    private FPlayer fPlayer;
    private long runtime;
    private int radiusSq;

    public StuckRequest(Player player, long delay, long time) {
        this.player = player;
        this.sentAt = player.getLocation();
        this.chunk = new FLocation(player.getLocation());
        this.fPlayer = FPlayers.getInstance().getByPlayer(player);
        this.radiusSq = P.p.getConfig().getInt("hcf.fstuck.radius", 10) * P.p.getConfig().getInt("hcf.fstuck.radius", 10);
        this.runtime = time + (delay * 1000);
        P.p.debug(this.toString()); // debug information <3
    }

    @Override
    public void run() {
        final int radius = P.p.getConfig().getInt("hcf.fstuck.radius", 10);

        if (!P.p.getStuckRequestMap().containsKey(player.getUniqueId())) {
            return;
        }

        // check for world difference or radius exceeding
        if (isOutsideRadius(player.getLocation())) {
            this.alert();
            this.cancel();
            return;
        }

        final Board board = Board.getInstance();
        final World world = chunk.getWorld();

        // spiral task to find nearest wilderness chunk
        new SpiralTask(new FLocation(player), radius * radius) {

            @Override
            public boolean work() {
                FLocation chunk = currentFLocation();
                Faction faction = board.getFactionAt(chunk);
                if (faction.isWilderness()) {
                    int cx = FLocation.chunkToBlock((int) chunk.getX());
                    int cz = FLocation.chunkToBlock((int) chunk.getZ());
                    int y = world.getHighestBlockYAt(cx, cz);
                    Location tp = new Location(world, cx, y, cz);

                    if (!Essentials.handleTeleport(player, tp)) {
                        player.teleport(tp); // does no safety checks
                        P.p.debug("/f stuck used regular teleport, not essentials!");
                    }

                    fPlayer.msg(TL.COMMAND_STUCK_TELEPORT, tp.getBlockX(), tp.getBlockY(), tp.getBlockZ());
                    StuckRequest.this.cancel();
                    this.stop();
                    return false;
                }
                return true;
            }
        };
    }

    public boolean isOutsideRadius(Location loc) {
        return loc.getWorld().getUID() != player.getWorld().getUID() || sentAt.distanceSquared(loc) > radiusSq;
    }

    public void cancel() {
        P.p.getStuckRequestMap().remove(player.getUniqueId()); // remove request
    }

    public void alert() {
        fPlayer.msg(TL.COMMAND_STUCK_OUTSIDE.format((int) Math.sqrt(radiusSq)));
    }

    public long getRemainingTime() {
        return this.runtime - System.currentTimeMillis();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StuckRequest{");
        sb.append("sentAt=").append(sentAt.getBlockX()).append(", ").append(sentAt.getBlockY()).append(", ").append(sentAt.getBlockZ());
        sb.append(", chunk=").append(chunk.toString());
        sb.append(", player=").append(player.getName());
        sb.append(", runtime=").append(runtime);
        sb.append(", radiusSq=").append(radiusSq).append("}");
        return sb.toString();
    }
}
