package com.example.fayetaleorigin.infected;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class SculkHeartAbility implements VisibleAbility, Listener {

    private static final int LEVEL_COST = 5;
    private static final int DURATION_TICKS = 20 * 30;
    private static final int REGEN_DURATION_TICKS = 20 * 10;

    private static final NamespacedKey SCULK_HEART_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "sculk_heart_item");
    private final Map<UUID, Integer> lastActivatedTick = new HashMap<>();
    private static final int DEBOUNCE_DELAY_TICKS = 3;

    private static final int BASE_STRENGTH_AMP = 2;
    private static final int BASE_SPEED_AMP = 2;
    private static final int BASE_HEALTH_AMP = 3;
    private static final int BASE_REGEN_AMP = 9;

    private static final int STRENGTH_INCREMENT = 2;
    private static final int SPEED_INCREMENT = 2;
    private static final int HEALTH_INCREMENT = 3;

    public SculkHeartAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "sculk_heart");
    }

    @Override
    public String description() {
        return "You possess a Pulsating Sculk Heart. Right-click it to sacrifice " + LEVEL_COST + " levels for immense temporary power (30s) and rapid regeneration (3s). Effects stack with repeated use. Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Sculk Heart";
    }

    private ItemStack createSculkHeart() {
        ItemStack heart = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Pulsating Sculk Heart").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right-click to unleash stacking sculk power.").color(NamedTextColor.GRAY),
                    Component.text("Costs " + LEVEL_COST + " Levels.").color(NamedTextColor.RED)
            ));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(SCULK_HEART_KEY, PersistentDataType.BYTE, (byte) 1);
            heart.setItemMeta(meta);
        }
        return heart;
    }

    private boolean isSculkHeart(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(SCULK_HEART_KEY, PersistentDataType.BYTE);
    }

    private void giveHeartIfNotPresent(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSculkHeart(item)) {
                return;
            }
        }
        ItemStack heart = createSculkHeart();
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(heart);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), heart);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        runForAbility(player, this::giveHeartIfNotPresent);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
            runForAbility(player, this::giveHeartIfNotPresent);
        }, 1L);
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isSculkHeart(event.getItemDrop().getItemStack())) {
            runForAbility(event.getPlayer(), p -> {
                event.setCancelled(true);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        boolean cancel = false;

        if (isSculkHeart(clickedItem) && event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER) {
            cancel = true;
        }
        if (isSculkHeart(cursorItem) && event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER) {
            cancel = true;
        }
        if (event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY") && isSculkHeart(clickedItem)) {
            cancel = true;
        }
        if (cancel) {
            runForAbility(player, p -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        runForAbility(event.getEntity(), p -> {
            event.getDrops().removeIf(this::isSculkHeart);
        });
        lastActivatedTick.remove(event.getEntity().getUniqueId());
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isSculkHeart(item)) return;

        event.setCancelled(true);

        runForAbility(player, p -> {
            int currentTick = Bukkit.getCurrentTick();
            UUID playerUUID = p.getUniqueId();

            if (currentTick < lastActivatedTick.getOrDefault(playerUUID, 0) + DEBOUNCE_DELAY_TICKS) {
                return;
            }
            lastActivatedTick.put(playerUUID, currentTick);


            boolean hasCorrectOrigin = true; 

            if (!hasCorrectOrigin) {

                return;
            }

            if (p.getLevel() < LEVEL_COST) {
                return;
            }

            p.giveExpLevels(-LEVEL_COST);

            applyStackingEffect(p, PotionEffectType.STRENGTH, DURATION_TICKS, BASE_STRENGTH_AMP, STRENGTH_INCREMENT);
            applyStackingEffect(p, PotionEffectType.SPEED, DURATION_TICKS, BASE_SPEED_AMP, SPEED_INCREMENT);
            applyStackingEffect(p, PotionEffectType.HEALTH_BOOST, DURATION_TICKS, BASE_HEALTH_AMP, HEALTH_INCREMENT);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, REGEN_DURATION_TICKS, BASE_REGEN_AMP));

            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.2f);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SCULK_CHARGE, 1.0f, 1.0f);
        });
    }

    private void applyStackingEffect(Player player, PotionEffectType type, int duration, int baseAmplifier, int increment) {
        PotionEffect existingEffect = player.getPotionEffect(type);
        int currentAmplifier = -1;

        if (existingEffect != null) {
            currentAmplifier = existingEffect.getAmplifier();
        }

        int newAmplifier = currentAmplifier + increment;
        if (currentAmplifier == -1) {
            newAmplifier = baseAmplifier;
        }

        player.addPotionEffect(new PotionEffect(type, duration, newAmplifier, true, true, true));
    }


    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastActivatedTick.remove(event.getPlayer().getUniqueId());
    }
}