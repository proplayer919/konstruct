package dev.proplayer919.minestomtest.permissions;

import java.util.Hashtable;
import java.util.Map;

public class PermissionRegistry {
    private static final Map<String, Permission> permissions = new Hashtable<>();

    public static void registerPermission(Permission permission) {
        permissions.put(permission.getPermissionNode(), permission);
    }

    public static boolean hasPermission(Permission permission) {
        return permissions.containsKey(permission.getPermissionNode());
    }

    public static Permission getPermissionByNode(String permissionNode) {
        return permissions.get(permissionNode);
    }
}
