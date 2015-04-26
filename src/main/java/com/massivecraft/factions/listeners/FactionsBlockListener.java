package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.zcore.util.TL;
import com.massivecraft.factions.zcore.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityInteractEvent;

public class FactionsBlockListener implements Listener {

    public P p;

    public FactionsBlockListener(P p) {
        this.p = p;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.canBuild()) {
            return;
        }

        // special case for flint&steel, which should only be prevented by DenyUsage list
        if (event.getBlockPlaced().getType() == Material.FIRE) {
            return;
        }

        if (!playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "build", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getInstaBreak() && !playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        Material material = event.getBlock().getType();
        if (material == Material.SOIL) {
            if (event.getEntity() instanceof Horse) {
                Entity passenger = event.getEntity().getPassenger();
                if (passenger != null && passenger instanceof Player) {
                    Player player = (Player) passenger;
                    Faction faction = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));
                    FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                    Relation rel = fplayer.getRelationTo(faction);
                    if (faction.isNormal() && rel.isAtMost(Relation.NEUTRAL)) {
                        if (rel.confDenyUseage()) {
                            Faction myFaction = fplayer.getFaction();
                            fplayer.msg(TL.PLAYER_USE_TERRITORY, (material == Material.SOIL ? "trample " : "use ") + TextUtil.getMaterialName(material), faction.getTag(myFaction));
                            event.setCancelled(true);
                        }
                    }
                } else {
                    Faction faction = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));
                    if (faction.isNormal()) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!Conf.handleExploitLiquidFlow) {
            return;
        }
        if (event.getBlock().isLiquid()) {
            if (event.getToBlock().isEmpty()) {
                Faction from = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));
                Faction to = Board.getInstance().getFactionAt(new FLocation(event.getToBlock()));
                if (from == to) {
                    // not concerned with inter-faction events
                    return;
                }
                // from faction != to faction and to faction isn't raidable, cancel :)
                if (to.isNormal() && !to.isRaidable()) {
                    if (from.isNormal() && from.getRelationTo(to).isAlly()) {
                        return;
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!Conf.pistonProtectionThroughDenyBuild) {
            return;
        }

        Faction pistonFaction = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));

        // target end-of-the-line empty (air) block which is being pushed into, including if piston itself would extend into air
        Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getLength() + 1);

        // if potentially pushing into air/water/lava in another territory, we need to check it out
        if ((targetBlock.isEmpty() || targetBlock.isLiquid()) && !canPistonMoveBlock(pistonFaction, targetBlock.getLocation())) {
            event.setCancelled(true);
        }

		/*
         * note that I originally was testing the territory of each affected block, but since I found that pistons can only push
		 * up to 12 blocks and the width of any territory is 16 blocks, it should be safe (and much more lightweight) to test
		 * only the final target block as done above
		 */
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        // if not a sticky piston, retraction should be fine
        if (!event.isSticky() || !Conf.pistonProtectionThroughDenyBuild) {
            return;
        }

        Location targetLoc = event.getRetractLocation();
        Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(targetLoc));

        // Check if the piston is moving in a faction's territory. This disables pistons entirely in faction territory.
        if (otherFaction.isNormal() && P.p.getConfig().getBoolean("disable-pistons-in-territory", false)) {
            event.setCancelled(true);
            return;
        }

        // if potentially retracted block is just air/water/lava, no worries
        if (targetLoc.getBlock().isEmpty() || targetLoc.getBlock().isLiquid()) {
            return;
        }

        Faction pistonFaction = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));

        if (!canPistonMoveBlock(pistonFaction, targetLoc)) {
            event.setCancelled(true);
        }
    }

    private boolean canPistonMoveBlock(Faction pistonFaction, Location target) {
        Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(target));

        if (pistonFaction == otherFaction) {
            return true;
        }

        if (otherFaction.isNone()) {
            return !Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(target.getWorld().getName());
        } else if (otherFaction.isSafeZone()) {
            return !Conf.safeZoneDenyBuild;
        } else if (otherFaction.isWarZone()) {
            return !Conf.warZoneDenyBuild;
        }

        Relation rel = pistonFaction.getRelationTo(otherFaction);
        return !rel.confDenyBuild(otherFaction.hasPlayersOnline());

    }

    public static boolean playerCanBuildDestroyBlock(Player player, Location location, String action, boolean justCheck) {
        String name = player.getName();
        if (Conf.playersWhoBypassAllProtection.contains(name)) {
            return true;
        }

        FPlayer me = FPlayers.getInstance().getById(player.getUniqueId().toString());
        if (me.isAdminBypassing()) {
            return true;
        }

        FLocation loc = new FLocation(location);
        Faction otherFaction = Board.getInstance().getFactionAt(loc);

        if (otherFaction.isNone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(player, location)) {
                return true;
            }
            if (!Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(location.getWorld().getName())) {
                return true; // This is not faction territory. Use whatever you like here.
            }
            if (!justCheck) {
                me.msg(TL.PLAYER_ACTION_ZONE, action, TL.WILDERNESS.toString());
            }
            return false;
        } else if (otherFaction.isSafeZone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(player, location)) {
                return true;
            }
            if (!Conf.safeZoneDenyBuild || Permission.MANAGE_SAFE_ZONE.has(player)) {
                return true;
            }
            if (!justCheck) {
                me.msg(TL.PLAYER_ACTION_ZONE, action, TL.SAFEZONE.toString());
            }
            return false;
        } else if (otherFaction.isWarZone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(player, location)) {
                return true;
            }
            if (!Conf.warZoneDenyBuild || Permission.MANAGE_WAR_ZONE.has(player)) {
                return true;
            }
            if (!justCheck) {
                me.msg(TL.PLAYER_ACTION_ZONE, action, TL.WARZONE.toString());
            }
            return false;
        }
        // is faction raidable? 
        if (otherFaction.isRaidable()) {
            return true;
        }

        Faction myFaction = me.getFaction();
        Relation rel = myFaction.getRelationTo(otherFaction);
        boolean online = otherFaction.hasPlayersOnline();
        boolean pain = !justCheck && rel.confPainBuild(online);
        boolean deny = rel.confDenyBuild(online);

        // hurt the player for building/destroying in other territory?
        if (pain) {
            player.damage(Conf.actionDeniedPainAmount);
            if (!deny) {
                me.msg(TL.PLAYER_PAIN_CLAIM, action, otherFaction.getTag(myFaction));
            }
        }

        // cancel building/destroying in other territory?
        if (deny) {
            if (!justCheck) {
                me.msg(TL.PLAYER_ACTION_CLAIM, action, otherFaction.getTag(myFaction));
            }
            return false;
        }

        // Also cancel and/or cause pain if player doesn't have ownership rights for this claim
        if (Conf.ownedAreasEnabled && (Conf.ownedAreaDenyBuild || Conf.ownedAreaPainBuild) && !otherFaction.playerHasOwnershipRights(me, loc)) {
            if (!pain && Conf.ownedAreaPainBuild && !justCheck) {
                player.damage(Conf.actionDeniedPainAmount);

                if (!Conf.ownedAreaDenyBuild) {
                    me.msg(TL.PLAYER_PAIN_CLAIM, action, otherFaction.getTag(myFaction));
                }
            }
            if (Conf.ownedAreaDenyBuild) {
                if (!justCheck) {
                    me.msg(TL.PLAYER_ACTION_CLAIM, action, otherFaction.getTag(myFaction));
                }
                return false;
            }
        }
        return true;
    }
}
