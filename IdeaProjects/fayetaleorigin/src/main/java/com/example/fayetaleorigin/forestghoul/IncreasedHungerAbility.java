package com.example.fayetaleorigin.forestghoul;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

public class IncreasedHungerAbility implements VisibleAbility, Listener {

    // How much faster hunger drains (e.g., 1.2 = 20% faster drain from actions)
    private static final double HUNGER_MULTIPLIER = 1.2;

    public IncreasedHungerAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "increased_hunger");
    }

    @Override
    public String description() {
        return "Your dense body requires more energy to sustain.";
    }

    @Override
    public String title() {
        return "Greater Appetite";
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // Only affect hunger loss, not gain
        if (event.getFoodLevel() < player.getFoodLevel()) {
            runForAbility(player, p -> {
                int foodDifference = player.getFoodLevel() - event.getFoodLevel();
                int increasedLoss = (int) Math.max(1, foodDifference * HUNGER_MULTIPLIER); // Lose at least 1 more
                // Calculate new food level based on increased loss
                int newFoodLevel = Math.max(0, player.getFoodLevel() - increasedLoss);
                event.setFoodLevel(newFoodLevel);
            });
        }
    }
}