package dev.proplayer919.construkt.permissions;

import java.util.UUID;

public class Permission {
    private final UUID uuid;
    private final String permissionNode;

    public Permission(String permissionNode) {
        this.uuid = UUID.randomUUID();
        this.permissionNode = permissionNode;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPermissionNode() {
        return permissionNode;
    }
}
