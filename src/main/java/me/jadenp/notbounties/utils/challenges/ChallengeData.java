package me.jadenp.notbounties.utils.challenges;

import java.util.LinkedList;

public class ChallengeData {
    private double progress; // progress towards the challenge
    private double goal; // number that the player's progress needs to reach to complete the challenge
    private boolean rewarded; // if the player has been rewarded or not

    public ChallengeData(double progress, double goal, boolean rewarded) {
        this.progress = progress;
        this.goal = goal;
        this.rewarded = rewarded;
    }

    public double getGoal() {
        return goal;
    }

    public double getProgress() {
        return progress;
    }

    public boolean isRewarded() {
        return rewarded;
    }
}
