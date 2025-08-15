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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AmassAbility implements VisibleAbility, Listener {

    private static final int HUNGER_COST = 4;
    private static final int MAX_ABSORPTION_AMPLIFIER = 14;
    private static final int ABSORPTION_POTION_DURATION = PotionEffect.INFINITE_DURATION;
    private static final NamespacedKey AMASS_ITEM_KEY = new NamespacedKey(FayetaleOrigins.getInstance(), "amass_item_rats");
    private final Map<UUID, Long> lastActivatedTick = new HashMap<>();
    private static final long DEBOUNCE_DELAY_TICKS = 3;

    // In-memory map to track the current level of players. This is our new "source of truth".
    private static final Map<UUID, Integer> playerAmassLevels = new ConcurrentHashMap<>();
    private static File dataFile;

    public AmassAbility() {}

    // --- YAML Data Management ---

    private void setupDataManager(JavaPlugin plugin) {
        if (dataFile == null) {
            File pluginDataFolder = plugin.getDataFolder();
            if (!pluginDataFolder.exists()) {
                pluginDataFolder.mkdirs();
            }
            dataFile = new File(pluginDataFolder, "playerdata.yml");
            if (!dataFile.exists()) {
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void savePlayerLevel(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String playerUUID = player.getUniqueId().toString();
        // Save the value from our reliable in-memory map.
        Integer level = playerAmassLevels.get(player.getUniqueId());
        if (level != null && level >= 0) {
            config.set(playerUUID + ".amass-level", level);
        } else {
            config.set(playerUUID + ".amass-level", null);
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerLevel(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        int savedLevel = config.getInt(player.getUniqueId() + ".amass-level", -1);
        if (savedLevel >= 0) {
            // Load the level from the file into our in-memory map.
            playerAmassLevels.put(player.getUniqueId(), savedLevel);
        }
    }

    private static void clearPlayerLevel(Player player) {
        // Clears from both map and file.
        playerAmassLevels.remove(player.getUniqueId());
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        config.set(player.getUniqueId().toString(), null);
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull Key getKey() {
        return Key.key(FayetaleOrigins.getInstance().getNamespace(), "amass_rats");
    }

    @Override
    public String description() {
        return "Right-click 'Clump of Rats' to consume 2 hunger bars for stacking Absorption (Max 15 hearts). Effect lasts until removed or lost. Cannot be dropped or lost on death.";
    }

    @Override
    public String title() {
        return "Amass";
    }

    private ItemStack createAmassItem() {
        ItemStack item = new ItemStack(Material.DIRT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Clump of Rats").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right-click to gather more...").color(NamedTextColor.GRAY),
                    Component.text("Costs 2 hunger bars.").color(NamedTextColor.YELLOW)
            ));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(AMASS_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isAmassItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (item.getType() != Material.DIRT) return false;
        return item.getItemMeta().getPersistentDataContainer().has(AMASS_ITEM_KEY, PersistentDataType.BYTE);
    }

    private void giveItemAndRestoreLevel(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean hasItem = false;
        for (ItemStack item : inventory.getContents()) {
            if (isAmassItem(item)) {
                hasItem = true;
                break;
            }
        }
        if (!hasItem) {
            inventory.addItem(createAmassItem());
        }

        loadPlayerLevel(player);

        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
            if (!player.isOnline()) return;
            Integer savedAmplifier = playerAmassLevels.get(player.getUniqueId());
            if (savedAmplifier != null && savedAmplifier >= 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, ABSORPTION_POTION_DURATION, savedAmplifier, true, true, true));
            }
        }, 10L);
    }

    private void removeItemAndResetState(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (isAmassItem(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        clearPlayerLevel(player);
        lastActivatedTick.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        runForAbility(event.getPlayer(), this::giveItemAndRestoreLevel);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        runForAbility(event.getPlayer(), this::savePlayerLevel);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        clearPlayerLevel(event.getPlayer());
        runForAbility(event.getPlayer(), this::giveItemAndRestoreLevel);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        if (isAmassItem(event.getItemDrop().getItemStack())) {
            runForAbility(event.getPlayer(), p -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        runForAbility(player, p -> {
            ItemStack currentItem = event.getCurrentItem();
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY && isAmassItem(currentItem)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        clearPlayerLevel(player);
        event.getDrops().removeIf(this::isAmassItem);
        lastActivatedTick.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        if (!isAmassItem(event.getItem())) return;

        event.setCancelled(true);

        runForAbility(player, p -> {
            UUID playerUUID = p.getUniqueId();
            long currentTick = Bukkit.getServer().getCurrentTick();
            if (currentTick < lastActivatedTick.getOrDefault(playerUUID, 0L) + DEBOUNCE_DELAY_TICKS) return;
            lastActivatedTick.put(playerUUID, currentTick);

            int currentAmplifier = playerAmassLevels.getOrDefault(p.getUniqueId(), -1);

            if (currentAmplifier >= MAX_ABSORPTION_AMPLIFIER) {
                p.sendMessage(Component.text("Amass is at maximum level!").color(NamedTextColor.GOLD));
                return;
            }

            if (p.getFoodLevel() < HUNGER_COST) {
                p.sendMessage(Component.text("Not enough hunger!").color(NamedTextColor.RED));
                return;
            }

            p.setFoodLevel(p.getFoodLevel() - HUNGER_COST);
            int newAmplifier = currentAmplifier + 1;

            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, ABSORPTION_POTION_DURATION, newAmplifier, true, true, true));
            playerAmassLevels.put(p.getUniqueId(), newAmplifier);
            p.sendMessage(Component.text("Amass Level: " + (newAmplifier + 1)).color(NamedTextColor.GREEN));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FOX_EAT, 1.0f, 1.2f);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            runForAbility(player, p -> {
                // Wait one tick to get the absorption value *after* damage has been applied.
                Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
                    if (p.isOnline()) {
                        // Directly get the remaining absorption health.
                        double absorptionAmount = p.getAbsorptionAmount();

                        // Calculate the effective amplifier from the health amount.
                        // Each level is 4 health points (2 hearts). Level 0 = 4 health points.
                        int effectiveAmplifier = (int) Math.floor((absorptionAmount / 4.0) - 1.0);

                        if (effectiveAmplifier < 0) {
                            // If no hearts are left, remove the player from the tracker.
                            playerAmassLevels.remove(p.getUniqueId());
                        } else {
                            // Otherwise, update the tracker with the correct, newly calculated level.
                            playerAmassLevels.put(p.getUniqueId(), effectiveAmplifier);
                        }
                    }
                }, 1L);
            });
        }
    }

    @EventHandler
    public void onOriginSwap(PlayerSwapOriginEvent event) {
        removeItemAndResetState(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(FayetaleOrigins.getInstance(), () -> {
            runForAbility(event.getPlayer(), this::giveItemAndRestoreLevel);
        }, 1L);
    }

    public static int getAndResetAmassLevel(Player player) {
        int level = playerAmassLevels.getOrDefault(player.getUniqueId(), -1);
        clearPlayerLevel(player);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        return level;
    }

    @Override
    public void initialize(JavaPlugin plugin) {
        setupDataManager(plugin);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                runForAbility(player, this::savePlayerLevel);
            }
        }, 6000L, 6000L); // 20 ticks * 60 seconds * 5 minutes
    }
}
