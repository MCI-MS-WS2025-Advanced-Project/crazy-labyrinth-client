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
        ColorAdjust adjust = new ColorAdjust();
        adjust.setHue(0.10);
        adjust.setSaturation(0.6);
        adjust.setBrightness(0.05);
        adjust.setContrast(0.15);

        Lighting lighting = new Lighting(new Light.Distant(60, 45, Color.web("#FFD54F")));
        lighting.setSurfaceScale(2.0);
        lighting.setDiffuseConstant(1.1);
        lighting.setSpecularConstant(0.7);
        lighting.setSpecularExponent(32);
        lighting.setContentInput(adjust);

        DropShadow glowShadow = new DropShadow();
        glowShadow.setRadius(10);
        glowShadow.setSpread(0.2);
        glowShadow.setColor(Color.rgb(255, 180, 40, 0.6));
        glowShadow.setOffsetX(0);
        glowShadow.setOffsetY(0);
        glowShadow.setInput(lighting);
    }

    /**
     * Apply outline, glow, and tint effects to a player image view.
     *
     * @param playerImage image view to style
     * @param color player color for accent tint
     */
    public static void applyPlayerStylingToImageView(
            javafx.scene.image.ImageView playerImage, PlayerColor color) {

        Color accentColor = PlayerPawnUtils.mapPlayerColorToFxColor(color);

        // Slightly boost saturation/contrast and lift brightness
        ColorAdjust adjust = new ColorAdjust();
        adjust.setSaturation(0.25);
        adjust.setContrast(0.20);
        adjust.setBrightness(0.06);

        // Brighten highlights to pop details
        Bloom bloom = new Bloom();
        bloom.setThreshold(0.32); // lower threshold -> more highlights
        bloom.setInput(adjust);
        // Dark outline to improve separation from background (keep radius small for pixel art)
        DropShadow outline = new DropShadow();
        outline.setRadius(1.6);
        outline.setSpread(1.0);
        outline.setColor(Color.rgb(0, 0, 0, 0.95));
        outline.setOffsetX(0);
        outline.setOffsetY(0);
        outline.setInput(bloom);

        // Colored halo to draw attention (uses provided accent color)
        /*
                DropShadow halo = new DropShadow();
                halo.setRadius(6.0);
                halo.setSpread(0.28);
                halo.setColor(accentColor.deriveColor(0, 1, 1, 0.65));
                halo.setOffsetX(0);
                halo.setOffsetY(0);
                halo.setInput(outline);
        */
        // Optional inner shadow for depth
        InnerShadow inner = new InnerShadow();
        inner.setRadius(2.0);
        inner.setChoke(0.35);
        inner.setColor(Color.rgb(0, 0, 0, 0.6));
        // inner.setInput(halo);
        // Optional subtle tint using a blend (kept very low to preserve original colors)
        // If the ImageView is resized, you may want to update this ColorInput's width/height
        // accordingly.
        double w =
                playerImage.getFitWidth() > 0
                        ? playerImage.getFitWidth()
                        : playerImage.getImage().getWidth();
        double h =
                playerImage.getFitHeight() > 0
                        ? playerImage.getFitHeight()
                        : playerImage.getImage().getHeight();
        ColorInput tintLayer = new ColorInput(0, 0, w, h, accentColor.deriveColor(0, 1, 1, 0.12));

        /*
        Blend tintBlend = new Blend();
        tintBlend.setMode(BlendMode.MULTIPLY);
        tintBlend.setTopInput(tintLayer);
        tintBlend.setBottomInput(inner);
         */

        Blend tintOverSprite = new Blend(BlendMode.SRC_ATOP);
        tintOverSprite.setTopInput(tintLayer); // the color
        tintOverSprite.setBottomInput(inner); // the sprite + halo/outline chain
        playerImage.setEffect(tintOverSprite);

        // Keep tint layer in sync if the ImageView changes size (optional, useful when using
        // fitWidth/fitHeight)
        playerImage
                .fitWidthProperty()
                .addListener((obs, oldV, newV) -> tintLayer.setWidth(newV.doubleValue()));
        playerImage
                .fitHeightProperty()
                .addListener((obs, oldV, newV) -> tintLayer.setHeight(newV.doubleValue()));
    }
}
