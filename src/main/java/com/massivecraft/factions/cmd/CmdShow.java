package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import com.massivecraft.factions.zcore.util.TagUtil;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

public class CmdShow extends FCommand {

    public CmdShow() {
        this.aliases.add("show");
        this.aliases.add("who");

        this.optionalArgs.put("faction tag", "yours");

        this.permission = Permission.SHOW.node;
        this.disableOnLock = false;

        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        Faction faction = myFaction;
        if (this.argIsSet(0)) {
            faction = this.argAsFaction(0);
        }

        if (faction == null) {
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostShow, TL.COMMAND_SHOW_TOSHOW, TL.COMMAND_SHOW_FORSHOW)) {
            return;
        }

        List<String> show = P.p.getConfig().getStringList("show");

        faction.updateDTR(); // get this out of way early

        // handle separate /f show for permanent and player-less factions
        if(faction.isNormal()) {
            if (faction.isPermanent() && faction.getFPlayers().size() == 0) {
                List<String> perm_show = P.p.getConfig().getStringList("permanent-show");

                if (perm_show != null && !perm_show.isEmpty()) {
                    show = perm_show; // we can show the permanent show from config
                }
            }
        } else {
            msg(p.txt.parse(TagUtil.parsePlain(faction, fme, show.get(0))));
            return; // we only show first line for non-normal factions
        }

        for (String raw : show) {
            String parsed = TagUtil.parsePlain(faction, fme, raw); // use relations

            if (parsed == null) {
                continue; // line to be ignored, due to minimal show
            }

            if (TagUtil.hasFancy(parsed)) {
                String colorized = p.txt.parse(parsed);
                List<TextComponent> components = TagUtil.parseFancy(faction, fme, colorized);

                if (components != null) {
                    for (TextComponent textComponent : components) {
                        fme.getPlayer().spigot().sendMessage(textComponent);
                    }
                }

                continue;
            }

            if (!parsed.contains("{notFrozen}") && !parsed.contains("{notPermanent}")) {
                if (parsed.contains("{ig}")) {
                    // replaces all variables with no home TL
                    parsed = parsed.substring(0, parsed.indexOf("{ig}")) + TL.COMMAND_SHOW_NOHOME.toString();
                }

                // we don't add these entire lines to fshow, wouldn't make any sense to.
                // Ex: DTR Freeze: 0 seconds. uhm, what?
                msg(p.txt.parse(parsed));
            }
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_SHOW_COMMANDDESCRIPTION;
    }
}