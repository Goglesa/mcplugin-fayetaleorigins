package com.example.fayetaleorigin.infected;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable; 
import org.bukkit.scheduler.BukkitTask; 
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SculkVeilAbility implements VisibleAbility, Listener {

    private final Map<UUID, Boolean> isVeiled = new HashMap<>();
    private final Map<UUID, Long> lastToggleTick = new HashMap<>();

    private final Map<UUID, BukkitTask> removalTasks = new HashMap<>();
    private static final long DEBOUNCE_TICKS = 3;
    private static final long GRACE_PERIOD_TICKS = 20; 

    private static final PotionEffect INVISIBILITY_EFFECT = new PotionEffect(
            PotionEffectType.INVISIBILITY,
            PotionEffect.INFINITE_DURATION,
            0, true, false, true
    );

    public SculkVeilAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "sculk_veil");
    }

    @Override
    public String description() {

        return "Left-click the air while standing on sculk to become invisible. Invisibility lingers for 1 second after leaving sculk.";
    }

    @Override
    public String title() {

        return "Sculk Veil";
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR) {
            Player player = event.getPlayer();
            runForAbility(player, p -> {
                toggleVeil(p);
                event.setCancelled(true);
            });
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock()) {
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();

            if (isVeiled.getOrDefault(playerId, false)) {
                runForAbility(player, p -> {
                    Block blockBelow = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
                    boolean isOnSculk = (blockBelow.getType() == Material.SCULK);

                    if (isOnSculk) {

                        cancelRemovalTask(playerId);
                    } else {

                        scheduleRemovalTask(p);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cancelRemovalTask(playerId); 
        removeVeil(event.getPlayer(), "Player quit");
        isVeiled.remove(playerId);
        lastToggleTick.remove(playerId);
    }



    private void toggleVeil(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTick = Bukkit.getCurrentTick();

        if (currentTick < lastToggleTick.getOrDefault(playerId, 0L) + DEBOUNCE_TICKS) {
            return; 
        }

        boolean currentlyVeiled = isVeiled.getOrDefault(playerId, false);

        if (currentlyVeiled) {

            cancelRemovalTask(playerId); 
            removeVeil(player, "Toggled off");
            lastToggleTick.put(playerId, currentTick);
        } else {

            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (blockBelow.getType() == Material.SCULK) {

                player.addPotionEffect(INVISIBILITY_EFFECT);
                isVeiled.put(playerId, true);
                lastToggleTick.put(playerId, currentTick);

                cancelRemovalTask(playerId);
            }
        }
    }

    private void scheduleRemovalTask(Player player) {
        UUID playerId = player.getUniqueId();

        if (!removalTasks.containsKey(playerId)) {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {

                    removalTasks.remove(playerId);

                    if (player.isOnline() && isVeiled.getOrDefault(playerId, false)) {

                        Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (blockBelow.getType() != Material.SCULK) {
                            removeVeil(player, "Grace period ended off sculk");
                        }
                    }
                }
            }.runTaskLater(FayetaleOrigins.getInstance(), GRACE_PERIOD_TICKS); 
            removalTasks.put(playerId, task);
        }
    }

    private void cancelRemovalTask(UUID playerId) {
        BukkitTask existingTask = removalTasks.remove(playerId);
        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }
    }

    private void removeVeil(Player player, String reason) {
        UUID playerId = player.getUniqueId();

        cancelRemovalTask(playerId);
        if (isVeiled.getOrDefault(playerId, false)) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            isVeiled.put(playerId, false);
        }
    }



    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}