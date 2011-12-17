package net.llamaslayers.minecraft.biome;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.util.LongHash;

public class ChunkBiome {
    public long key;
    private byte[] biomes;
    private long millis;
    private boolean dirty = false;;
    
    public ChunkBiome(int x, int z, byte[] biomes) {
        this.biomes = biomes;
        this.millis = System.currentTimeMillis();
    }
    
    public int getX() {
        return LongHash.msw(key);
    }
    
    public int getZ() {
        return LongHash.lsw(key);
    }
    
    public byte[] getBiomes() {
        return biomes;
    }
    
    public void setBiomes(byte[] biomes) {
        this.biomes = biomes;
        dirty = true;
    }
    
    public Biome getBiome(int x, int z) {
        int idx = biomes[getIndex(x, z)];
        if (idx == BiomeCache.NOT_SET)
            return null;
        return Biome.values()[idx];
    }
    
    public void setBiome(int x, int z, Biome b) {
        biomes[getIndex(x, z)] = (byte) b.ordinal();
        dirty = true;
    }
    
    private int getIndex(int x, int z) {
        return x & 15 | (z & 15) << 4;
    }
    
    public long getSecondsOld() {
        return (System.currentTimeMillis() - millis) / 1000;
    }
    
    public void save(File dir) {
        String filename = getFilename();
        FileOutputStream out;
        try {
            out = new FileOutputStream(new File(dir, filename));
            out.write(biomes);
            out.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static ChunkBiome load(File dir, int x, int z) {
        String filename = String.format("%s.%s.dat", Integer.toString(x, 36), Integer.toString(z, 36));
        FileInputStream out;
        byte[] biomes = new byte[256]; // 16*16
        try {
            out = new FileInputStream(new File(dir, filename));
            out.read(biomes);
            out.close();
            ChunkBiome cb = new ChunkBiome(x, z, biomes);
            return cb;
        } catch (FileNotFoundException e) {
            // Don't bother throwing errors if there's no file.
        } catch (IOException e) {
            // Now THIS is a problem...
            e.printStackTrace();
        }
        return null;
    }
    
    private String getFilename() {
        // TODO Auto-generated method stub
        return String.format("%s.%s.dat", Integer.toString(getX(), 36), Integer.toString(getZ(), 36));
    }
    
    public void clearBiome(int x, int z) {
        biomes[getIndex(x, z)] = BiomeCache.NOT_SET;
        dirty = true;
    }
    
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return dirty;
    }
}
