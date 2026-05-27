import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Standalone PNG generator for the 10 claim-stone tier textures (v3.0).
 *
 * Each texture is a 16x16 PNG with a solid fill, 1px black border, a
 * subtle highlight/shadow on opposing edges, and the tier number rendered
 * pixel-art (digits 0-9 on a 3x5 glyph grid).
 *
 *   javac GenerateTextures.java
 *   java  GenerateTextures <output-dir>
 */
public class GenerateTextures {
    record TierSpec(String id, String number, Color fill, Color text) {}

    private static final TierSpec[] SPECS = {
        new TierSpec("claimstone_10x10",   "10",  new Color(0xB0BEC5), Color.BLACK),
        new TierSpec("claimstone_25x25",   "25",  new Color(0x64B5F6), Color.WHITE),
        new TierSpec("claimstone_40x40",   "40",  new Color(0x4DD0E1), Color.WHITE),
        new TierSpec("claimstone_64x64",   "64",  new Color(0x81C784), Color.WHITE),
        new TierSpec("claimstone_80x80",   "80",  new Color(0x388E3C), Color.WHITE),
        new TierSpec("claimstone_100x100", "100", new Color(0xFFD54F), Color.BLACK),
        new TierSpec("claimstone_150x150", "150", new Color(0xFF8A65), Color.WHITE),
        new TierSpec("claimstone_250x250", "250", new Color(0xEF5350), Color.WHITE),
        new TierSpec("claimstone_300x300", "300", new Color(0xB71C1C), Color.WHITE),
        new TierSpec("claimstone_500x500", "500", new Color(0x7B1FA2), Color.WHITE),
    };

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : ".";
        File dir = new File(out);
        if (!dir.exists()) dir.mkdirs();
        for (TierSpec s : SPECS) {
            BufferedImage img = render(s);
            File f = new File(dir, s.id + ".png");
            ImageIO.write(img, "PNG", f);
            System.out.println("wrote " + f.getAbsolutePath());
        }
    }

    private static BufferedImage render(TierSpec spec) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // 1) solid fill
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, spec.fill.getRGB());
            }
        }
        // 2) 1px black border
        int b = Color.BLACK.getRGB();
        for (int i = 0; i < 16; i++) {
            img.setRGB(i, 0, b);
            img.setRGB(i, 15, b);
            img.setRGB(0, i, b);
            img.setRGB(15, i, b);
        }
        // 3) light highlight (top + left, just inside border)
        Color light = lighten(spec.fill, 0.18f);
        for (int x = 1; x < 15; x++) img.setRGB(x, 1, light.getRGB());
        for (int y = 1; y < 15; y++) img.setRGB(1, y, light.getRGB());
        // 4) shadow (bottom + right, just inside border)
        Color dark = darken(spec.fill, 0.25f);
        for (int x = 1; x < 15; x++) img.setRGB(x, 14, dark.getRGB());
        for (int y = 1; y < 15; y++) img.setRGB(14, y, dark.getRGB());
        // 5) digits
        drawDigits(img, spec.number, spec.text);
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

    /** Draws digits centred horizontally in the 14x14 inner area, 5 tall. */
    private static void drawDigits(BufferedImage img, String num, Color color) {
        // Each digit is 3px wide x 5px tall; 1px gap between digits
        int totalW = num.length() * 3 + (num.length() - 1);
        int ox = (16 - totalW) / 2;
        int oy = (16 - 5) / 2 + 1;
        int rgb = color.getRGB();
        int cursor = ox;
        for (int i = 0; i < num.length(); i++) {
            char c = num.charAt(i);
            String[] g = glyph(c - '0');
            for (int gy = 0; gy < g.length; gy++) {
                for (int gx = 0; gx < g[gy].length(); gx++) {
                    if (g[gy].charAt(gx) == '#') {
                        int x = cursor + gx;
                        int y = oy + gy;
                        if (x >= 1 && x <= 14 && y >= 1 && y <= 14) {
                            img.setRGB(x, y, rgb);
                        }
                    }
                }
            }
            cursor += 4; // 3 wide + 1 gap
        }
    }

    /** 3x5 pixel-art digits 0-9. */
    private static String[] glyph(int d) {
        return switch (d) {
            case 0 -> new String[]{"###", "#.#", "#.#", "#.#", "###"};
            case 1 -> new String[]{".#.", "##.", ".#.", ".#.", "###"};
            case 2 -> new String[]{"###", "..#", "###", "#..", "###"};
            case 3 -> new String[]{"###", "..#", "###", "..#", "###"};
            case 4 -> new String[]{"#.#", "#.#", "###", "..#", "..#"};
            case 5 -> new String[]{"###", "#..", "###", "..#", "###"};
            case 6 -> new String[]{"###", "#..", "###", "#.#", "###"};
            case 7 -> new String[]{"###", "..#", "..#", "..#", "..#"};
            case 8 -> new String[]{"###", "#.#", "###", "#.#", "###"};
            case 9 -> new String[]{"###", "#.#", "###", "..#", "###"};
            default -> new String[]{"###", "###", "###", "###", "###"};
        };
    }
}
