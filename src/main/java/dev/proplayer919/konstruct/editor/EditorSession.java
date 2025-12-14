package dev.proplayer919.konstruct.editor;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;

public record EditorSession(Instance instance, Player player) {
    public void save() {
        instance.saveChunksToStorage();
    }
}
