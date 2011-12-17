package net.llamaslayers.minecraft.biome;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.llamaslayers.minecraft.biome.listener.BiomeHandler;
import net.minecraft.server.BiomeBase;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

public class BiomePlugin extends JavaPlugin {
    protected static BiomePlugin instance = null;
    protected BiomeHandler worldListener = null;
    private static Map<String, BiomeCache> biomeCache = new HashMap<String, BiomeCache>();
    
    public void onDisable() {
        instance = null;
        getServer().getScheduler().cancelTasks(this);
    }
    
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        instance = this;
        for (World w : getServer().getWorlds()) {
            biomeCache.put(w.getName(), new BiomeCache(this, w));
        }
        worldListener = new BiomeHandler(this);
        pm.registerEvent(Event.Type.BIOME_GENERATE, worldListener, Event.Priority.Normal, this);
        pm.registerEvent(Event.Type.BIOME_CLIMATE, worldListener, Event.Priority.Normal, this);
        
        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                for (String w : biomeCache.keySet()) {
                    biomeCache.get(w).prune();
                }
            }
        }, 100, 100);
        getCommand("biome").setExecutor(new BiomeCommand());
        
        if (getConfiguration().getKeys("global_biomes") == null) {
            getConfiguration().setHeader("# Allowed biomes in this config:", "# " + Arrays.toString(Biome.values()), "");
            for (World world : getServer().getWorlds()) {
                getConfiguration().setProperty("global_biomes." + world.getName(), "auto");
            }
            getConfiguration().save();
        }
    }
    
    public static void setBiomeForChunk(final String world, final int x,
            final int z, final Biome biome) {
        if (!biomeCache.containsKey(world)) {
            biomeCache.put(world, new BiomeCache(instance, instance.getServer().getWorld(world)));
        }
        byte[] b = new byte[256];
        Arrays.fill(b, 0, 255, (byte) biome.ordinal());
        biomeCache.get(world).getChunk(x, z).setBiomes(b);
        save();
    }
    
    public static void clearBiomeForChunk(String world, int x, int z) {
        if (!biomeCache.containsKey(world)) {
            biomeCache.put(world, new BiomeCache(instance, instance.getServer().getWorld(world)));
        }
        biomeCache.get(world).clearChunk(x, z);
        save();
    }
    
    public static void setBiomeForRegion(final String world,
            final Region region, final Biome biome) {
        if (!biomeCache.containsKey(world)) {
            biomeCache.put(world, new BiomeCache(instance, instance.getServer().getWorld(world)));
        }
        Iterator<BlockVector> it = region.iterator();
        int lasty = -1;
        while (it.hasNext()) {
            BlockVector block = it.next();
            if (block.getBlockY() == lasty) {
                continue;
            }
            int x = block.getBlockX();
            int z = block.getBlockZ();
            if (biome != null)
                biomeCache.get(world).getChunk(x >> 4, z >> 4).setBiome(x & 15, z & 15, biome);
            else
                biomeCache.get(world).getChunk(x >> 4, z >> 4).clearBiome(x & 15, z & 15);
            lasty = block.getBlockY();
        }
        save();
    }
    
    public static void clearBiomeForRegion(String world, Region region) {
        setBiomeForRegion(world, region, null);
    }
    
    private static void save() {
        instance.getDataFolder().mkdirs();
        for (World w : instance.getServer().getWorlds()) {
            if (!biomeCache.containsKey(w.getName())) {
                biomeCache.put(w.getName(), new BiomeCache(instance, w));
            }
            biomeCache.get(w.getName()).save();
        }
    }
    
    public static Biome getBiomeForLocation(String world, int x, int z) {
        if (!biomeCache.containsKey(world)) {
            biomeCache.put(world, new BiomeCache(instance, instance.getServer().getWorld(world)));
        }
        Biome cached = biomeCache.get(world).getChunk(x >> 4, z >> 4).getBiome(x & 15, z & 15);
        if (cached == null)
            cached = biomeCache.get(world).getDefaultBiome();
        if (cached == null)
            return instance.getServer().getWorld(world).getBiome(x, z);
        return cached;
    }
    
    public static BiomeBase getBiomeBaseForLocation(String world, int x, int z) {
        Biome biome = getBiomeForLocation(world, x, z);
        if (biome == null)
            return null;
        switch (biome) {
            case DESERT:
                return BiomeBase.DESERT;
            case EXTREME_HILLS:
                return BiomeBase.EXTREME_HILLS;
            case FOREST:
                return BiomeBase.FOREST;
            case FROZEN_OCEAN:
                return BiomeBase.FROZEN_OCEAN;
            case FROZEN_RIVER:
                return BiomeBase.FROZEN_RIVER;
            case HELL:
                return BiomeBase.HELL;
            case ICE_MOUNTAINS:
                return BiomeBase.ICE_MOUNTAINS;
            case ICE_PLAINS:
                return BiomeBase.ICE_PLAINS;
            case MUSHROOM_ISLAND:
                return BiomeBase.MUSHROOM_ISLAND;
            case MUSHROOM_SHORE:
                return BiomeBase.MUSHROOM_SHORE;
            case OCEAN:
                return BiomeBase.OCEAN;
            case PLAINS:
                return BiomeBase.PLAINS;
            case RIVER:
                return BiomeBase.RIVER;
            case SKY:
                return BiomeBase.SKY;
            case SWAMPLAND:
                return BiomeBase.SWAMPLAND;
            case TAIGA:
                return BiomeBase.TAIGA;
        }
        return null;
    }
    
    protected static WorldEditPlugin getWorldEdit() {
        Plugin worldEdit = instance.getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null)
            return null;
        
        if (worldEdit instanceof WorldEditPlugin)
            return (WorldEditPlugin) worldEdit;
        else
            return null;
    }
    
    protected static boolean hasPermissionTo(CommandSender sender,
            BiomePermission permission) {
        if (sender.isOp())
            return true;
        
        if (!(sender instanceof Player))
            return false;
        
        return ((Player) sender).hasPermission("biome." + permission.name().toLowerCase().replace('_', '.'));
    }
}
