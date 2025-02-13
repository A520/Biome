package net.llamaslayers.minecraft.biome;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.util.LongHashtable;
import org.bukkit.craftbukkit.CraftWorld;

/**
 * Loosely based off of Notch's system, but with marginally better speed and a system to serialize to disk.
 * 
 * @author Rob
 * 
 */
public class BiomeCache {
    public static final byte NOT_SET = -1;
    LongHashtable<ChunkBiome> chunks = new LongHashtable<ChunkBiome>();
    private BiomePlugin plugin;
    private World world;
    private Biome defaultBiome;
    
    public BiomeCache(BiomePlugin biomePlugin, World w) {
        this.plugin = biomePlugin;
        this.world = w;
        String biomeName = biomePlugin.getConfig().getString("global_biomes." + this.world.getName());
        if (biomeName.isEmpty() || biomeName.equalsIgnoreCase("auto")) {
            this.defaultBiome = null;
        } else {
            this.defaultBiome = Biome.valueOf(biomeName);
        }
    }
    
    public ChunkBiome getChunk(int x, int z) {
        if (!chunks.containsKey(x, z)) {
            ChunkBiome chunk = ChunkBiome.load(getDataFolder(), x, z);
            if (chunk == null) {
                chunk = createChunk(x, z);
            }
            chunks.put(x, z, chunk);
            return chunk;
        }
        return chunks.get(x, z);
    }
    
    private File getDataFolder() {
        return new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "data" + File.separator + world.getName());
    }
    
    private ChunkBiome createChunk(int x, int z) {
        byte[] biome = new byte[256];
        Arrays.fill(biome, 0, 255, NOT_SET);
        return new ChunkBiome(x, z, biome);
    }
    
    @SuppressWarnings("unchecked")
    public void prune() {
        for (ChunkBiome chunk : (ArrayList<ChunkBiome>) chunks.values().clone()) {
            if (chunk.getSecondsOld() > 10) {
                chunks.remove(chunk.getX(), chunk.getZ());
            }
        }
    }
    
    public void clearChunk(int x, int z) {
        chunks.remove(x, z);
    }
    
    @SuppressWarnings("unchecked")
    public void save() {
        getDataFolder().mkdirs();
        for (ChunkBiome chunk : (ArrayList<ChunkBiome>) chunks.values().clone()) {
            if (chunk.isDirty()) {
                chunk.save(getDataFolder());
                ((CraftWorld) world).getHandle().getWorldChunkManager().clearCacheForChunk(chunk.getX(), chunk.getZ());
            }
        }
    }
    
    public Biome getDefaultBiome() {
        return defaultBiome;
    }
}
