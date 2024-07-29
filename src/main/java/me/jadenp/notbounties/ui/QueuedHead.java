package me.jadenp.notbounties.ui;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record QueuedHead(UUID uuid, ItemStack itemStack, int slot) {
}
