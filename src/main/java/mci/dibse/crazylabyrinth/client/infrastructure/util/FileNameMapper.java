package mci.dibse.crazylabyrinth.client.infrastructure.util;

import javafx.scene.image.Image;
import labyrinth.contracts.models.BonusType;
import labyrinth.contracts.models.Direction;
import labyrinth.contracts.models.PlayerColor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileNameMapper {

    private static final Map<Integer, String> TREASURE_NAMES = new HashMap<>();

    private static final Map<BonusType, String> BONI_NAMES = new HashMap<>();

    private static final Map<PlayerColor, String> PLAYER_NAMES = new HashMap<>();


    private static final String UP_DOWN = "I";
    private static final String UP_RIGHT = "L";
    private static final String UP_DOWN_LEFT = "T";


    private final static String TREASURE_0 = "BackToHome";
    private final static String TREASURE_1 = "Schatz2";
    private final static String TREASURE_2 = "Iron";
    private final static String TREASURE_3 = "Gold";
    private final static String TREASURE_4 = "Apple_red";
    private final static String TREASURE_5 = "Bombe";
    private final static String TREASURE_6 = "Fackel";
    private final static String TREASURE_7 = "GoldMuenze";
    private final static String TREASURE_8 = "Kelch";
    private final static String TREASURE_9 = "Kette";
    private final static String TREASURE_10 = "Kompass";
    private final static String TREASURE_11 = "Krone";
    private final static String TREASURE_12 = "Kugel";
    private final static String TREASURE_13 = "Pilz";
    private final static String TREASURE_14 = "Ring";
    private final static String TREASURE_15 = "Schatzkarte";
    private final static String TREASURE_16 = "Schluessel";
    private final static String TREASURE_17 = "Schwert";
    private final static String TREASURE_18 = "Seil";
    private final static String TREASURE_19 = "Spitzhacke";
    private final static String TREASURE_20 = "Trank_blau";
    private final static String TREASURE_21 = "Zaubersprueche";
    private final static String TREASURE_22 = "Knochen";
    private final static String TREASURE_23 = "ZeitUhr";
    private final static String TREASURE_24 = "Schatz1";



    private final static String HOME_RED = "Haus_red";
    private final static String HOME_BLUE = "Haus_blue";
    private final static String HOME_GREEN = "Haus_gruen";
    private final static String HOME_YELLOW = "Haus_gelb";


    private final static String PLAYER_RED = "player_red";
    private final static String PLAYER_BLUE = "player_blue";
    private final static String PLAYER_GREEN = "player_green";
    private final static String PLAYER_YELLOW = "player_yellow";

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

    public static String getTreasureName(int id) {
        if (!TREASURE_NAMES.containsKey(id)) {
            throw new IllegalArgumentException("Unknown treasure ID: " + id);
        }
        return TREASURE_NAMES.get(id);
    }


    public static int getHomeTreasureIdForPlayerColor(PlayerColor color) {
        return switch (color) {
            case RED -> 100;
            case BLUE -> 200;
            case GREEN -> 300;
            case YELLOW -> 400;
        };
    }


    public static String getBoniName(BonusType type) {
        if (!BONI_NAMES.containsKey(type)) {
            throw new IllegalArgumentException("Unknown bonus type: " + type);
        }
        return BONI_NAMES.get(type);
    }

    public static String loadPlayerName(PlayerColor color) {
        if (!PLAYER_NAMES.containsKey(color)) {
            throw new IllegalArgumentException("Unknown player color: " + color);
        }
        return PLAYER_NAMES.get(color);
    }



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

}


