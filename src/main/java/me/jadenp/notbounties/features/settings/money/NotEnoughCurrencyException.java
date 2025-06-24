package me.jadenp.notbounties.features.settings.money;

public class NotEnoughCurrencyException extends Exception {
    public NotEnoughCurrencyException(String message) {
        super(message);
    }
}
