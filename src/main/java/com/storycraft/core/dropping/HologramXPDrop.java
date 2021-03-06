package com.storycraft.core.dropping;

import com.storycraft.StoryMiniPlugin;
import com.storycraft.server.hologram.Hologram;
import com.storycraft.server.hologram.HologramManager;
import com.storycraft.server.hologram.SimpleHologram;
import com.storycraft.util.ConnectionUtil;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityExperienceOrb;
import net.minecraft.server.v1_15_R1.Packet;
import net.minecraft.server.v1_15_R1.PacketPlayOutCollect;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

public class HologramXPDrop extends StoryMiniPlugin implements Listener {

    private static final int SHOW_TIME = 30;

    @Override
    public void onEnable(){
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent e){
        Player killer = e.getEntity().getKiller();

        if (killer != null && e.getDroppedExp() > 0){
            int xp = e.getDroppedExp();
            e.setDroppedExp(0);

            giveExp(killer, xp, e.getEntity().getLocation());
        }
    }

    public void giveExp(Player player, int amount, Location expLocation){
        Hologram xpHologram = new XPHologram(expLocation, player, ChatColor.YELLOW + "" + amount + " XP");
        HologramManager hologramManager = getPlugin().getDecorator().getHologramManager();
        hologramManager.addHologram(xpHologram);

        new EntityExperienceOrb(((CraftWorld)expLocation.getWorld()).getHandle(), 0, 0, 0, amount).pickup(((CraftPlayer)player).getHandle());
    }

    private class XPHologram extends SimpleHologram {

        private Player collector;

        public XPHologram(Location location, Player collector, String... texts) {
            super(location, texts);

            this.collector = collector;
        }

        public Player getCollector() {
            return collector;
        }

        @Override
        public void onAdd(){
            getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {
                HologramManager hologramManager = getPlugin().getDecorator().getHologramManager();
                List<Entity> list = hologramManager.getHologramEntityList(XPHologram.this);

                Packet[] packetList = new Packet[list.size()];

                for (int i = 0; i < packetList.length; i++){
                    packetList[i] = new PacketPlayOutCollect(list.get(i).getId(), collector.getEntityId(), 1);
                }

                ConnectionUtil.sendPacketNearby(getLocation(), packetList);

                getPlugin().getServer().getScheduler().runTaskLaterAsynchronously(getPlugin(), () -> {
                    hologramManager.removeHologram(this);
                }, 30);
            }, SHOW_TIME);
        }
    }
}
