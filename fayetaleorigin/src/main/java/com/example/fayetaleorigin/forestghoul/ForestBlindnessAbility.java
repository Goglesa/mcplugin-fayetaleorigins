package com.example.fayetaleorigin.forestghoul;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome; 
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.*;

public class ForestBlindnessAbility implements VisibleAbility, Listener {

    private static final int EFFECT_DURATION = 60;
    private static final int EFFECT_AMPLIFIER = 0; 


    private static final Set<Biome> FOREST_BIOMES;
    static {
        Set<Biome> tempSet = new HashSet<>();
        tempSet.addAll(Arrays.asList(
                Biome.FOREST, Biome.FLOWER_FOREST, Biome.BIRCH_FOREST, Biome.OLD_GROWTH_BIRCH_FOREST,
                Biome.DARK_FOREST, Biome.TAIGA, Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA,
                Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE,Biome.WARPED_FOREST, Biome.CRIMSON_FOREST, Biome.PALE_GARDEN,
                Biome.CHERRY_GROVE
                // Add more biomes if needed
        ));
        FOREST_BIOMES = Collections.unmodifiableSet(tempSet); 
    }

    private static final PotionEffect BLINDNESS_EFFECT = new PotionEffect(
            PotionEffectType.BLINDNESS, EFFECT_DURATION, EFFECT_AMPLIFIER, true, false, false);

    public ForestBlindnessAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "forest_blindness");
    }

    @Override
    public String description() {
        return "Your eyes are adapted to the woods; you struggle to see outside of forest biomes.";
    }

    @Override
    public String title() {
        return "Forest Dependant";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (event.getTickNumber() % 20 != 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            runForAbility(player, this::updateBlindness);
        }
    }

    private void updateBlindness(Player player) {
        Biome currentBiome = player.getLocation().getBlock().getBiome();
        boolean inForest = FOREST_BIOMES.contains(currentBiome);

        if (!inForest) {
            player.addPotionEffect(BLINDNESS_EFFECT);
        } else {
            if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS)) {
            event.getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    // Removed onAbilityRemove method

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}