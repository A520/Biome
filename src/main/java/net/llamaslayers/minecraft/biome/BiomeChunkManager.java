package net.llamaslayers.minecraft.biome;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.minecraft.server.BiomeBase;
import net.minecraft.server.BiomeCache;
import net.minecraft.server.ChunkPosition;
import net.minecraft.server.World;
import net.minecraft.server.WorldChunkManager;

public class BiomeChunkManager extends WorldChunkManager {
    private final World world;
    public final WorldChunkManager inner;
    private Field _f;
    
    public BiomeChunkManager(World world, WorldChunkManager inner) {
        this.world = world;
        this.inner = inner;
        
//        for(Field f : WorldChunkManager.class.getDeclaredFields()) {
//            Logger.getLogger("Minecraft").info("FIELD: "+f.toGenericString());
//        }
        
        this._f = getPrivateField("f");
    }
    
    private Field getPrivateField(String string) {
        Field f;
        try {
            f = WorldChunkManager.class.getDeclaredField("f");
            f.setAccessible(true);
            return f;
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    public BiomeBase[] a(BiomeBase[] buffer, int x, int z, int rx, int rz,
            boolean flag) {
        BiomeBase[] out = inner.a(buffer, x, z, rx, rz, flag);
        int i = 0;
        for (int j = 0; j < rx; ++j) {
            for (int k = 0; k < rz; ++k) {
                BiomeBase userDefined = BiomePlugin.getBiomeBaseForLocation(world.getWorld().getName(), (x + j), (z + k));
                if (userDefined != null) {
                    buffer[i++] = userDefined;
                    continue;
                }
            }
        }
        return buffer;
    }
    
    private BiomeCache getBiomeCache() {
	    return (BiomeCache)getFieldValue(_f);
	}
    
    private Object getFieldValue(Field field) {
        try {
            return field.get(inner);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public BiomeBase getBiome(int i, int j) {
        return getBiomeCache().b(i, j);
    }
    
    public float[] getWetness(float[] buffer, int x, int z, int rx, int rz) {
        buffer=inner.getWetness(buffer, x, z, rx, rz);
        
        int i = 0;
        for (int j = 0; j < rx; ++j) {
            for (int k = 0; k < rz; ++k) {
                BiomeBase userDefined = BiomePlugin.getBiomeBaseForLocation(world.getWorld().getName(), (x + j), (z + k));
                if (userDefined != null) {
                    buffer[i++] = userDefined.z;
                    continue;
                }
            }
        }
        
        return buffer;
    }
    
    public float a(int i, int j, int k) {
        return this.a(getBiomeCache().c(i, k), j);
    }
    
    public float a(float f, int i) {
        return f;
    }
    
    public float[] a(int i, int j, int k, int l) {
        this.a = this.getTemperatures(this.a, i, j, k, l);
        return this.a;
    }
    
    public float[] getTemperatures(float[] buffer, int x, int z, int rx, int rz) {
        buffer=inner.getTemperatures(buffer, x,z,rx,rz);
        
        int i = 0;
        for (int j = 0; j < rx; ++j) {
            for (int k = 0; k < rz; ++k) {
                BiomeBase userDefined = BiomePlugin.getBiomeBaseForLocation(world.getWorld().getName(), (x + j), (z + k));
                if (userDefined != null) {
                    buffer[i++] = userDefined.y;
                    continue;
                }
            }
        }
        
        return buffer;
    }
    
    public BiomeBase[] getBiomes(BiomeBase[] abiomebase, int i, int j, int k,
            int l) {
        abiomebase=inner.getBiomes(abiomebase, i,j,k,l);
        return abiomebase;
    }
    
    public BiomeBase[] a(BiomeBase[] abiomebase, int i, int j, int k, int l) {
        return this.a(abiomebase, i, j, k, l, true);
    }
    
    public boolean a(int i, int j, int k, List list) {
        boolean o = inner.a(i,j,k,list);
        
        return o;
    }
    
    public ChunkPosition a(int i, int j, int k, List list, Random random) {
        ChunkPosition chunkposition = inner.a(i, j, k, list, random);
        
        return chunkposition;
    }
    
    public void b() {
        getBiomeCache().a();
    }
}
