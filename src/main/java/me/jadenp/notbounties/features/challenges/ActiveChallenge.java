package me.jadenp.notbounties.features.challenges;

/**
 * @param challenge      active challenge
 * @param variationIndex what variation the player is on
 */
public record ActiveChallenge(Challenge challenge, int variationIndex) {

}
