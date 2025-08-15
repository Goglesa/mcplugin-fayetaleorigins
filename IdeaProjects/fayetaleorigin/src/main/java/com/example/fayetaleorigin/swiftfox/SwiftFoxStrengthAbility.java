package com.example.fayetaleorigin.swiftfox;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; // Requires Paper
import net.kyori.adventure.key.Key;
// Removed Attribute and AttributeModifier imports
import org.bukkit.Bukkit; // Import Bukkit
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; // Import EventHandler
import org.bukkit.event.Listener;   // Import Listener
import org.bukkit.event.player.PlayerQuitEvent; // Import PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect; // Import PotionEffect
import org.bukkit.potion.PotionEffectType; // Import PotionEffectType
import org.jetbrains.annotations.NotNull;

// Implement Listener instead of AttributeModifierAbility
public class SwiftFoxStrengthAbility implements VisibleAbility, Listener {

    private static final int EFFECT_AMPLIFIER = 0; // Strength I
    private static final int EFFECT_DURATION = 60; // Apply for 3 seconds, refresh frequently

    private static final PotionEffect STRENGTH_EFFECT = new PotionEffect(
            PotionEffectType.STRENGTH, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);

    public SwiftFoxStrengthAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "swift_fox_strength");
    }

    @Override
    public String description() {
        return "You are naturally stronger, dealing more damage with your attacks.";
    }

    @Override
    public String title() {
        return "Sharp Bite";
    }

    // Removed getAttribute(), getAmount(), getOperation() methods

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        // Refresh roughly every 2 seconds
        if (event.getTickNumber() % 40 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // runForAbility checks if player has this ability active
                runForAbility(player, p -> p.addPotionEffect(STRENGTH_EFFECT));
            }
        }
    }

    // Clean up effect on quit
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Check if the player has the specific strength effect from this ability
        if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
            PotionEffect existingEffect = player.getPotionEffect(PotionEffectType.STRENGTH);
            if (existingEffect != null && existingEffect.getAmplifier() == EFFECT_AMPLIFIER && existingEffect.getDuration() <= EFFECT_DURATION) {
                player.removePotionEffect(PotionEffectType.STRENGTH);
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        // Register listener for tick events
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}