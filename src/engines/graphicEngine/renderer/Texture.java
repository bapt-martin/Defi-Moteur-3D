package engines.graphicEngine.renderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Texture {
    private BufferedImage image;
    private String textureName;
    private int width;
    private int height;
    private int widthMinusOne, heightMinusOne;
    private int[] pixels;

    public static final Texture WHITE_PIXEL = createWhitePixel();

    public Texture() {
        this.image = null;
        this.textureName = null;
        this.width = 0;
        this.height = 0;
    }

    public Texture(String filepath) {
        try {
            this.image = ImageIO.read(new File(filepath));
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.widthMinusOne = width - 1;
            this.heightMinusOne = height - 1;
            this.pixels = new int[width * height];

            image.getRGB(0, 0, width, height, this.pixels, 0, width);
        } catch (IOException e) {
            System.err.println("Texture loading failed : " + filepath);
            e.printStackTrace();
        }
    }

    public int getPixelRGB(double u, double v) {
        if (pixels == null) return 0x00000000;

        if (u < 0.0) u = 0.0; else if (u > 1.0) u = 1.0;
        if (v < 0.0) v = 0.0; else if (v > 1.0) v = 1.0;

        int x = (int) (u * widthMinusOne);
        int y = (int) ((1.0 - v) * heightMinusOne);

        return pixels[x + y * width];
    }

    public void printPixelColor(double u, double v) {
        int packedRGB = getPixelRGB(u, v);

        Color color = new Color(packedRGB, true);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        System.out.println("Pixel aux UV (" + u + ", " + v + ") -> R: " + r + " | G: " + g + " | B: " + b + " | A: " + a);
    }

    private static Texture createWhitePixel() {
        Texture t = new Texture();

        t.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        t.image.setRGB(0, 0, 0xFFFFFF);

        t.width = t.image.getWidth();
        t.height = t.image.getHeight();
        t.widthMinusOne = t.width - 1;
        t.heightMinusOne = t.height - 1;
        t.pixels = new int[t.width * t.height];

        t.image.getRGB(0, 0, t.width, t.height, t.pixels, 0, t.width);

        return t;
    }

    public String getTextureName() {
        if (textureName == null){
            return "NaN";
        } else {
            return textureName;
        }
    }

    public void setTextureName(String textureName) {
        this.textureName = textureName;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}