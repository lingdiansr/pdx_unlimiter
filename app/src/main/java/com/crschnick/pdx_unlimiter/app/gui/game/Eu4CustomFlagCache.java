package com.crschnick.pdx_unlimiter.app.gui.game;

import com.crschnick.pdx_unlimiter.app.core.CacheManager;
import com.crschnick.pdx_unlimiter.app.core.ErrorHandler;
import com.crschnick.pdx_unlimiter.app.installation.Game;
import com.crschnick.pdx_unlimiter.app.installation.GameInstallation;
import com.crschnick.pdx_unlimiter.app.util.ColorHelper;
import com.crschnick.pdx_unlimiter.core.parser.TextFormatParser;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.crschnick.pdx_unlimiter.app.util.ColorHelper.*;
import static com.crschnick.pdx_unlimiter.app.util.ColorHelper.getBlue;

public class Eu4CustomFlagCache extends CacheManager.Cache {

    private static final int EMBLEM_SIZE = 64;
    private static final int EMBLEM_WIDTH = 32;
    private static final int PATTERN_SIZE = 128;

    private static final int PATTERN_COLOR_2 = 0xFF00FF00;
    private static final int PATTERN_COLOR_1 = 0xFFFF0000;
    private static final int PATTERN_COLOR_3 = 0xFF0000FF;

    record Texture(BufferedImage img, int x, int y, int frames, int colors) {

    }

    private List<Integer> colors = new ArrayList<>();
    private List<Integer> flagColors = new ArrayList<>();
    private List<Texture> textures = new ArrayList<>();
    private BufferedImage emblems;

    public Eu4CustomFlagCache() {
        super(CacheManager.Scope.GAME_SPECIFIC);

        try {
            emblems = ImageLoader.loadAwtImage(GameInstallation.ALL.get(Game.EU4).getPath().resolve("gfx")
                    .resolve("interface").resolve("client_state_symbols_large.dds"), null);

            var colorsFile = GameInstallation.ALL.get(Game.EU4).getPath().resolve("common")
                    .resolve("custom_country_colors").resolve("00_custom_country_colors.txt");
            var content = TextFormatParser.textFileParser().parse(colorsFile);
            content.forEach((k, v) -> {
                if (k.equals("color")) {
                    var mc = v.getNodeArray();
                    int color =
                            (mc.get(0).getInteger() << 24) +
                                    (mc.get(1).getInteger() << 16) +
                                    (mc.get(2).getInteger() << 8) + 0xFF;
                    colors.add(color);
                }

                if (k.equals("flag_color")) {
                    var mc = v.getNodeArray();
                    int color =
                            (mc.get(0).getInteger() << 24) +
                                    (mc.get(1).getInteger() << 16) +
                                    (mc.get(2).getInteger() << 8) + 0xFF;
                    flagColors.add(color);
                }
            });
            content.getNodeForKey("textures").forEach((k, v) -> {
                var tex = new Texture(
                        ImageLoader.loadAwtImage(GameInstallation.ALL.get(Game.EU4).getPath().resolve(
                                Path.of(v.getNodeForKey("file").getString())), null),
                        v.getNodeForKey("size").getNodeForKey("x").getInteger(),
                        v.getNodeForKey("size").getNodeForKey("y").getInteger(),
                        v.getNodeForKey("noOfFrames").getInteger(),
                        v.getNodeForKey("color").getInteger());
                textures.add(tex);
            });
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
    }

    public Color getFlagColor(int index) {
        if (index >= flagColors.size()) {
            return new Color(0, 0, 0, 0);
        }

        return ColorHelper.awtColorFromInt(flagColors.get(index), 0xFF);
    }

    public void renderTexture(BufferedImage img, int flagIndex, List<Integer> flagColors, int emblemIndex) {
        Function<Integer, Color> patternFunction = (Integer rgb) -> {
            int id = ColorHelper.pickClosestColor(rgb, PATTERN_COLOR_1, PATTERN_COLOR_2, PATTERN_COLOR_3);
            return getFlagColor(flagColors.get(id));
        };

        int current = 0;
        for (var tex : textures) {
            if (flagIndex < current + tex.frames) {
                int rel = flagIndex - current;
                int xOff = rel % tex.x;
                int yOff = rel / tex.x;

                img.getGraphics().drawImage(
                        tex.img,
                        -xOff * img.getWidth(),
                        -yOff * img.getWidth(),
                        (int) (((double) img.getWidth() / PATTERN_SIZE) * tex.img.getWidth()),
                        (int) (((double) img.getHeight() / PATTERN_SIZE) * tex.img.getHeight()),
                        new Color(0, 0, 0, 0),
                        null);

                for (int x = 0; x < img.getWidth(); x++) {
                    for (int y = 0; y < img.getHeight(); y++) {
                        int argb = img.getRGB(x, y);
                        var replace = patternFunction.apply(argb);
                        img.setRGB(x, y, replace.getRGB());
                    }
                }

                break;
            }
            current += tex.frames;
        }

        renderEmblem(img, emblemIndex);
    }

    public void renderEmblem(BufferedImage img, int index) {
        int xOff = index % EMBLEM_WIDTH;
        int yOff = index / EMBLEM_WIDTH;

        BufferedImage i = new BufferedImage(EMBLEM_SIZE, EMBLEM_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics g = i.getGraphics();
        g.drawImage(
                emblems,
                -xOff * EMBLEM_SIZE,
                -yOff * EMBLEM_SIZE,
                emblems.getWidth(),
                emblems.getHeight(),
                new Color(0, 0, 0, 0),
                null);

        img.getGraphics().drawImage(
                i,
                (int) (0.2 * img.getWidth()),
                (int) (0.2 * img.getWidth()),
                (int) (0.6 * img.getWidth()),
                (int) (0.6 * img.getHeight()),
                new Color(0, 0, 0, 0),
                null);
    }
}
