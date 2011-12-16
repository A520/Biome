package net.llamaslayers.minecraft.biome.listener;

import net.llamaslayers.minecraft.biome.BiomePlugin;

import org.bukkit.event.world.WorldListener;

public class BiomeHandler extends WorldListener {

    private BiomePlugin plugin;

    public BiomeHandler(BiomePlugin biomePlugin) {
        this.plugin=biomePlugin;
    }
    
}
