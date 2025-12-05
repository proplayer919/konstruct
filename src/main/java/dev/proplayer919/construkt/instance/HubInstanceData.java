package dev.proplayer919.construkt.instance;

import lombok.Getter;
import lombok.Setter;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

import java.util.Collection;
import java.util.HashSet;

@Getter
public class HubInstanceData extends InstanceData {
    @Setter
    private Collection<Player> players = new HashSet<>();

    public HubInstanceData(Instance instance, String id) {
        super(InstanceType.HUB, instance, id);
    }
}
