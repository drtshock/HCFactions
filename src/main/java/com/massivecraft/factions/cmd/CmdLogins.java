package com.massivecraft.factions.cmd;

import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;

public class CmdLogins extends FCommand {

    public CmdLogins() {
        super();
        this.aliases.add("login");
        this.aliases.add("logins");
        this.aliases.add("logout");
        this.aliases.add("logouts");
        this.senderMustBePlayer = true;
        this.senderMustBeMember = true;
        this.permission = Permission.MONITOR_LOGINS.node;
    }

    @Override
    public void perform() {
        boolean monitor = fme.isMonitoringJoins();
        fme.msg(TL.COMMAND_LOGINS_TOGGLE, monitor ? TL.COMMAND_LOGINS_FALSE : TL.COMMAND_LOGINS_TRUE);
        fme.setMonitorJoins(!monitor);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_LOGINS_DESCRIPTION;
    }
}
