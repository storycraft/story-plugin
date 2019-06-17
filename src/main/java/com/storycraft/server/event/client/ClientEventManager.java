package com.storycraft.server.event.client;

import com.storycraft.server.ServerExtension;
import com.storycraft.server.packet.AsyncPacketOutEvent;
import com.storycraft.util.reflect.Reflect;
import net.minecraft.server.v1_14_R1.PacketPlayOutMapChunk;
import net.minecraft.server.v1_14_R1.PacketPlayOutUnloadChunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class ClientEventManager extends ServerExtension implements Listener {

    private Reflect.WrappedField<Integer, PacketPlayOutMapChunk> chunkLoadPacketX;
    private Reflect.WrappedField<Integer, PacketPlayOutMapChunk> chunkLoadPacketZ;
    private Reflect.WrappedField<Boolean, PacketPlayOutMapChunk> isFullChunkField;

    private Reflect.WrappedField<Integer, PacketPlayOutUnloadChunk> chunkUnloadPacketX;
    private Reflect.WrappedField<Integer, PacketPlayOutUnloadChunk> chunkUnloadPacketZ;

    @Override
    public void onEnable(){
        this.chunkLoadPacketX = Reflect.getField(PacketPlayOutMapChunk.class, "a");
        this.chunkLoadPacketZ = Reflect.getField(PacketPlayOutMapChunk.class, "b");
        this.isFullChunkField = Reflect.getField(PacketPlayOutMapChunk.class, "g");

        this.chunkUnloadPacketX = Reflect.getField(PacketPlayOutUnloadChunk.class, "a");
        this.chunkUnloadPacketZ = Reflect.getField(PacketPlayOutUnloadChunk.class, "b");

        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @EventHandler
    public void invokePlayerLoadChunkEvent(AsyncPacketOutEvent e){
        if (e.getPacket() instanceof PacketPlayOutMapChunk){
            try {
            PacketPlayOutMapChunk packet = (PacketPlayOutMapChunk) e.getPacket();
            Player p = e.getTarget();

            World w = p.getWorld();

            int locX = chunkLoadPacketX.get(packet);
            int locZ = chunkLoadPacketZ.get(packet);

            boolean isFullChunk = isFullChunkField.get(packet);

            AsyncPlayerLoadChunkEvent event = new AsyncPlayerLoadChunkEvent(p, w, locX, locZ, isFullChunk);
            getPlugin().getServer().getPluginManager().callEvent(event);

            if (event.isCancelled())
                e.setCancelled(true);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler
    public void invokePlayerUnloadChunkEvent(AsyncPacketOutEvent e){
        if (e.getPacket() instanceof PacketPlayOutUnloadChunk){
            PacketPlayOutUnloadChunk packet = (PacketPlayOutUnloadChunk) e.getPacket();
            Player p = e.getTarget();

            World w = p.getWorld();

            int locX = chunkUnloadPacketX.get(packet);
            int locZ = chunkUnloadPacketZ.get(packet);

            AsyncPlayerUnloadChunkEvent event = new AsyncPlayerUnloadChunkEvent(p, w, locX, locZ);
            getPlugin().getServer().getPluginManager().callEvent(event);

            if (event.isCancelled())
                e.setCancelled(true);
        }
    }
}
