package cn.star.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;



public class Main extends JavaPlugin implements Listener {
    private String worldName;
    private Location spawnLocation;
    private int restorePercentage;
    private boolean enableParticles;
    private boolean enableBuild;
    private Map<Player, Integer> killCount;
    private Map<String, Integer> killStats;
    int kills = 0;

    @Override
    public void onEnable() {
        loadConfig();
        getServer().getConsoleSender().sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD + "星之角斗场" + ChatColor.WHITE + "] " + ChatColor.AQUA + "已启用 " + ChatColor.WHITE + "  by" + ChatColor.GOLD + ChatColor.BOLD + " 耀星");
        killCount = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);
        File killFile = new File(getDataFolder(), "kill.yml");
        if (killFile.exists()) {
            FileConfiguration killConfig = YamlConfiguration.loadConfiguration(killFile);
            for (String playerName : killConfig.getKeys(false)) {
                int kills = killConfig.getInt(playerName);
                killStats.put(playerName, kills);
            }
        }
        if (enableBuild) {
            getServer().getPluginManager().registerEvents(new BuildListener(), this);
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
        getServer().getConsoleSender().sendMessage(ChatColor.WHITE + "[" + ChatColor.GOLD + "星之角斗场" + ChatColor.WHITE + "] " + ChatColor.RED + "已禁用" + ChatColor.WHITE + "  by" + ChatColor.GOLD + ChatColor.BOLD + " 耀星");
        saveKillStatsToFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("duel")) {
            if (args.length >= 3 && args[0].equalsIgnoreCase("kset")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (!player.hasPermission("duel.admin")) {
                        player.sendMessage(ChatColor.RED + "你没有权限执行该命令！");
                        return true;
                    }
                }
                String playerName = args[1];
                int kills = Integer.parseInt(args[2]);
                setKillCount(playerName, kills);
                sender.sendMessage(ChatColor.GREEN + "已成功设置玩家 " + ChatColor.AQUA + playerName + ChatColor.GREEN + " 的击杀数为 " + ChatColor.RED + kills);
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("kb")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (!player.hasPermission("duel.use")) {
                            player.sendMessage(ChatColor.RED + "你没有权限执行该命令！");
                            return true;
                        }
                    }
                    showKillLeaderboard(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("reload")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (!player.hasPermission("duel.admin")) {
                            player.sendMessage(ChatColor.RED + "你没有权限执行该命令！");
                            return true;
                        }
                    }
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "配置文件已重新加载。");
                    return true;
                } else if (args[0].equalsIgnoreCase("sk")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (!player.hasPermission("duel.use")) {
                            player.sendMessage(ChatColor.RED + "你没有权限执行该命令！");
                            return true;
                        }
                    }
                    giveStandardKit(sender);
                    return true;
                } else if (args[0].equalsIgnoreCase("mk")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (!player.hasPermission("duel.use")) {
                            player.sendMessage(ChatColor.RED + "你没有权限执行该命令！");
                            return true;
                        }
                        int kills = getKillCount(player.getName());
                        player.sendMessage(ChatColor.GREEN + "您已成功击杀 " + ChatColor.RED + kills + ChatColor.GREEN + " 名玩家");
                        return true;
                    }
                }
            }
        }
        sender.sendMessage(ChatColor.WHITE + "—————————————— [" + ChatColor.GOLD + ChatColor.BOLD + "星之角斗场" + ChatColor.WHITE + "] ——————————————");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "    | " + ChatColor.AQUA + "  /duel kb");
        sender.sendMessage(ChatColor.GRAY + "        - 查看排行榜");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "    | " + ChatColor.AQUA + "  /duel sk");
        sender.sendMessage(ChatColor.GRAY + "        - 获取标准装备");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.RED + "        管理员相关指令请看插件介绍");
        sender.sendMessage("");
        return false;
    }

    private void setKillCount(String playerName, int kills) {
        killStats.put(playerName, kills);
    }

    private int getKillCount(String playerName) {
        if (killStats.containsKey(playerName)) {
            return killStats.get(playerName);
        }
        return 0;
    }

    private void giveStandardKit(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FileConfiguration config = getConfig();

            if (config.contains("standardkit.armor") && config.contains("standardkit.sword") && config.contains("standardkit.food") && config.contains("standardkit.bow")) {
                Map<String, Object> armorConfig = config.getConfigurationSection("standardkit.armor").getValues(false);
                int sword = config.getInt("standardkit.sword");
                int food = config.getInt("standardkit.food");
                int bow = config.getInt("standardkit.bow");

                player.getInventory().clear();

                ItemStack helmet = getArmorItem(armorConfig, "a");
                if (helmet != null) {
                    player.getInventory().setHelmet(helmet);
                }

                ItemStack chestplate = getArmorItem(armorConfig, "b");
                if (chestplate != null) {
                    player.getInventory().setChestplate(chestplate);
                }

                ItemStack leggings = getArmorItem(armorConfig, "c");
                if (leggings != null) {
                    player.getInventory().setLeggings(leggings);
                }

                ItemStack boots = getArmorItem(armorConfig, "d");
                if (boots != null) {
                    player.getInventory().setBoots(boots);
                }

                ItemStack swordItem = getSwordItem(sword);
                player.getInventory().setItem(0, swordItem);

                ItemStack bowItem = getBowItem(bow);
                player.getInventory().setItem(1, bowItem);

                ItemStack foodItem = getFoodItem(food);
                player.getInventory().setItem(2, foodItem);

                if (bow == 1) {
                    player.getInventory().setItem(9, new ItemStack(Material.ARROW, 32));
                }

                sender.sendMessage(ChatColor.GREEN + "已成功给予玩家标准装备！");
            } else {
                sender.sendMessage(ChatColor.RED + "配置文件中未设置标准装备！");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "该指令只能由玩家执行！");
        }
    }

    private ItemStack getArmorItem(Map<String, Object> armorConfig, String armorType) {
        ItemStack armorItem;
        int quality = (int) armorConfig.getOrDefault(armorType, 0);
                                                                                                    //by TheFlareStar
        switch (armorType.toLowerCase()) {
            case "a":
                switch (quality) {
                    case 1:
                        armorItem = new ItemStack(Material.LEATHER_HELMET);
                        break;
                    case 2:
                        armorItem = new ItemStack(Material.IRON_HELMET);
                        break;
                    case 3:
                        armorItem = new ItemStack(Material.DIAMOND_HELMET);
                        break;
                    default:
                        armorItem = null;
                        break;
                }
                break;
            case "b":
                switch (quality) {
                    case 1:
                        armorItem = new ItemStack(Material.LEATHER_CHESTPLATE);
                        break;
                    case 2:
                        armorItem = new ItemStack(Material.IRON_CHESTPLATE);
                        break;
                    case 3:
                        armorItem = new ItemStack(Material.DIAMOND_CHESTPLATE);
                        break;
                    default:
                        armorItem = null;
                        break;
                }
                break;
            case "c":
                switch (quality) {
                    case 1:
                        armorItem = new ItemStack(Material.LEATHER_LEGGINGS);
                        break;
                    case 2:
                        armorItem = new ItemStack(Material.IRON_LEGGINGS);
                        break;
                    case 3:
                        armorItem = new ItemStack(Material.DIAMOND_LEGGINGS);
                        break;
                    default:
                        armorItem = null;
                        break;
                }
                break;
            case "d":
                switch (quality) {
                    case 1:
                        armorItem = new ItemStack(Material.LEATHER_BOOTS);
                        break;
                    case 2:
                        armorItem = new ItemStack(Material.IRON_BOOTS);
                        break;
                    case 3:
                        armorItem = new ItemStack(Material.DIAMOND_BOOTS);
                        break;
                    default:
                        armorItem = null;
                        break;
                }
                break;
            default:
                armorItem = null;
                break;
        }
        return armorItem;
    }

    private ItemStack getSwordItem(int swordType) {
        ItemStack swordItem;

        switch (swordType) {
            case 1:
                swordItem = new ItemStack(Material.WOOD_SWORD);
                break;
            case 2:
                swordItem = new ItemStack(Material.STONE_SWORD);
                break;
            case 3:
                swordItem = new ItemStack(Material.IRON_SWORD);
                break;
            case 4:
                swordItem = new ItemStack(Material.DIAMOND_SWORD);
                break;
            default:
                swordItem = new ItemStack(Material.AIR);
                break;
        }
        return swordItem;
    }

    private ItemStack getFoodItem(int foodType) {
        ItemStack foodItem;

        switch (foodType) {
            case 1:
                foodItem = new ItemStack(Material.COOKIE,8);
                break;
            case 2:
                foodItem = new ItemStack(Material.APPLE,8);
                break;
            case 3:
                foodItem = new ItemStack(Material.BREAD,8);
                break;
            case 4:
                foodItem = new ItemStack(Material.COOKED_BEEF,8);
                break;
            default:
                foodItem = new ItemStack(Material.AIR);
                break;
        }
        return foodItem;
    }

    private ItemStack getBowItem(int bowType) {
        ItemStack bowItem;

        switch (bowType) {
            case 1:
                bowItem = new ItemStack(Material.BOW);
                break;
            case 2:
                bowItem = new ItemStack(Material.SHIELD);
                break;
            default:
                bowItem = new ItemStack(Material.AIR);
                break;
        }
        return bowItem;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player killer = event.getEntity().getKiller();

            if (killer != null && killer.getWorld().getName().equalsIgnoreCase(worldName)) {
                killer.addPotionEffect(new PotionEffect(PotionEffectType.HEAL, 20, restorePercentage - 1));
                killer.setFoodLevel(20);

                kills = killCount.getOrDefault(killer, 0);

                kills++;

                if (kills >= 5) {
                    Bukkit.broadcastMessage(ChatColor.WHITE + "[" + ChatColor.RED + "连杀" + ChatColor.WHITE + "] " + ChatColor.AQUA + killer.getName() + ChatColor.WHITE + " 连续击杀了五名玩家。");

                    killer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 10 * 20, 0));

                    if (enableParticles) {
                        World world = killer.getWorld();
                        Location location = killer.getLocation();
                        for (int i = 0; i < 10; i++) {
                            double angle = 2 * Math.PI * i / 10;
                            double x = Math.cos(angle);
                            double z = Math.sin(angle);
                            Location particleLocation = location.clone().add(x, 1.0, z);
                            world.playEffect(particleLocation, org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
                        }
                    }

                    kills = 0;
                }

                killCount.put(killer, kills);

                updateKillStats(killer.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player.getWorld().getName().equalsIgnoreCase(worldName)) {
            player.spigot().respawn();

            if (spawnLocation != null) {
                player.teleport(spawnLocation);
            }

            kills = 0;
        }
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        worldName = config.getString("world");

        String[] spawnCoords = config.getString("spawn").split(",");
        if (spawnCoords.length == 3) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = Double.parseDouble(spawnCoords[0]);
                double y = Double.parseDouble(spawnCoords[1]);
                double z = Double.parseDouble(spawnCoords[2]);
                spawnLocation = new Location(world, x, y, z);
            }
        }

        enableParticles = config.getBoolean("effect");
        enableBuild = config.getBoolean("build");
        restorePercentage = config.getInt("restore");

        config.addDefault("world", "world");
        config.addDefault("spawn", "124,96,24");
        config.addDefault("restore", 1);
        config.addDefault("effect", true);
        config.addDefault("build", false);


        killStats = new HashMap<>();
    }

    private void saveKillStatsToFile() {
        FileConfiguration killConfig = new YamlConfiguration();
        File killFile = new File(getDataFolder(), "kill.yml");

        for (Map.Entry<String, Integer> entry : killStats.entrySet()) {
            String playerName = entry.getKey();
            int kills = entry.getValue();
            killConfig.set(playerName, kills);
        }

        try {
            killConfig.save(killFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateKillStats(String playerName) {
        if (killStats.containsKey(playerName)) {
            int kills = killStats.get(playerName);
            killStats.put(playerName, kills + 1);
        } else {
            killStats.put(playerName, 1);
        }
    }

    private void showKillLeaderboard(CommandSender sender) {
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(killStats.entrySet());
        sortedList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        int playersPerPage = 10;
        int startIndex = 0;
        int endIndex = Math.min(startIndex + playersPerPage, sortedList.size());

        sender.sendMessage(ChatColor.DARK_GRAY + "========== " + ChatColor.WHITE + "【 击杀 】" + ChatColor.DARK_GRAY + " ==========");
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            String playerName = entry.getKey();
            int kills = entry.getValue();
            sender.sendMessage(ChatColor.WHITE + " " + (i + 1) + ". " + ChatColor.AQUA + playerName + ChatColor.WHITE + " 已击杀 " + ChatColor.RED + kills + ChatColor.WHITE + " 名玩家");
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "========== " + ChatColor.WHITE + "【 击杀 】" + ChatColor.DARK_GRAY + " ==========");
    }

    private class BuildListener implements Listener {
        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            Player player = event.getPlayer();
            World world = player.getWorld();

            if (world.getName().equalsIgnoreCase(worldName)) {
                if (enableBuild && !player.hasPermission("duel.admin")) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "你没有权限破坏该世界的建筑！");
                }
            }
        }
    }
}
