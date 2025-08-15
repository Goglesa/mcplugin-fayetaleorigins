package com.example.fayetaleorigin.neon;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.CooldownAbility;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.cooldowns.Cooldowns;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent; // <-- Import Damage Event
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer; // <-- Import PersistentDataContainer
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.key.Key;

import java.util.HashMap;
import java.util.List;

public class SummonGolemAbility implements VisibleAbility, Listener, CooldownAbility {

    private static final int GOLEM_COUNT = 2;
    private static final int COOLDOWN_TICKS = 20 * 30;
    private static final NamespacedKey SUMMON_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "golem_summon_item");
    // Key to tag summoned golems
    private static final NamespacedKey SUMMONED_GOLEM_TAG = new NamespacedKey(FayetaleOrigins.getInstance(), "summoned_golem_tag");


    public SummonGolemAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "summon_golem");
    }

    @Override
    public String description() {
        return "Right-click while holding the 'Summon Iron Golem' flower to summon " + GOLEM_COUNT + " temporary Iron Golem allies (30s Cooldown). Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Summon Guardians";
    }

    @Override
    public Cooldowns.CooldownInfo getCooldownInfo() {
        return new Cooldowns.CooldownInfo(COOLDOWN_TICKS, "wolf_howl");
    }

    private boolean isSummonItem(ItemStack item) {
        if (item == null || item.getType() != Material.POPPY || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SUMMON_ITEM_KEY, PersistentDataType.BYTE);
    }

    public ItemStack createSummonItem() {
        ItemStack item = new ItemStack(Material.POPPY);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Summon Iron Golem").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to summon guardians.").color(NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(SUMMON_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void giveItemIfNotPresent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSummonItem(item)) {
                return;
            }
        }
        ItemStack summonItem = createSummonItem();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(summonItem);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), summonItem);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        runForAbility(event.getPlayer(), this::giveItemIfNotPresent);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
            runForAbility(event.getPlayer(), this::giveItemIfNotPresent);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isSummonItem(event.getItemDrop().getItemStack())) {
            runForAbility(event.getPlayer(), p -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        runForAbility(event.getEntity(), p -> event.getDrops().removeIf(this::isSummonItem));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack heldItem = event.getItem();

        if (isSummonItem(heldItem)) {
            event.setCancelled(true);
            runForAbility(player, p -> {
                if (this.hasCooldown(p)) {
                    return;
                }
                this.setCooldown(p);
                summonGolems(p);
            });
        }
    }

    // --- Golem Spawning Logic ---
    private void summonGolems(Player player) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector sideOffset = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        world.playSound(playerLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 1.0f);

        for (int i = 0; i < GOLEM_COUNT; i++) {
            Location spawnLoc = playerLoc.clone().add(direction.clone().multiply(2.0));
            spawnLoc.add(sideOffset.clone().multiply( (i % 2 == 0 ? 1.5 : -1.5) ));
            Location safeLoc = findSafeSpawnLocation(spawnLoc);

            if (safeLoc != null) {
                IronGolem golem = (IronGolem) world.spawnEntity(safeLoc, EntityType.IRON_GOLEM);
                golem.setPlayerCreated(true);
                // Tag the golem so we know it was summoned by this ability
                golem.getPersistentDataContainer().set(SUMMONED_GOLEM_TAG, PersistentDataType.BYTE, (byte) 1);
            }
        }
    }

    private Location findSafeSpawnLocation(Location center) {
        World world = center.getWorld();
        if (world == null) return null;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location check = center.clone().add(x, 0, z);
                Location below = check.clone().subtract(0, 1, 0);
                Location above = check.clone().add(0, 1, 0);
                if (below.getBlock().getType().isSolid() &&
                        check.getBlock().getType().isAir() &&
                        above.getBlock().getType().isAir()) {
                    return check;
                }
            }
        }
        return null;
    }

    // --- Golem Aggro Logic ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) // Listen after other plugins
    public void onGolemDamaged(EntityDamageByEntityEvent event) {
        // Check if the damaged entity is an Iron Golem
        if (!(event.getEntity() instanceof IronGolem golem)) return;

        // Check if the damager is a Player
        if (!(event.getDamager() instanceof Player damager)) return;

        // Check if the golem has our custom tag
        PersistentDataContainer container = golem.getPersistentDataContainer();
        if (container.has(SUMMONED_GOLEM_TAG, PersistentDataType.BYTE)) {
            // Make the golem target the player who damaged it
            golem.setTarget(damager);
        }
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
