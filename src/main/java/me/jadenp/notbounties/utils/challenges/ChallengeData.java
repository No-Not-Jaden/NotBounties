package me.jadenp.notbounties.utils.challenges;

public class ChallengeData {
    private double progress; // current value that the player has for the challenge
    private final double goal; // number that the player's progress needs to reach to complete the challenge
    private boolean rewarded; // if the player has been rewarded or not
    private boolean notified; // if the player has been notified - this is not stored after a restart

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

    public void addProgress(double addedProgress){
        progress+= addedProgress;
    }

    public boolean isRewarded() {
        return rewarded;
    }

    public void setRewarded(boolean rewarded) {
        this.rewarded = rewarded;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public boolean isNotified() {
        return notified;
    }
}
