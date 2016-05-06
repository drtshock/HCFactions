package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class CmdShowInvites extends FCommand {

    public CmdShowInvites() {
        super();
        aliases.add("showinvites");
        permission = Permission.SHOW_INVITES.node;

        senderMustBePlayer = true;
        senderMustBeMember = true;
    }

    @Override
    public void perform() {
        TextComponent base = new TextComponent();
        base.setColor(ChatColor.GOLD);

        for (String id : myFaction.getInvites()) {
            FPlayer fp = FPlayers.getInstance().getById(id);
            String name = fp != null ? fp.getName() : id;

            TextComponent invite = new TextComponent(name + "");
            invite.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(TL.COMMAND_SHOWINVITES_CLICKTOREVOKE.format(name)).create()));
            invite.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Conf.baseCommandAliases.get(0) + " deinvite " + name));
            invite.setColor(ChatColor.WHITE);
            base.addExtra(invite);
        }

        fme.getPlayer().spigot().sendMessage(base);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_SHOWINVITES_DESCRIPTION;
    }


}
