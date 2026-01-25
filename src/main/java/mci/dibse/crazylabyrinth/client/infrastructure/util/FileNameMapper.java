package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.PlayerColor;
import mci.dibse.crazylabyrinth.client.domain.model.enums.Theme;

public class FileNameMapper {

    private static final Map<Integer, String> TREASURE_NAMES = new HashMap<>();

    private static final Map<BonusType, String> BONI_NAMES = new HashMap<>();

    private static final Map<PlayerColor, String> PLAYER_NAMES = new HashMap<>();

    private static String UP_DOWN = "I0";
    private static String UP_RIGHT = "L0";
    private static String UP_DOWN_LEFT = "T0";
    private static Theme currentTheme = Theme.RUNEHAVEN_LABYRINTH;

    private static final String TREASURE_0 = "BackToHome";
    private static final String TREASURE_1 = "Schatz2";
    private static final String TREASURE_2 = "Iron";
    private static final String TREASURE_3 = "Gold";
    private static final String TREASURE_4 = "Apple_red";
    private static final String TREASURE_5 = "Bombe";
    private static final String TREASURE_6 = "Fackel";
    private static final String TREASURE_7 = "GoldMuenze";
    private static final String TREASURE_8 = "Kelch";
    private static final String TREASURE_9 = "Kette";
    private static final String TREASURE_10 = "Kompass";
    private static final String TREASURE_11 = "Krone";
    private static final String TREASURE_12 = "Kugel";
    private static final String TREASURE_13 = "Pilz";
    private static final String TREASURE_14 = "Ring";
    private static final String TREASURE_15 = "Schatzkarte";
    private static final String TREASURE_16 = "Schluessel";
    private static final String TREASURE_17 = "Schwert";
    private static final String TREASURE_18 = "Seil";
    private static final String TREASURE_19 = "Spitzhacke";
    private static final String TREASURE_20 = "Trank_blau";
    private static final String TREASURE_21 = "Zaubersprueche";
    private static final String TREASURE_22 = "Knochen";
    private static final String TREASURE_23 = "ZeitUhr";
    private static final String TREASURE_24 = "Schatz1";

    private static final String HOME_RED = "Haus_red";
    private static final String HOME_BLUE = "Haus_blue";
    private static final String HOME_GREEN = "Haus_gruen";
    private static final String HOME_YELLOW = "Haus_gelb";

    private static final String PLAYER_RED = "player_red";
    private static final String PLAYER_BLUE = "player_blue";
    private static final String PLAYER_GREEN = "player_green";
    private static final String PLAYER_YELLOW = "player_yellow";

    static {
        TREASURE_NAMES.put(0, TREASURE_0);
        TREASURE_NAMES.put(1, TREASURE_1);
        TREASURE_NAMES.put(2, TREASURE_2);
        TREASURE_NAMES.put(3, TREASURE_3);
        TREASURE_NAMES.put(4, TREASURE_4);
        TREASURE_NAMES.put(5, TREASURE_5);
        TREASURE_NAMES.put(6, TREASURE_6);
        TREASURE_NAMES.put(7, TREASURE_7);
        TREASURE_NAMES.put(8, TREASURE_8);
        TREASURE_NAMES.put(9, TREASURE_9);
        TREASURE_NAMES.put(10, TREASURE_10);
        TREASURE_NAMES.put(11, TREASURE_11);
        TREASURE_NAMES.put(12, TREASURE_12);
        TREASURE_NAMES.put(13, TREASURE_13);
        TREASURE_NAMES.put(14, TREASURE_14);
        TREASURE_NAMES.put(15, TREASURE_15);
        TREASURE_NAMES.put(16, TREASURE_16);
        TREASURE_NAMES.put(17, TREASURE_17);
        TREASURE_NAMES.put(18, TREASURE_18);
        TREASURE_NAMES.put(19, TREASURE_19);
        TREASURE_NAMES.put(20, TREASURE_20);
        TREASURE_NAMES.put(21, TREASURE_21);
        TREASURE_NAMES.put(22, TREASURE_22);
        TREASURE_NAMES.put(23, TREASURE_23);
        TREASURE_NAMES.put(24, TREASURE_24);

        TREASURE_NAMES.put(100, HOME_RED);
        TREASURE_NAMES.put(200, HOME_BLUE);
        TREASURE_NAMES.put(300, HOME_GREEN);
        TREASURE_NAMES.put(400, HOME_YELLOW);

        BONI_NAMES.put(BonusType.PUSH_FIXED, "Faust");
        BONI_NAMES.put(BonusType.PUSH_TWICE, "2x");
        BONI_NAMES.put(BonusType.BEAM, "Beam");
        BONI_NAMES.put(BonusType.SWAP, "Tauschen");

        PLAYER_NAMES.put(PlayerColor.RED, PLAYER_RED);
        PLAYER_NAMES.put(PlayerColor.BLUE, PLAYER_BLUE);
        PLAYER_NAMES.put(PlayerColor.GREEN, PLAYER_GREEN);
        PLAYER_NAMES.put(PlayerColor.YELLOW, PLAYER_YELLOW);
    }

    /**
     * Return the filename for a treasure id.
     *
     * @param id treasure id
     * @return treasure filename
     */
    public static String getTreasureName(int id) {
        if (!TREASURE_NAMES.containsKey(id)) {
            throw new IllegalArgumentException("Unknown treasure ID: " + id);
        }
        return TREASURE_NAMES.get(id);
    }

    /**
     * Return the home treasure id for a given player color.
     *
     * @param color player color
     * @return home treasure id
     */
    public static int getHomeTreasureIdForPlayerColor(PlayerColor color) {
        return switch (color) {
            case RED -> 100;
            case BLUE -> 200;
            case GREEN -> 300;
            case YELLOW -> 400;
        };
    }

    /**
     * Return the filename for a bonus type.
     *
     * @param type bonus type
     * @return bonus filename
     */
    public static String getBoniName(BonusType type) {
        if (!BONI_NAMES.containsKey(type)) {
            throw new IllegalArgumentException("Unknown bonus type: " + type);
        }
        return BONI_NAMES.get(type);
    }

    /**
     * Return the filename for a player pawn.
     *
     * @param color player color
     * @return player filename
     */
    public static String loadPlayerName(PlayerColor color) {
        if (!PLAYER_NAMES.containsKey(color)) {
            throw new IllegalArgumentException("Unknown player color: " + color);
        }
        return PLAYER_NAMES.get(color);
    }

    /**
     * Return the tile image name for a set of directions.
     *
     * @param direction list of directions
     * @return tile image name
     */
    public static String loadImageNameForDirection(List<Direction> direction) {
        direction.sort(Comparator.comparingInt(Direction::ordinal));
        return switch (direction.size()) {
            case 2 -> {
                if (direction.contains(Direction.UP) && direction.contains(Direction.DOWN)) {
                    yield UP_DOWN;
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)) {
                    yield UP_DOWN;
                } else if ((direction.contains(Direction.UP)
                        && direction.contains(Direction.RIGHT))) {
                    yield UP_RIGHT;
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield UP_RIGHT;
                } else if (direction.contains(Direction.UP) && direction.contains(Direction.LEFT)) {
                    yield UP_RIGHT;
                } else if (direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield UP_RIGHT;
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            case 3 -> {
                if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.LEFT)) {
                    yield UP_DOWN_LEFT;
                } else if (direction.contains(Direction.UP)
                        && direction.contains(Direction.DOWN)
                        && direction.contains(Direction.RIGHT)) {
                    yield UP_DOWN_LEFT;
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.UP)) {
                    yield UP_DOWN_LEFT;
                } else if (direction.contains(Direction.LEFT)
                        && direction.contains(Direction.RIGHT)
                        && direction.contains(Direction.DOWN)) {
                    yield UP_DOWN_LEFT;
                } else {
                    throw new IllegalStateException("Unexpected directions: " + direction);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + direction.size());
        };
    }

    /**
     * Set the current theme for tile filenames.
     *
     * @param theme theme to apply
     */
    public static void setTheme(Theme theme) {
        currentTheme = theme;
        setTileFileNamesForTheme(theme.getId());
    }

    /**
     * Return the currently selected theme.
     *
     * @return current theme
     */
    public static Theme getTheme() {
        return currentTheme;
    }

    /**
     * Update tile filename suffixes based on theme id.
     *
     * @param id theme id
     */
    private static void setTileFileNamesForTheme(int id) {
        UP_DOWN = "I" + id;
        UP_RIGHT = "L" + id;
        UP_DOWN_LEFT = "T" + id;
    }
}
