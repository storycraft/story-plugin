package com.storycraft.core.player.home;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.storycraft.StoryPlugin;
import com.storycraft.command.ICommand;
import com.storycraft.config.json.JsonConfigEntry;
import com.storycraft.config.json.JsonConfigFile;
import com.storycraft.core.MiniPlugin;
import com.storycraft.core.rank.ServerRank;
import com.storycraft.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class HomeManager extends MiniPlugin implements Listener {

    private JsonConfigFile homeConfigFile;

    @Override
    public void onLoad(StoryPlugin plugin) {
        plugin.getConfigManager().addConfigFile("home.json", homeConfigFile = new JsonConfigFile()).run();
        
        plugin.getCommandManager().addCommand(new HomeCommand());
        plugin.getCommandManager().addCommand(new SetHomeCommand());
    }

    @Override
    public void onEnable() {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.isCancelled() || !(e.getClickedBlock().getBlockData() instanceof Bed))
            return;

            if (!e.getClickedBlock().getLocation().equals(e.getPlayer().getLocation()))
                setPlayerHome(e.getPlayer(), e.getClickedBlock().getLocation());
        }

    public void setPlayerHome(Player player, Location location) {
        setRespawnLocation(player, location);
        player.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.SUCCESS, "HomeManager", "집 위치가 " + location.getWorld().getName() + " " + (Math.floor(location.getX() * 10) / 10) + " " + (Math.floor(location.getY() * 10) / 10) + " " + (Math.floor(location.getZ() * 10) / 10) + " 로 지정되었습니다."));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Location respawnLocation = getRespawnLocation(e.getPlayer());

        if (respawnLocation != null)
            e.setRespawnLocation(respawnLocation);
    }

    public Location getRespawnLocation(Player p) {
        JsonConfigEntry entry;

        if (!homeConfigFile.contains(p.getUniqueId().toString()) || (entry = homeConfigFile.getObject(p.getUniqueId().toString())) == null)
            return null;

        try {
            return new Location(getPlugin().getServer().getWorld(entry.get("world").getAsString()), entry.get("x").getAsDouble(), entry.get("y").getAsDouble(), entry.get("z").getAsDouble(), entry.get("pitch").getAsFloat(), entry.get("yaw").getAsFloat());
        } catch (Exception e) {
            return null;
        }
    }

    public void setRespawnLocation(Player p, Location location) {
        p.setBedSpawnLocation(location);

        JsonConfigEntry entry = new JsonConfigEntry();

        entry.set("world", location.getWorld().getName());
        entry.set("x", location.getX());
        entry.set("y", location.getY());
        entry.set("z", location.getZ());
        entry.set("yaw", location.getYaw());
        entry.set("pitch", location.getPitch());

        homeConfigFile.set(p.getUniqueId().toString(), entry);
    }

    public class HomeCommand implements ICommand {

        public static final int TELEPORT_COOLTIME = 60000;

        private Map<UUID, Long> timeMap; 

        public HomeCommand() {
            this.timeMap = new HashMap<>();
        }

        @Override
        public String[] getAliases() {
            return new String[]{ "home" };
        }

        @Override
        public void onCommand(CommandSender sender, String[] args) {
            Player p = (Player) sender;

            if (getPlugin().getRankManager().hasPermission(p, ServerRank.MOD) && args.length > 0){
                String targetPlayer = args[0];
                OfflinePlayer pl = getPlugin().getServer().getOfflinePlayer(targetPlayer);
                if (pl == null) {
                    p.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.FAIL, "HomeManager", "플레이어 " + targetPlayer + " 을(를) 찾을 수 없습니다"));
                }
                else {
                    Location location = getRespawnLocation(p);

                    if (location == null) {
                        p.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.FAIL, "HomeManager", "플레이어 " + targetPlayer + " 의 집이 설정되어있지 않습니다"));
                        return;
                    }

                    p.teleport(location);
                    p.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.SUCCESS, "HomeManager", targetPlayer + " 의 집으로 이동되었습니다"));
                    timeMap.put(p.getUniqueId(), System.currentTimeMillis());
                }
            }
            else {
                Location location = getRespawnLocation(p);

                if (location == null) {
                    p.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.FAIL, "HomeManager", "집 위치가 지정되어 있지 않습니다. /sethome을 사용해 지정후 사용해 주세요"));
                    return;
                }

                if (isCoolTimeDone(p) || getPlugin().getRankManager().hasPermission(p, ServerRank.MOD)) {
                    p.teleport(location);
                    p.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.SUCCESS, "HomeManager", "집으로 이동되었습니다"));
                }
                else {
                    p.sendMessage(MessageUtil.getPluginMessage(MessageUtil.MessageType.FAIL, "HomeManager", "다음 커맨드 사용까지 " + Math.ceil((TELEPORT_COOLTIME - System.currentTimeMillis() - getLastTeleport(p)) / 1000) + " 초 더 기다려야 합니다"));
                }
            }
        }

        protected void updateLastTeleport(Player p) {
            if (timeMap.containsKey(p.getUniqueId()))
                timeMap.replace(p.getUniqueId(), System.currentTimeMillis());
            else
                timeMap.put(p.getUniqueId(), System.currentTimeMillis());
        }

        protected long getLastTeleport(Player p) {
            if (!timeMap.containsKey(p.getUniqueId()))
                return 0;
            else
                return timeMap.get(p.getUniqueId());
        }

        protected boolean isCoolTimeDone(Player p) {
            return System.currentTimeMillis() - getLastTeleport(p) >= TELEPORT_COOLTIME;
        }

        @Override
        public boolean availableOnConsole() {
            return false;
        }

        @Override
        public boolean availableOnCommandBlock() {
            return false;
		}

    }

    public class SetHomeCommand implements ICommand {

        @Override
        public String[] getAliases() {
            return new String[]{ "sethome" };
        }

        @Override
        public void onCommand(CommandSender sender, String[] args) {
            Player p = (Player) sender;
            Location location = p.getLocation();

            setPlayerHome(p, location);
        }

        @Override
        public boolean availableOnConsole() {
            return false;
        }

        @Override
        public boolean availableOnCommandBlock() {
            return false;
		}

    }

}
