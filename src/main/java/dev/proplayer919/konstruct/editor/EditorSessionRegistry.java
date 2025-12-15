package dev.proplayer919.konstruct.editor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditorSessionRegistry {
    private static final Map<UUID, EditorSession> sessions = new HashMap<>();

    public static void addSession(EditorSession session) {
        sessions.put(session.getHost().getUuid(), session);
    }

    public static void removeSession(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public static EditorSession getSession(UUID playerUuid) {
        return sessions.get(playerUuid);
    }

    public static Collection<UUID> getPlayers() {
        return sessions.keySet();
    }
}
