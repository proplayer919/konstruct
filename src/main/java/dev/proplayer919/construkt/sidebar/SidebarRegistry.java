package dev.proplayer919.construkt.sidebar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SidebarRegistry {
    private static final Map<UUID, SidebarData> sidebars = new HashMap<>();

    public static void registerSidebar(SidebarData sidebar) {
        sidebars.put(sidebar.getPlayerId(), sidebar);
    }

    public static SidebarData getSidebarByPlayerId(UUID playerId) {
        return sidebars.get(playerId);
    }

    public static void removeSidebarByPlayerId(UUID playerId) {
        sidebars.remove(playerId);
    }
}
