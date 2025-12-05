package dev.proplayer919.construkt.instance;

import net.minestom.server.instance.Instance;

import java.util.HashMap;
import java.util.Map;

public class GameInstanceRegistry {
    private static final Map<String, GameInstanceData> instances = new HashMap<>();

    public static void registerInstance(GameInstanceData instance) {
        instances.put(instance.getId(), instance);
    }

    public static GameInstanceData getInstanceById(String id) {
        return instances.get(id);
    }

    public static GameInstanceData getInstanceByInstance (Instance instance) {
        for (GameInstanceData gameInstanceData : instances.values()) {
            if (gameInstanceData.getInstance().equals(instance)) {
                return gameInstanceData;
            }
        }
        return null;
    }

    public static void removeInstanceById(String id) {
        instances.remove(id);
    }

    public static Map<String, GameInstanceData> getAllInstances() {
        return instances;
    }
}
