package org.mskcc.oncotree.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Hongxin on 5/12/16.
 */
public enum Level {
    PRIMARY("1", "Primary"),
    Secondary("2", "Secondary"),
    Tertiary("3", "Tertiary"),
    Quaternary("4", "Quaternary"),
    Quinternary("5", "Quinternary");

    private Level(String level, String description) {
        this.level = level;
        this.description = description;
    }

    private final String level;
    private final String description;

    public String getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    private static final Map<String, Level> map = new HashMap<>();

    static {
        for (Level level : Level.values()) {
            map.put(level.getLevel(), level);
        }
    }

    /**
     * @param level
     * @return
     */
    public static Level getByLevel(String level) {
        return map.get(level);
    }

    public static Level getByName(String name) {
        for (Level level : Level.values()) {
            if (level.name().equals(name)) {
                return level;
            }
        }
        return null;
    }
}
