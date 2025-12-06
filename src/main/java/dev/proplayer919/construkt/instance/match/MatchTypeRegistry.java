package dev.proplayer919.construkt.instance.match;

import java.util.HashMap;
import java.util.Map;

public class MatchTypeRegistry {
    private static final Map<String, MatchType> matchTypes = new HashMap<>();

    public static void registerMatchType(MatchType matchType) {
        matchTypes.put(matchType.getId(), matchType);
    }

    public static MatchType getMatchTypeById(String id) {
        return matchTypes.get(id);
    }

    public static Map<String, MatchType> getAllMatchTypes() {
        return matchTypes;
    }
}
