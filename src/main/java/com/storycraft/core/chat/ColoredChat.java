package com.storycraft.core.chat;

import com.storycraft.StoryMiniPlugin;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ColoredChat extends StoryMiniPlugin implements Listener {
    
    @Override
    public void onEnable() {
        getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().hasPermission("server.chat.color")) {
            e.setMessage(ChatColor.translateAlternateColorCodes('&', e.getMessage()));
        }
    }
    
}