package com.example.fayetaleorigin;

import com.example.fayetaleorigin.antling.NightVisionAbility;
import com.example.fayetaleorigin.antling.TremorSenseAbility;
import com.example.fayetaleorigin.forestghoul.ForestBlindnessAbility;
import com.example.fayetaleorigin.forestghoul.ForestSightAbility;
import com.example.fayetaleorigin.forestghoul.IncreasedHungerAbility;
import com.example.fayetaleorigin.golem.*;
import com.example.fayetaleorigin.infected.SculkHeartAbility;
import com.example.fayetaleorigin.infected.SculkVeilAbility;
import com.example.fayetaleorigin.misc.WeaknessAbility;
import com.example.fayetaleorigin.neon.DebuffSlamAbility;
import com.example.fayetaleorigin.neon.ReducedVitalityAbility;
import com.example.fayetaleorigin.neon.SummonGolemAbility;
import com.example.fayetaleorigin.neon.VillageHeroAbility;
import com.example.fayetaleorigin.ratsinatrenchcoat.AmassAbility;
import com.example.fayetaleorigin.ratsinatrenchcoat.ScatterAbility;
import com.example.fayetaleorigin.ratsinatrenchcoat.TwoHeartsAbility;
import com.example.fayetaleorigin.rowan.SculkPresenceAbility;
import com.example.fayetaleorigin.rowan.SculkResonanceAbility;
import com.example.fayetaleorigin.rowan.SculkSymbiosisAbility;
import com.example.fayetaleorigin.rowan.WardensBaneAbility;
import com.example.fayetaleorigin.swiftfox.SwiftFoxDigAbility;
import com.example.fayetaleorigin.swiftfox.SwiftFoxJumpAbility;
import com.example.fayetaleorigin.swiftfox.SwiftFoxSpeedAbility;
import com.example.fayetaleorigin.swiftfox.SwiftFoxStrengthAbility;
import com.example.fayetaleorigin.witherfox.StrengthInNumbersAbility;
import com.example.fayetaleorigin.witherfox.TheStormAbility;
import com.starshootercity.OriginsAddon;
import com.starshootercity.abilities.types.Ability;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;



public class FayetaleOrigins extends OriginsAddon {

    private static FayetaleOrigins instance;


    public FayetaleOrigins() {

    }


    @Override
    public void onRegister() {
        instance = this;
    }

    @Override
    public @NotNull String getNamespace() {
        return "fayetaleorigin";
    }

    public static FayetaleOrigins getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FayetaleOrigins addon instance not available! Was onRegister called?");
        }
        return instance;
    }

    @Override
    public @NotNull List<Ability> getRegisteredAbilities() {
        List<Ability> abilities = new ArrayList<>();
        try {
            abilities.add(new WeaknessAbility());
            abilities.add(new TremorSenseAbility());
            abilities.add(new StoneSkinAbility());
            abilities.add(new GolemStrengthAbility());
            abilities.add(new OreEaterAbility());
            abilities.add(new IncreasedHungerAbility());
            abilities.add(new PickaxeWeaknessAbility());
            abilities.add(new SlamAbility());
            abilities.add(new SculkVeilAbility());
            abilities.add(new SculkHeartAbility());
            abilities.add(new SummonGolemAbility());
            abilities.add(new VillageHeroAbility());
            abilities.add(new DebuffSlamAbility());
            abilities.add(new SculkResonanceAbility());
            abilities.add(new WardensBaneAbility());
            abilities.add(new SculkSymbiosisAbility());
            abilities.add(new SculkPresenceAbility());
            abilities.add(new StrengthInNumbersAbility());
            abilities.add(new TheStormAbility());
            abilities.add(new ForestSightAbility());
            abilities.add(new ForestBlindnessAbility());
            abilities.add(new ReducedVitalityAbility());
            abilities.add(new SwiftFoxSpeedAbility());
            abilities.add(new SwiftFoxDigAbility());
            abilities.add(new SwiftFoxStrengthAbility());
            abilities.add(new SwiftFoxJumpAbility());
            abilities.add(new AmassAbility());
            abilities.add(new ScatterAbility());
            abilities.add(new TwoHeartsAbility());
            abilities.add(new NightVisionAbility());
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Failed to instantiate abilities", t);
        }
        return abilities;
    }
}

