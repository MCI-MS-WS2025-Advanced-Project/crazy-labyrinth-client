package mci.dibse.crazylabyrinth.client.domain.model.enums;

public enum Theme {
    RUNEHAVEN_LABYRINTH("Runehaven Labyrinth", 0),
    GLOWING_CAVES("Glowing Caves", 1),
    STONE_CATACOMBS("Stone Catacombs", 2),
    EMBERFORGE_MAZE("Emberforge Maze", 3);

    private String displayName;
    private int id;

    Theme(String displayName, int id) {
        this.displayName = displayName;
        this.id = id;
    }

    /**
     * Return the display name for the theme.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Return the theme identifier.
     *
     * @return theme id
     */
    public int getId() {
        return id;
    }
}
