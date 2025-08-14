package com.example.fayetaleorigin.swiftfox;

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

public class SwiftFoxJumpAbility implements VisibleAbility, AttributeModifierAbility {


    private static final double JUMP_STRENGTH_INCREASE = 0.18;

    public SwiftFoxJumpAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "swift_fox_jump");
    }

    @Override
    public String description() {
        return "You can jump significantly higher, approximately 2 blocks.";
    }

    @Override
    public String title() {
        return "Agile Leap";
    }

    @Override
    public @NotNull Attribute getAttribute() {
        Attribute jumpStrengthAttribute = MVAttribute.JUMP_STRENGTH.get();
        if (jumpStrengthAttribute == null) {
            FayetaleOrigins.getInstance().getLogger().severe("MVAttribute.JUMP_STRENGTH.get() returned null! SwiftFoxJumpAbility might not work.");
            throw new IllegalStateException("Could not retrieve JUMP_STRENGTH attribute via MVAttribute.");
        }
        return jumpStrengthAttribute;
    }

    @Override
    public double getAmount(Player player) {
        return JUMP_STRENGTH_INCREASE;
    }

    @Override
    public AttributeModifier.@NotNull Operation getOperation() {
        return AttributeModifier.Operation.ADD_NUMBER;
    }

    @Override
    public void initialize(JavaPlugin plugin) {
       
    }
}