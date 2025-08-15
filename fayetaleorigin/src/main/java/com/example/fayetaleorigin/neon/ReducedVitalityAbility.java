package com.example.fayetaleorigin.neon;

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


public class ReducedVitalityAbility implements VisibleAbility, AttributeModifierAbility {

    public ReducedVitalityAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "reduced_vitality");
    }

    @Override
    public String description() {
        return "You are naturally more frail, having 4 fewer hearts.";
    }

    @Override
    public String title() {
        return "Frail Body";
    }

    @Override
    public @NotNull Attribute getAttribute() {
        
        Attribute maxHealthAttribute = MVAttribute.MAX_HEALTH.get();
        if (maxHealthAttribute == null) {

            String errorMessage = "MVAttribute.MAX_HEALTH.get() returned null! ReducedVitalityAbility cannot function. Check Origins Reborn version and server compatibility.";
            FayetaleOrigins.getInstance().getLogger().severe(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        return maxHealthAttribute;
    }

    @Override
    public double getAmount(Player player) {
        return -8.0;
    }

    @Override
    public AttributeModifier.@NotNull Operation getOperation() {
        return AttributeModifier.Operation.ADD_NUMBER;
    }

    @Override
    public void initialize(JavaPlugin plugin) {

    }
}
