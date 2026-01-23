package mci.dibse.crazylabyrinth.client.infrastructure.util;

import javafx.scene.effect.*;
import javafx.scene.paint.Color;
import labyrinth.contracts.models.PlayerColor;

public class ImageViewStyler {

    /**
     * Apply glow/lighting effects to a treasure image view.
     *
     * @param treasureImage image view to style
     */
    public static void applyTreasureStylingToImageView(javafx.scene.image.ImageView treasureImage) {

        // 1. Adjust Base Image: Boost Saturation & Brightness slightly
        ColorAdjust baseAdjust = new ColorAdjust();
        baseAdjust.setSaturation(0.4);
        baseAdjust.setBrightness(0.1);
        baseAdjust.setContrast(0.15);

        // 2. Strong Glow (Radiance)
        Glow glow = new Glow();
        glow.setLevel(0.65); // High level for "flashy" look
        glow.setInput(baseAdjust);


        DropShadow outline = new DropShadow();
        outline.setRadius(2.0);
        outline.setSpread(0.2);
        outline.setColor(Color.BLACK);
        outline.setInput(glow);

        // 4. Colored Aura/Halo (The main "flashy" part)
        DropShadow aura = new DropShadow();

        aura.setInput(outline);
        treasureImage.setEffect(aura);





    }

    /**
     * Apply outline, glow, and tint effects to a player image view.
     *
     * @param playerImage image view to style
     * @param color player color for accent tint
     */
    public static void applyPlayerStylingToImageView(javafx.scene.image.ImageView playerImage, PlayerColor color) {

        Color accentColor = PlayerPawnUtils.mapPlayerColorToFxColor(color);

        // 1. Adjust Base Image: Boost Saturation & Brightness slightly
        ColorAdjust baseAdjust = new ColorAdjust();
        baseAdjust.setSaturation(0.4);
        baseAdjust.setBrightness(0.1);
        baseAdjust.setContrast(0.15);

        // 2. Strong Glow (Radiance)
        Glow glow = new Glow();
        glow.setLevel(0.65); // High level for "flashy" look
        glow.setInput(baseAdjust);


        DropShadow outline = new DropShadow();
        outline.setRadius(2.0);
        outline.setSpread(0.2);
        outline.setColor(Color.BLACK);
        outline.setInput(glow);

        // 4. Colored Aura/Halo (The main "flashy" part)
        DropShadow aura = new DropShadow();

        aura.setInput(outline);


        playerImage.setEffect(aura);


    }
}
