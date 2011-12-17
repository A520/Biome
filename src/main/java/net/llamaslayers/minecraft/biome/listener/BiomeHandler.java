package net.llamaslayers.minecraft.biome.listener;

import net.llamaslayers.minecraft.biome.BiomePlugin;
import net.minecraft.server.BiomeBase;

import org.bukkit.block.Biome;
import org.bukkit.event.world.BiomeClimateEvent;
import org.bukkit.event.world.BiomesInChunkEvent;
import org.bukkit.event.world.WorldListener;

public class BiomeHandler extends WorldListener {
    
    private BiomePlugin plugin;
    
    public BiomeHandler(BiomePlugin biomePlugin) {
        this.plugin = biomePlugin;
    }
    
    public void onBiomeClimate(BiomeClimateEvent event) {
        for (int x = event.getX(); x < event.getX() + event.getRangeX(); x++) {
            for (int z = event.getZ(); z < event.getZ() + event.getRangeZ(); z++) {
                BiomeBase cached = BiomePlugin.getBiomeBaseForLocation(event.getWorld().getName(), x, z);
                if (cached != null) {
                    int lx = x - event.getX();
                    int lz = z - event.getZ();
                    switch (event.getClimateType()) {
                        case TEMPERATURE:
                            event.setClimate(lx, lz, cached.w);
                            break;
                        case WETNESS:
                            event.setClimate(lx, lz, cached.y);
                            break;
                    }
                }
            }
        }
    }
    
    public void onBiomeGenerate(BiomesInChunkEvent event) {
        
        for (int x = event.getX(); x < event.getX() + event.getRangeX(); x++) {
            for (int z = event.getZ(); z < event.getZ() + event.getRangeZ(); z++) {
                Biome cached = BiomePlugin.getBiomeForLocation(event.getWorld().getName(), x, z);
                if (cached != null) {
                    int lx = x - event.getX();
                    int lz = z - event.getZ();
                    event.setBiome(lx, lz, cached);
                }
            }
        }
    }
}
