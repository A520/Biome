package net.llamaslayers.minecraft.biome;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.World;
import org.bukkit.craftbukkit.util.LongHashtable;

/**
 * Loosely based off of Notch's system, but with marginally better speed and a system to serialize to disk.
 * 
 * @author Rob
 * 
 */
public class BiomeCache {
    static final byte NOT_SET = -1;
    LongHashtable<ChunkBiome> chunks = new LongHashtable<ChunkBiome>();
    private BiomePlugin plugin;
    private World world;
    
    public BiomeCache(BiomePlugin biomePlugin, World w) {
        this.plugin = biomePlugin;
        this.world = w;
    }
    
    public ChunkBiome getChunk(int x, int z) {
        if (!chunks.containsKey(x, z)) {
            ChunkBiome chunk = ChunkBiome.load(getDataFolder(), x, z);
            if (chunk == null) {
                chunk = createChunk(x, z);
            }
            chunks.put(chunk);
            return chunk;
        }
        return chunks.get(x, z);
    }
    
    private File getDataFolder() {
        return new File(plugin.getDataFolder(), "data" + File.pathSeparator + world.getName());
    }
    
    private ChunkBiome createChunk(int x, int z) {
        byte[] biome = new byte[256];
        Arrays.fill(biome, 0, 255, NOT_SET);
        return new ChunkBiome(x, z, biome);
    }

    @SuppressWarnings("unchecked")
    public void prune() {
        for(ChunkBiome chunk : (ArrayList<ChunkBiome>)chunks.values().clone()) {
            if(chunk.getSecondsOld()>10) {
                chunks.remove(chunk.key);
            }
        }
    }

    public void clearChunk(int x, int z) {
        chunks.remove(x, z);
    }

    public void save() {
        for(ChunkBiome chunk : (ArrayList<ChunkBiome>)chunks.values().clone()) {
            if(chunk.isDirty()) {
                chunk.save(getDataFolder());
            }
        }
    }
}
