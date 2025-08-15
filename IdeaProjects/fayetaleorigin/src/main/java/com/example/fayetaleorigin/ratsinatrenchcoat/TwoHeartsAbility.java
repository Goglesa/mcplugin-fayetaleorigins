package com.example.fayetaleorigin.ratsinatrenchcoat;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.AttributeModifierAbility;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.version.MVAttribute;
import net.kyori.adventure.key.Key;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class TwoHeartsAbility implements VisibleAbility, AttributeModifierAbility {

    public TwoHeartsAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "two_hearts");
    }

    @Override
    public String description() {
        return "You are incredibly frail, having only 2 hearts.";
    }

    @Override
    public String title() {
        return "Fragile Form";
    }

    @Override
    public @NotNull Attribute getAttribute() {
        Attribute maxHealthAttribute = MVAttribute.MAX_HEALTH.get();
        if (maxHealthAttribute == null) {
            String errorMessage = "MVAttribute.MAX_HEALTH.get() returned null! TwoHeartsAbility cannot function.";
            FayetaleOrigins.getInstance().getLogger().severe(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        return maxHealthAttribute;
    }

    @Override
    public double getAmount(Player player) {
        // Default max health is 20. To get to 4 (2 hearts), subtract 16.
        return -16.0;
    }

    @Override
    public AttributeModifier.@NotNull Operation getOperation() {
        return AttributeModifier.Operation.ADD_NUMBER;
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        // No listeners needed for passive attribute modification
    }
}