package cz.minigate.chestlock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestLock extends JavaPlugin implements Listener {

    private Map<UUID, String> chestOwners = new HashMap<>();
    private Map<Block, UUID> lockedChests = new HashMap<>();

    @Override
    public void onEnable(){
        //veci ktoré sa vykonajú pri spustení
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        getLogger().info("ChestLock plugin úspešne aktivovaný!");
    }

    @Override
    public void onDisable() {
        //veci ktoré sa vykonajú pri vypnutí
        getLogger().info("ChestLock plugin úspešne ukončený!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // pri pripojení hráča sa prejde všetky zamknuté chesty a zistí sa, či mu nejaký patrí
        Player player = event.getPlayer();
        for (Block block : lockedChests.keySet()) {
            UUID owner = lockedChests.get(block);
            if (owner.equals(player.getUniqueId())) {
                chestOwners.put(player.getUniqueId(), block.getLocation().toString());
            }
        }
    }

    @EventHandler
    //pri položení chestky sa vytvorí majitelovi UUID zamkne chestku a odošle správu
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            lockedChests.put(block, playerUUID);
            chestOwners.put(playerUUID, block.getLocation().toString());
            player.sendMessage(ChatColor.GREEN + "Zamkli ste truhlicu!");
        }
    }

    @EventHandler
    //Kontrola pri pokose na otvorenie chestky
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.CHEST && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            if (lockedChests.containsKey(block)) {
                UUID owner = lockedChests.get(block);
                //ak nieje majitelom odošle mu správu a zruší oevent otvorenia chestky
                if (!playerUUID.equals(owner)) {
                    player.sendMessage(ChatColor.RED + "Tento truhlica je zamknutá a nieste jej majitel!");
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    //pri ničení chestky skontroluje či je majitel alebo má práva "chestlock.break.all" ak nie odošle spprávu a nezničí chestku , ale ak je majitelom alebo má práva chestku zničí
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CHEST) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            if (lockedChests.containsKey(block)) {
                UUID owner = lockedChests.get(block);
                if (!playerUUID.equals(owner) && !player.hasPermission("chestlock.break.all")) {
                    player.sendMessage(ChatColor.RED + "Nemôžete zničiť túto truhlicu, nie ste jej majiteľom!");
                    event.setCancelled(true);
                    return;
                } else {
                    lockedChests.remove(block);
                    chestOwners.remove(owner);
                    player.sendMessage(ChatColor.GREEN + "Zničili ste truhlicu!");
                }
            }
        }
    }

    @Override
    //príkaz "/removechest"
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("removechest")) {
            Player player = (Player) sender;
            //skontroluje či má hráč permisie "chestlock.remove" ak nie odošle mu správu
            if (!player.hasPermission("chestlock.remove")) {
                player.sendMessage(ChatColor.RED    + "Nemáte oprávnenie použiť tento príkaz!");
                return true;
            }
            //skontroluje že či sa  hráč pozerá ne chestku ak nie odošle správu
            Block block = player.getTargetBlock(null, 10);
            if (block.getType() != Material.CHEST) {
                player.sendMessage(ChatColor.RED + "Musíte sa pozerať na chestku.");
                return true;
            }
            //skontroluje či je zamknutá
            if (!lockedChests.containsKey(block)) {
                player.sendMessage(ChatColor.RED + "Táto truhlica nie je zamknutá!");
                return true;
            }
            //skontroluje či ma permisie na ničenie všetkých chestiek
            UUID owner = lockedChests.get(block);
            if (!owner.equals(player.getUniqueId()) && !player.hasPermission("chestlock.remove.all")) {
                player.sendMessage(ChatColor.RED + "Nemôžete odstrániť túto chestku, nemáte oprávnenie!");
                return true;
            } else {
                //ak všetko splňa chestku odstráni a zobrazí správu
                lockedChests.remove(block);
                chestOwners.remove(owner);
                block.setType(Material.AIR);
                player.sendMessage(ChatColor.GREEN + "Truhlica bola úspešne odstránená!");
                return true;
            }
        }
        return true;
    }
}

