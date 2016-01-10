package com.massivecraft.factions.event;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import org.bukkit.event.Cancellable;

/**
 * Event called when a factions dtr changes.
 */
public class DTRChangeEvent extends FactionPlayerEvent implements Cancellable {

    private boolean cancelled = false;
    private double from, to;
    private String message;

    public DTRChangeEvent(Faction f, FPlayer p, double from, double to) {
        super(f, p);
        this.from = from;
        this.to = to;
    }

    public DTRChangeEvent(Faction faction, FPlayer player, double delta) {
        this(faction, player, faction.getDTR(), faction.getDTR() + delta);
    }

    public double getFrom() {
        return from;
    }

    public double getTo() {
        return to;
    }

    public void setTo(double to) {
        this.to = to;
    }

    /**
     * Get the id of the faction.
     *
     * @return id of faction as String
     * @deprecated use getFaction().getId() instead.
     */
    @Deprecated
    public String getFactionId() {
        return getFaction().getId();
    }

    /**
     * Get the tag of the faction.
     *
     * @return tag of faction as String
     * @deprecated use getFaction().getTag() instead.
     */
    @Deprecated
    public String getFactionTag() {
        return getFaction().getTag();
    }


    /**
     * Get the dtr change message.
     *
     * @return dtr change message as String.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the dtr change message.
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean c) {
        this.cancelled = c;
    }

}
