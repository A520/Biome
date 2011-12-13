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
import java.util.Map;
import java.util.Set;

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
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

public class BiomePlugin extends JavaPlugin {
    protected static BiomePlugin instance = null;
    private static Map<String, Map<LocationSet, Biome>> cache = new HashMap<String, Map<LocationSet, Biome>>();
    private PermissionHandler permissionHandler;
    
    public void onDisable() {
        for (World _world : getServer().getWorlds()) {
            CraftWorld world = (CraftWorld) _world;
            if (world.getHandle().worldProvider.b instanceof BiomeChunkManager) {
                world.getHandle().worldProvider.b = ((BiomeChunkManager) world.getHandle().worldProvider.b).inner;
            }
        }
        instance = null;
    }
    
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.WORLD_INIT, new WorldListener() {
            @Override
            public void onWorldInit(WorldInitEvent event) {
                CraftWorld world = (CraftWorld) event.getWorld();
                world.getHandle().worldProvider.b = new BiomeChunkManager(world.getHandle(), world.getHandle().worldProvider.b);
            }
        }, Event.Priority.Normal, this);
        instance = this;
        getCommand("biome").setExecutor(new BiomeCommand());
        
        for (World world : getServer().getWorlds()) {
            ((CraftWorld) world).getHandle().worldProvider.b = new BiomeChunkManager(((CraftWorld) world).getHandle(), ((CraftWorld) world).getHandle().worldProvider.b);
        }
        
        if (getConfiguration().getKeys("global_biomes") == null) {
            getConfiguration().setHeader("# Allowed biomes in this config:", "# " + Arrays.toString(Biome.values()), "");
            for (World world : getServer().getWorlds()) {
                getConfiguration().setProperty("global_biomes." + world.getName(), "auto");
            }
            getConfiguration().save();
        }
    }
    
    private void setupPermissions() {
        if (permissionHandler != null)
            return;
        
        Plugin permissionsPlugin = getServer().getPluginManager().getPlugin("Permissions");
        
        if (permissionsPlugin == null)
            return;
        
        permissionHandler = ((Permissions) permissionsPlugin).getHandler();
    }
    
    // Saving this for a rainy day
    /*
     * private static LocationSet getOwnedRegions(Player player) { Plugin plugin = instance.getServer().getPluginManager() .getPlugin("WorldGuard"); LocationSet locations = LocationSet.EMPTY; if (plugin != null) { WorldGuardPlugin wg = (WorldGuardPlugin) plugin; LocalPlayer lp = wg.wrapPlayer(player); for (ProtectedRegion region : wg .getRegionManager(player.getWorld()).getRegions().values()) { if (region.isOwner(lp)) { locations = locations.add(new LocationSet(region)); } } } return locations; }
     */
    
    public static void setBiomeForChunk(String world, int x, int z, Biome biome) {
        setBiomeForChunk(world, x, z, biome, null);
    }
    
    public static void setBiomeForChunk(final String world, final int x,
            final int z, final Biome biome, final Player player) {
        getBiomeForLocation(world, 0, 0); // Load world into cache
        instance.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
            public void run() {
                LocationSet chunk = new LocationSet(x, z);
                // Saving this for a rainy day
                /*
                 * if (player.hasPermission("biome.worldguard")) { LocationSet oldChunk = chunk; chunk = chunk.union(getOwnedRegions(player)); if (oldChunk.length != chunk.length) { player.sendMessage((oldChunk.length - chunk.length) + "/" + oldChunk.length + " (x, z) pairs were removed from your selection because of permissions."); } }
                 */
                synchronized (cache) {
                    for (LocationSet locations : cache.get(world).keySet()) {
                        boolean dirty = false;
                        for (int _x = x << 4; _x < x << 16 + 16; _x++) {
                            for (int _z = z << 4; _z < z << 16 + 16; _z++) {
                                if (locations.in(_x, _z)) {
                                    dirty = true;
                                }
                            }
                        }
                        if (dirty) {
                            Biome _biome = cache.get(world).get(locations);
                            LocationSet newLocations = locations.remove(chunk);
                            cache.get(world).remove(locations);
                            if (!newLocations.isEmpty()) {
                                cache.get(world).put(newLocations, _biome);
                            }
                        }
                    }
                    if (biome != null) {
                        cache.get(world).put(chunk, biome);
                    }
                }
                saveBiomeCache(world);
            }
        });
    }
    
    public static void clearBiomeForChunk(String world, int x, int z) {
        setBiomeForChunk(world, x, z, null);
    }
    
    public static void clearBiomeForChunk(String world, int x, int z,
            Player player) {
        setBiomeForChunk(world, x, z, null, player);
    }
    
    public static void setBiomeForRegion(String world, Region region,
            Biome biome) {
        setBiomeForRegion(world, region, biome, null);
    }
    
    public static void setBiomeForRegion(final String world,
            final Region region, final Biome biome, final Player player) {
        getBiomeForLocation(world, 0, 0); // Load world into cache
        instance.getServer().getScheduler().scheduleAsyncDelayedTask(instance, new Runnable() {
            public void run() {
                LocationSet chunk = new LocationSet(region);
                // Saving this for a rainy day
                /*
                 * if (player.hasPermission("biome.worldguard")) { LocationSet oldChunk = chunk; chunk = chunk.union(getOwnedRegions(player)); if (oldChunk.length != chunk.length) { player.sendMessage((oldChunk.length - chunk.length) + "/" + oldChunk.length + " (x, z) pairs were removed from your selection because of permissions."); } }
                 */
                Set<LocationSet> regions;
                synchronized (cache) {
                    regions = cache.get(world).keySet();
                }
                for (LocationSet locations : regions) {
                    Biome _biome;
                    synchronized (cache) {
                        _biome = cache.get(world).get(locations);
                    }
                    LocationSet newLocations = locations.remove(chunk);
                    if (!newLocations.equals(locations)) {
                        synchronized (cache) {
                            cache.get(world).remove(locations);
                        }
                        if (!newLocations.isEmpty()) {
                            synchronized (cache) {
                                cache.get(world).put(newLocations, _biome);
                            }
                        }
                    }
                }
                if (biome != null) {
                    synchronized (cache) {
                        cache.get(world).put(chunk, biome);
                    }
                }
                saveBiomeCache(world);
            }
        });
    }
    
    public static void clearBiomeForRegion(String world, Region region) {
        setBiomeForRegion(world, region, null);
    }
    
    public static void clearBiomeForRegion(String world, Region region,
            Player player) {
        setBiomeForRegion(world, region, null, player);
    }
    
    private static void saveBiomeCache(String world) {
        synchronized (cache) {
            instance.getDataFolder().mkdirs();
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(instance.getDataFolder(), world + ".dat")));
                out.writeObject(cache.get(world));
                out.flush();
                out.close();
            } catch (IOException ex) {
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Biome getBiomeForLocation(String world, int x, int z) {
        synchronized (cache) {
            if (!cache.containsKey(world)) {
                cache.put(world, new HashMap<LocationSet, Biome>());
                
                try {
                    ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(instance.getDataFolder(), world + ".dat")));
                    @SuppressWarnings("rawtypes")
                    Map worldData = (Map) in.readObject();
                    Map<LocationSet, Biome> data;
                    in.close();
                    if (worldData.isEmpty())
                        return null;
                    if (worldData.keySet().iterator().next() instanceof Integer) {
                        // Convert v0.1 data
                        data = new HashMap<LocationSet, Biome>();
                        for (Object _chunkID : worldData.keySet()) {
                            int chunkID = ((Integer) _chunkID).intValue();
                            int chunkX = chunkID >> 16;
                            int chunkZ = chunkID & 0xffff;
                            if ((chunkZ & 0x8000) != 0) {
                                chunkZ ^= 0x8000;
                                chunkZ *= -1;
                            }
                            data.put(new LocationSet(chunkX, chunkZ), (Biome) worldData.get(_chunkID));
                        }
                    } else {
                        data = worldData;
                    }
                    
                    cache.get(world).putAll(data);
                } catch (FileNotFoundException ex) {
                    return null;
                } catch (IOException ex) {
                    return null;
                } catch (ClassNotFoundException ex) {
                    return null;
                }
            }
            
            for (LocationSet locations : cache.get(world).keySet()) {
                if (locations.in(x, z))
                    return cache.get(world).get(locations);
            }
            try {
                return Biome.valueOf(instance.getConfiguration().getString("gobal_biomes." + world, "auto"));
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
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
        instance.setupPermissions();
        if (sender.isOp())
            return true;
        
        if (!(sender instanceof Player))
            return false;
        
        Player player = (Player) sender;
        
        if (instance.permissionHandler == null)
            return false;
        
        return instance.permissionHandler.has(player, "biome." + permission.name().toLowerCase().replace('_', '.'));
    }
}
