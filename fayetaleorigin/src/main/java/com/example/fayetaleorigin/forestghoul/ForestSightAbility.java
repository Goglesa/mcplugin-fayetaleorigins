package com.example.fayetaleorigin.forestghoul;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.OriginsReborn;
import com.starshootercity.abilities.types.VisibleAbility;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.starshootercity.events.PlayerSwapOriginEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.*;

public class ForestSightAbility implements VisibleAbility, Listener {

    private static final double CHECK_RADIUS = 64.0;
    private static final Set<Biome> FOREST_BIOMES;
    static {
        Set<Biome> tempSet = new HashSet<>();
        tempSet.addAll(Arrays.asList(
                Biome.FOREST, Biome.FLOWER_FOREST, Biome.BIRCH_FOREST, Biome.OLD_GROWTH_BIRCH_FOREST,
                Biome.DARK_FOREST, Biome.TAIGA, Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA,
                Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE,Biome.WARPED_FOREST, Biome.CRIMSON_FOREST
        ));
        FOREST_BIOMES = Collections.unmodifiableSet(tempSet);
    }

    private final Map<UUID, Set<UUID>> currentlyGlowingForPlayer = new HashMap<>();

    public ForestSightAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "forest_sight");
    }

    @Override
    public String description() {
        return "You can see nearby living entities through the trees when you are both within a forest.";
    }

    @Override
    public String title() {
        return "Forest Sight";
    }

    @EventHandler
    public void onServerTickEnd(ServerTickEndEvent event) {
        if (event.getTickNumber() % 20 != 0) return;

        for (Player sourcePlayer : Bukkit.getOnlinePlayers()) {
            runForAbility(sourcePlayer, this::updateSight);
        }
    }

    private void updateSight(Player sourcePlayer) {
        UUID sourceId = sourcePlayer.getUniqueId();
        boolean sourceInForest = FOREST_BIOMES.contains(sourcePlayer.getLocation().getBlock().getBiome());

        Set<UUID> shouldGlowNow = new HashSet<>();
        Set<UUID> wasGlowingPreviously = currentlyGlowingForPlayer.computeIfAbsent(sourceId, k -> new HashSet<>());

        if (sourceInForest) {
            Collection<Entity> nearbyEntities = sourcePlayer.getWorld().getNearbyEntities(sourcePlayer.getLocation(), CHECK_RADIUS, CHECK_RADIUS, CHECK_RADIUS,
                    entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(sourceId));

            for (Entity targetEntity : nearbyEntities) {
                if (FOREST_BIOMES.contains(targetEntity.getLocation().getBlock().getBiome())) {
                    shouldGlowNow.add(targetEntity.getUniqueId());
                }
            }
        }

        // Entities that should no longer glow for this player
        Set<UUID> toTurnOff = new HashSet<>(wasGlowingPreviously);
        toTurnOff.removeAll(shouldGlowNow);
        for (UUID targetId : toTurnOff) {
            Entity target = Bukkit.getEntity(targetId);
            if (target != null) {
                setGlowing(sourcePlayer, target, false);
            }
        }

        // Refresh glowing for all entities that should be glowing
        for (UUID targetId : shouldGlowNow) {
            Entity target = Bukkit.getEntity(targetId);
            if (target != null) {
                setGlowing(sourcePlayer, target, true);
            }
        }

        // Update the state for the next check
        currentlyGlowingForPlayer.put(sourceId, shouldGlowNow);
    }

    private void setGlowing(Player observer, Entity target, boolean glowing) {
        if (observer == null || !observer.isOnline() || target == null || !target.isValid()) return;
        OriginsReborn.getMVE().sendEntityData(observer, target, getData(target, glowing));
    }

    private static byte getData(Entity entity, boolean forceGlowing) {
        byte data = 0;

        if (forceGlowing || entity.isGlowing()) {
            data = (byte) (data | 0x40);
        }

        if (entity.getFireTicks() > 0) {
            data = (byte) (data | 0x01);
        }

        if (entity instanceof Player playerEntity) {
            if (playerEntity.isSneaking()) data = (byte) (data | 0x02);
            if (playerEntity.isSprinting()) data = (byte) (data | 0x08);
            if (playerEntity.isSwimming()) data = (byte) (data | 0x10);
            if (playerEntity.isGliding()) data |= -128;
        }

        if (entity instanceof LivingEntity livingEntity && livingEntity.isInvisible()) {
            data = (byte) (data | 0x20);
        }

        return data;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer());
        for (Set<UUID> targetSet : currentlyGlowingForPlayer.values()) {
            targetSet.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onOriginSwap(PlayerSwapOriginEvent event) {
        cleanup(event.getPlayer());
    }

    private void cleanup(Player observer) {
        Set<UUID> targets = currentlyGlowingForPlayer.remove(observer.getUniqueId());
        if (targets != null) {
            for (UUID targetId : targets) {
                Entity target = Bukkit.getEntity(targetId);
                if (target != null) {
                    setGlowing(observer, target, false);
                }
            }
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
