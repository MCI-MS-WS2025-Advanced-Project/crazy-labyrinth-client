package mci.dibse.crazylabyrinth.client.infrastructure.util;

import java.util.HashMap;
import java.util.Map;

public class TreasureMapper {


    private static final Map<Integer, String> TREASURE_NAMES = new HashMap<>();



    private final static String TREASURE_0 = "BackToHome";
    private final static String TREASURE_1 = "Schatz2";
    private final static String TREASURE_2 = "Iron";
    private final static String TREASURE_3 = "Gold";
    private final static String TREASURE_4 = "Apple_red";
    private final static String TREASURE_5 = "Bombe";
    private final static String TREASURE_6 = "Fackel";
    private final static String TREASURE_7 = "GoldMünze";
    private final static String TREASURE_8 = "Kelch";
    private final static String TREASURE_9 = "Kette";
    private final static String TREASURE_10 = "Kompass";
    private final static String TREASURE_11 = "Krone";
    private final static String TREASURE_12 = "Kugel";
    private final static String TREASURE_13 = "Pilz";
    private final static String TREASURE_14 = "Ring";
    private final static String TREASURE_15 = "Schatzkarte";
    private final static String TREASURE_16 = "Schlüssel";
    private final static String TREASURE_17 = "Schwert";
    private final static String TREASURE_18 = "Seil";
    private final static String TREASURE_19 = "Spitzhacke";
    private final static String TREASURE_20 = "Trank_blau";
    private final static String TREASURE_21 = "Zaubersprüche";
    private final static String TREASURE_22 = "Knochen";
    private final static String TREASURE_23 = "ZeitUhr";
    private final static String TREASURE_24 = "Schatz1";


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
    }

    public static String getTreasureName(int id) {
        if (!TREASURE_NAMES.containsKey(id)) {
            throw new IllegalArgumentException("Unknown treasure ID: " + id);
        }
        return TREASURE_NAMES.get(id);
    }


}
