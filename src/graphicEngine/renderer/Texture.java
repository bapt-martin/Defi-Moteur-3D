package graphicEngine.renderer;

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
        } catch (IOException e) {
            System.err.println("Texture loading failed : " + filepath);
            e.printStackTrace();
        }
    }

    public int getPixelRGB(double u, double v) {
        if (image == null) return 0xFF000000;

        u = Math.max(0.0, Math.min(1.0, u));
        v = Math.max(0.0, Math.min(1.0, v));

        int x = (int) (u * (width - 1));
        int y = (int) ((1.0 - v) * (height - 1));

        return image.getRGB(x, y);
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
        t.width = 1;
        t.height = 1;
        t.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        t.image.setRGB(0, 0, 0xFFFFFF);
        return t;
    }

    public String getTextureName() {
        return textureName;
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