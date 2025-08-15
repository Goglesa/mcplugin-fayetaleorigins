package com.example.fayetaleorigin.witherfox;

// Removed AttributeModifierAbility import
import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent; // Requires Paper
import net.kyori.adventure.key.Key;
// Removed Attribute and AttributeModifier imports
import org.bukkit.Bukkit; // Import Bukkit
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; // Import EventHandler
import org.bukkit.event.Listener; // Import Listener
import org.bukkit.event.player.PlayerQuitEvent; // Import PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect; // Import PotionEffect
import org.bukkit.potion.PotionEffectType; // Import PotionEffectType
import org.jetbrains.annotations.NotNull;

import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map; // Import Map
import java.util.UUID; // Import UUID


// Implement Listener instead of AttributeModifierAbility
public class StrengthInNumbersAbility implements VisibleAbility, Listener {

    // Default values
    private static final double DEFAULT_RANGE = 8.0;
    private static final int DEFAULT_REQUIRED_PLAYERS = 2; // Need 2 *other* players nearby
    private static final int EFFECT_AMPLIFIER = 0; // Strength I
    private static final int EFFECT_DURATION = 60; // Apply for 3 seconds, refresh frequently

    // Track players currently buffed by this ability
    private final Map<UUID, Boolean> hasStrengthBuff = new HashMap<>();

    private static final PotionEffect STRENGTH_BUFF = new PotionEffect(
            PotionEffectType.STRENGTH, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, true);


    public StrengthInNumbersAbility() {}

    // --- Event Handling for Passive Effect ---

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        // Check periodically (e.g., every second)
        if (event.getTickNumber() % 20 == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Check if player has the ability
                runForAbility(player, this::updateStrengthBuff);
            }
        }
    }

    // Helper method to check conditions and apply/remove effect
    private void updateStrengthBuff(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentlyBuffed = hasStrengthBuff.getOrDefault(playerId, false);

        // Check nearby players
        List<Entity> entities = player.getNearbyEntities(DEFAULT_RANGE, DEFAULT_RANGE, DEFAULT_RANGE);
        long nearbyPlayerCount = entities.stream()
                .filter(entity -> entity instanceof Player && !entity.getUniqueId().equals(playerId))
                .count();

        boolean shouldBeBuffed = (nearbyPlayerCount >= DEFAULT_REQUIRED_PLAYERS);

        if (shouldBeBuffed) {
            // Apply or refresh buff
            player.addPotionEffect(STRENGTH_BUFF);
            if (!currentlyBuffed) {
                hasStrengthBuff.put(playerId, true); // Mark as buffed
            }
        } else {
            // Remove buff if they were buffed by this ability
            if (currentlyBuffed) {
                // Check if they still have Strength before removing,
                // though removing when absent is harmless.
                if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
                    player.removePotionEffect(PotionEffectType.STRENGTH);
                }
                hasStrengthBuff.put(playerId, false); // Unmark
            }
        }
    }

    // Clean up map and effect on quit
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (hasStrengthBuff.getOrDefault(playerId, false)) {
            event.getPlayer().removePotionEffect(PotionEffectType.STRENGTH);
        }
        hasStrengthBuff.remove(playerId);
    }

    // Removed onAbilityRemove method as it cannot be overridden


    // --- VisibleAbility Implementation ---

    @Override
    public String description() {
        return "You gain Strength I when near " + DEFAULT_REQUIRED_PLAYERS + " or more other players.";
    }

    @Override
    public String title() {
        return "Strength in Numbers";
    }

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "strength_in_numbers");
    }

    // --- Other Methods ---

    @Override
    public void initialize(JavaPlugin plugin) {
        // Register listener for tick events
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
