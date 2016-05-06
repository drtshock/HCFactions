package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.zcore.util.TL;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class CmdFWarp extends FCommand {

    public CmdFWarp() {
        super();
        this.aliases.add("warp");
        this.aliases.add("warps");
        this.optionalArgs.put("warpname", "warpname");

        this.permission = Permission.WARP.node;
        this.senderMustBeMember = true;
        this.senderMustBeModerator = false;
    }

    @Override
    public void perform() {
        //TODO: check if in combat.
        if (args.size() == 0) {
            TextComponent base = new TextComponent(TL.COMMAND_FWARP_WARPS.toString());
            base.setColor(net.md_5.bungee.api.ChatColor.GOLD);

            Map<String, LazyLocation> warps = myFaction.getWarps();

            for (String s : warps.keySet()) {
                TextComponent warp = new TextComponent(s + " ");
                warp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(TL.COMMAND_FWARP_CLICKTOWARP.toString()).create()));
                warp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Conf.baseCommandAliases.get(0) + " warp " + s));
                warp.setColor(ChatColor.WHITE);
                base.addExtra(warp);
            }

            fme.getPlayer().spigot().sendMessage(base);
        } else if (args.size() > 1) {
            fme.msg(TL.COMMAND_FWARP_COMMANDFORMAT);
        } else {
            final String warpName = argAsString(0);
            if (myFaction.isWarp(argAsString(0))) {
                if (!transact(fme)) {
                    return;
                }
                final FPlayer fplayer = fme;
                final UUID uuid = fme.getPlayer().getUniqueId();
                this.doWarmUp(TL.WARMUPS_NOTIFY_TELEPORT, warpName, new Runnable() {
                    @Override
                    public void run() {
                        Player player = Bukkit.getPlayer(uuid);
                        player.teleport(fplayer.getFaction().getWarp(warpName).getLocation());
                        fplayer.msg(TL.COMMAND_FWARP_WARPED, warpName);
                    }
                }, this.p.getConfig().getLong("warmups.f-warp", 0));
            } else {
                fme.msg(TL.COMMAND_FWARP_INVALID, warpName);
            }
        }
    }

    private boolean transact(FPlayer player) {
        return !P.p.getConfig().getBoolean("warp-cost.enabled", false) || player.isAdminBypassing() || payForCommand(P.p.getConfig().getDouble("warp-cost.warp", 5), TL.COMMAND_FWARP_TOWARP.toString(), TL.COMMAND_FWARP_FORWARPING.toString());
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_FWARP_DESCRIPTION;
    }
}
