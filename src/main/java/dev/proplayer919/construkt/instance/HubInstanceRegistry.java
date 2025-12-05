package dev.proplayer919.construkt.instance;

import net.minestom.server.instance.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HubInstanceRegistry {
    private static final Map<String, HubInstanceData> instances = new HashMap<>();

    public static void registerInstance(HubInstanceData instance) {
        instances.put(instance.getId(), instance);
    }

    public static HubInstanceData getInstanceById(String id) {
        return instances.get(id);
    }

    public static void removeInstanceById(String id) {
        instances.remove(id);
    }

    public static Map<String, HubInstanceData> getAllInstances() {
        return instances;
    }

    public static HubInstanceData getInstanceByInstance (Instance instance) {
        for (HubInstanceData hubInstanceData : instances.values()) {
            if (hubInstanceData.getInstance().equals(instance)) {
                return hubInstanceData;
            }
        }
        return null;
    }

    public static HubInstanceData getInstanceWithLowestPlayers() {
        HubInstanceData lowest = null;
        for (HubInstanceData instance : instances.values()) {
            if (lowest == null || instance.getInstance().getPlayers().size() < lowest.getInstance().getPlayers().size()) {
                lowest = instance;
            }
        }
        return lowest;
    }

    public static HubInstanceData getInstanceWithPlayer(UUID playerId) {
        for (HubInstanceData instance : instances.values()) {
            if (instance.getInstance().getPlayers().stream().anyMatch(player -> player.getUuid().equals(playerId))) {
                return instance;
            }
        }
        return null;
    }
}
