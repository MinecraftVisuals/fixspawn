package net.keeplocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Remembers where a player logged out and puts them back there when they rejoin,
 * overriding any spawn plugin that teleports players to spawn on every join.
 */
public final class KeepLocationPlugin extends JavaPlugin implements Listener {

    private NamespacedKey key;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        key = new NamespacedKey(this, "logout_location");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("KeepLocation enabled.");
    }

    private boolean enabled() {
        return getConfig().getBoolean("enabled", true);
    }

    // save the location when the player leaves
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (!enabled()) return;
        Player player = e.getPlayer();
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        String data = world.getName() + ";" + loc.getX() + ";" + loc.getY() + ";"
                + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, data);
    }

    // after the spawn plugin has done its teleport, send them back
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (!enabled()) return;
        Player player = e.getPlayer();
        if (!player.hasPlayedBefore()) return; // brand new players go to spawn as normal

        String data = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (data == null) return;

        Location saved = parse(data);
        if (saved == null) return;

        long delay = getConfig().getLong("delay-ticks", 5L);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) return;
            Location now = player.getLocation();
            if (now.getWorld() == saved.getWorld() && now.distanceSquared(saved) < 1.0) return;
            player.teleport(saved);
        }, delay);
    }

    private Location parse(String data) {
        try {
            String[] p = data.split(";");
            World world = Bukkit.getWorld(p[0]);
            if (world == null) return null;
            return new Location(world,
                    Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                    Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        } catch (Exception ex) {
            return null;
        }
    }
}
