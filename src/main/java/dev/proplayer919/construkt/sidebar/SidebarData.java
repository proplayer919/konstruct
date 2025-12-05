package dev.proplayer919.construkt.sidebar;

import lombok.Getter;
import net.bridgesplash.sidebar.sidebar.CustomSidebar;
import net.bridgesplash.sidebar.state.State;
import net.kyori.adventure.text.Component;

import java.util.UUID;

@Getter
public class SidebarData {
    private final UUID playerId;
    private final CustomSidebar sidebar = new CustomSidebar(Component.text("Construkt"));
    private final State<String> instanceId = new State<>("hub-1");

    public SidebarData(UUID playerId) {
        this.playerId = playerId;

        this.sidebar.addState("instance_id", this.instanceId);
        this.sidebar.setLine("instance_id_line", "Instance: <state:instance_id/>");
    }

    public void setInstanceId(String instanceId) {
        this.instanceId.set(instanceId);
    }
}
