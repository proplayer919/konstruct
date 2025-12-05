package dev.proplayer919.construkt.instance;

import lombok.Getter;
import net.minestom.server.instance.Instance;

@Getter
public class InstanceData {
    private final InstanceType type;
    private final Instance instance;
    private final String id;

    public InstanceData(InstanceType type, Instance instance, String id) {
        this.type = type;
        this.instance = instance;
        this.id = id;
    }
}
