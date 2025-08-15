package com.example.fayetaleorigin.ratsinatrenchcoat;

import com.example.fayetaleorigin.FayetaleOrigins;
import com.starshootercity.abilities.types.VisibleAbility;
import com.starshootercity.events.PlayerSwapOriginEvent;
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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

public class ScatterAbility implements VisibleAbility, Listener {

    private static final int SPEED_BASE_DURATION = 20 * 20;
    private static final NamespacedKey SCATTER_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "scatter_item_whistle");
    private final Map<UUID, Long> lastActivatedTick = new HashMap<>();
    private static final long DEBOUNCE_DELAY_TICKS = 3;


    public ScatterAbility() {}

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "scatter_rats");
    }

    @Override
    public String description() {
        return "Right-click your 'Startling Whistle' to lose all absorption and gain Speed based on absorption lost. Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Scatter!";
    }

    private ItemStack createScatterItem() {
        ItemStack item = new ItemStack(Material.COBBLESTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Startling Whistle").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right-click to disperse and flee!").color(NamedTextColor.GRAY)
            ));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(SCATTER_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isScatterItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (item.getType() != Material.COBBLESTONE) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SCATTER_ITEM_KEY, PersistentDataType.BYTE);
    }

    private void giveItemIfNotPresent(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (isScatterItem(item)) return;
        }
        ItemStack scatterItem = createScatterItem();
        HashMap<Integer, ItemStack> leftover = inventory.addItem(scatterItem);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), scatterItem);
        }
    }

    private void removeItemFromPlayer(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isScatterItem(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }
        lastActivatedTick.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        runForAbility(event.getPlayer(), this::giveItemIfNotPresent);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> runForAbility(event.getPlayer(), this::giveItemIfNotPresent), 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isScatterItem(event.getItemDrop().getItemStack())) {
            runForAbility(event.getPlayer(), p -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        runForAbility(player, p -> {
            ItemStack currentItem = event.getCurrentItem();
            // Basic safeguard against moving item to other inventories like chests.
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isScatterItem(currentItem)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isScatterItem);
        lastActivatedTick.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        if (!isScatterItem(event.getItem())) return;

        event.setCancelled(true);

        runForAbility(player, p -> {
            UUID playerUUID = p.getUniqueId();
            long currentTick = Bukkit.getServer().getCurrentTick();
            if (currentTick < lastActivatedTick.getOrDefault(playerUUID, 0L) + DEBOUNCE_DELAY_TICKS) return;
            lastActivatedTick.put(playerUUID, currentTick);

            int amassAmplifier = AmassAbility.getAndResetAmassLevel(p);

            if (amassAmplifier < 0) {
                p.sendMessage(Component.text("No absorption to scatter!").color(NamedTextColor.GRAY));
                return;
            }

            int speedAmplifier = amassAmplifier;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_BASE_DURATION, speedAmplifier, true, true, true));
            p.sendMessage(Component.text("Scattered! Gained Speed " + (speedAmplifier + 1) + ".").color(NamedTextColor.GREEN));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FOX_SCREECH, 1.0f, 1.5f);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);
        });
    }

    @EventHandler
    public void onOriginSwap(PlayerSwapOriginEvent event) {
        removeItemFromPlayer(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
            runForAbility(event.getPlayer(), this::giveItemIfNotPresent);
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastActivatedTick.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }
}