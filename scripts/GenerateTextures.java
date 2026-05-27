import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Standalone PNG generator for the 5 claim block tier textures. Runs once at
 * build-time so we end up with clean, distortion-free 16x16 PNGs in the
 * resource pack.
 *
 *   javac GenerateTextures.java
 *   java  GenerateTextures <output-dir>
 */
public class GenerateTextures {
    record TierSpec(int tier, Color fill, Color text, Color border) {}

    private static final TierSpec[] SPECS = {
        new TierSpec(1, new Color(0x5DADEC), Color.WHITE, Color.BLACK),
        new TierSpec(2, new Color(0x57D68D), Color.WHITE, Color.BLACK),
        new TierSpec(3, new Color(0xF5C542), Color.BLACK, Color.BLACK),
        new TierSpec(4, new Color(0xF08030), Color.WHITE, Color.BLACK),
        new TierSpec(5, new Color(0xE53030), Color.WHITE, Color.BLACK),
    };

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : ".";
        File dir = new File(out);
        if (!dir.exists()) dir.mkdirs();
        for (TierSpec s : SPECS) {
            BufferedImage img = render(s);
            File f = new File(dir, "claim_block_tier_" + s.tier + ".png");
            ImageIO.write(img, "PNG", f);
            System.out.println("wrote " + f.getAbsolutePath());
        }
    }

    /**
     * Renders one 16x16 tier texture: solid fill, 1px black border, big bold
     * digit centred. Numbers use a hard-coded 3x5 pixel grid so the result is
     * pixel-perfect at 16x16 without any anti-aliasing artefacts.
     */
    private static BufferedImage render(TierSpec spec) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // 1) solid fill
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, spec.fill.getRGB());
            }
        }
        // 2) 1px border
        int b = spec.border.getRGB();
        for (int i = 0; i < 16; i++) {
            img.setRGB(i, 0, b);
            img.setRGB(i, 15, b);
            img.setRGB(0, i, b);
            img.setRGB(15, i, b);
        }
        // 3) inner highlight (slightly lighter top edge for depth)
        Color light = lighten(spec.fill, 0.18f);
        for (int x = 1; x < 15; x++) img.setRGB(x, 1, light.getRGB());
        for (int y = 1; y < 15; y++) img.setRGB(1, y, light.getRGB());
        // 4) shadow (darker bottom-right edge)
        Color dark = darken(spec.fill, 0.25f);
        for (int x = 1; x < 15; x++) img.setRGB(x, 14, dark.getRGB());
        for (int y = 1; y < 15; y++) img.setRGB(14, y, dark.getRGB());
        // 5) digit
        drawDigit(img, spec.tier, spec.text);
        return img;
    }

    private static Color lighten(Color c, float frac) {
        int r = Math.min(255, (int) (c.getRed()   + (255 - c.getRed())   * frac));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * frac));
        int b = Math.min(255, (int) (c.getBlue()  + (255 - c.getBlue())  * frac));
        return new Color(r, g, b);
    }
    private static Color darken(Color c, float frac) {
        int r = Math.max(0, (int) (c.getRed()   * (1 - frac)));
        int g = Math.max(0, (int) (c.getGreen() * (1 - frac)));
        int b = Math.max(0, (int) (c.getBlue()  * (1 - frac)));
        return new Color(r, g, b);
    }

    /** Pixel-art digits 1-5 on a 5-wide x 7-tall grid centred at (5..9, 4..10). */
    private static void drawDigit(BufferedImage img, int digit, Color color) {
        String[] glyph = switch (digit) {
            case 1 -> new String[] {
                "..#..",
                ".##..",
                "..#..",
                "..#..",
                "..#..",
                "..#..",
                ".###."
            };
            case 2 -> new String[] {
                ".###.",
                "#...#",
                "....#",
                "...#.",
                "..#..",
                ".#...",
                "#####"
            };
            case 3 -> new String[] {
                ".###.",
                "#...#",
                "....#",
                "..##.",
                "....#",
                "#...#",
                ".###."
            };
            case 4 -> new String[] {
                "...#.",
                "..##.",
                ".#.#.",
                "#..#.",
                "#####",
                "...#.",
                "...#."
            };
            case 5 -> new String[] {
                "#####",
                "#....",
                "####.",
                "....#",
                "....#",
                "#...#",
                ".###."
            };
            default -> new String[]{"....."};
        };
        // Centre 5x7 in 16x16 -> top-left = (6, 5)
        int ox = 6, oy = 5;
        // Re-centre 5-wide so it's at columns 5..9
        ox = (16 - 5) / 2;     // = 5
        oy = (16 - 7) / 2;     // = 4
        int rgb = color.getRGB();
        for (int gy = 0; gy < glyph.length; gy++) {
            for (int gx = 0; gx < glyph[gy].length(); gx++) {
                if (glyph[gy].charAt(gx) == '#') {
                    img.setRGB(ox + gx, oy + gy, rgb);
                }
            }
        }
    }
}
