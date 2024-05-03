package me.jadenp.notbounties.utils.challenges;

public class ActiveChallenge {

    private final Challenge challenge; // active challenge
    private final int variationIndex; // what variation the player is on

    public ActiveChallenge(Challenge challenge, int variationIndex) {

        this.challenge = challenge;
        this.variationIndex = variationIndex;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public int getVariationIndex() {
        return variationIndex;
    }
}
