package me.jadenp.notbounties.bounty_events;


import me.jadenp.notbounties.data.player_data.RewardHead;

public class DropRewardHead {
    private Boolean setters;
    private Boolean killers;

    public DropRewardHead(Boolean setters, Boolean killers) {
        this.setters = setters;
        this.killers = killers;
    }

    public DropRewardHead() {}

    /**
     * Check if the bounty has a custom reward head override for setters.
     * @return True if the bounty has a custom reward head override for setters.
     */
    public boolean hasSetterOverride() {
        return setters != null;
    }

    /**
     * Check if the bounty has a custom reward head override for killers.
     * @return True if the bounty has a custom reward head override for killers.
     */
    public boolean hasKillerOverride() {
        return killers != null;
    }


    public void setSetterOverride(Boolean b) {
        setters = b;
    }

    public void setKillerOverride(Boolean b) {
        killers = b;
    }

    /**
     * Check if the bounty should give setters heads when claimed.
     * If the bounty doesn't have an override, the config value will be used.
     * @return True if the bounty should give setters heads when claimed.
     */
    public boolean isDropSettersHead() {
        if (hasSetterOverride())
            return setters;
        return RewardHead.isRewardSetters();
    }

    /**
     * Check if the bounty should give the killer a head when claimed.
     * If the bounty doesn't have an override, the config value will be used.
     * @return True if the bounty should give the killer a head when claimed.
     */
    public boolean isDropKillerHead() {
        if (hasKillerOverride())
            return killers;
        return RewardHead.isRewardKiller();
    }

}