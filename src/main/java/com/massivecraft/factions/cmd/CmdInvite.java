package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class CmdInvite extends FCommand {

    public CmdInvite() {
        super();
        this.aliases.add("invite");
        this.aliases.add("inv");

        this.requiredArgs.add("player name");
        //this.optionalArgs.put("", "");

        this.permission = Permission.INVITE.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null) {
            return;
        }

        if (you.getFaction() == myFaction) {
            msg(TL.COMMAND_INVITE_ALREADYMEMBER, you.getName(), myFaction.getTag());
            msg(TL.GENERIC_YOUMAYWANT.toString() + p.cmdBase.cmdKick.getUseageTemplate(false));
            return;
        }

        // prevent inviting players to faction if enabled
        if (!P.p.getConfig().getBoolean("hcf.dtr.freeze-join", true) && myFaction.isFrozen()) {
            msg(TL.COMMAND_INVITE_FROZEN.format(you.getName()));
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostInvite, TL.COMMAND_INVITE_TOINVITE.toString(), TL.COMMAND_INVITE_FORINVITE.toString())) {
            return;
        }

        myFaction.invite(you);
        if (!you.isOnline()) {
            return;
        }

        HoverEvent tooltip = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(TL.COMMAND_INVITE_CLICKTOJOIN.toString()).create());
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Conf.baseCommandAliases.get(0) + " join " + myFaction.getTag());

        TextComponent base = new TextComponent(fme.describeTo(you, true));
        base.setHoverEvent(tooltip);
        base.setClickEvent(clickEvent);

        TextComponent next = new TextComponent(TL.COMMAND_INVITE_INVITEDYOU.toString());
        next.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        next.setHoverEvent(tooltip);
        next.setClickEvent(clickEvent);
        base.addExtra(next);

        TextComponent next2 = new TextComponent(myFaction.describeTo(you));
        next2.setHoverEvent(tooltip);
        next2.setClickEvent(clickEvent);
        base.addExtra(next2);

        you.getPlayer().spigot().sendMessage(base);

        //you.msg("%s<i> invited you to %s", fme.describeTo(you, true), myFaction.describeTo(you));
        myFaction.msg(TL.COMMAND_INVITE_INVITED, fme.describeTo(myFaction, true), you.describeTo(myFaction));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_INVITE_DESCRIPTION;
    }

}
