package dev.proplayer919.minestomtest.permissions;

import dev.proplayer919.minestomtest.storage.SqliteDatabase;
import net.minestom.server.entity.Player;

import java.util.UUID;

public class PlayerPermissionRegistry {
    // Use a SQLite database for persistence instead of a serialized file
    private static final SqliteDatabase db = new SqliteDatabase(java.nio.file.Path.of("data", "permissions.db"));

    static {
        try {
            db.connect();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Close DB on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            db.close();
        }, "PlayerPermissionRegistry-DB-Close"));
    }

    public static void grantPermission(Player player, Permission permission) {
        grantPermission(player.getUuid(), permission);
    }

    public static void grantPermission(UUID id, Permission permission) {
        try {
            db.insertPlayerPermissionSync(id, permission.getPermissionNode());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static boolean hasPermission(Player player, Permission permission) {
        return hasPermission(player.getUuid(), permission);
    }

    public static boolean hasPermission(UUID id, Permission permission) {
        try {
            return db.playerHasPermissionSync(id, permission.getPermissionNode());
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public static void revokePermission(Player player, Permission permission) {
        revokePermission(player.getUuid(), permission);
    }

    public static void revokePermission(UUID id, Permission permission) {
        try {
            db.removePlayerPermissionSync(id, permission.getPermissionNode());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
