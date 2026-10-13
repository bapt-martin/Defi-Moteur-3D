package engines.graphicEngine.math.tools;

public class ColorUtils {
    public static final int WHITE  = 0xFFFFFFFF; // 255, 255, 255
    public static final int BLACK  = 0xFF000000; // 0, 0, 0
    public static final int RED    = 0xFFFF0000; // 255, 0, 0
    public static final int GREEN  = 0xFF00FF00; // 0, 255, 0
    public static final int BLUE   = 0xFF0000FF; // 0, 0, 255

    public static final int YELLOW  = 0xFFFFFF00; // 255, 255, 0
    public static final int CYAN    = 0xFF00FFFF; // 0, 255, 255
    public static final int MAGENTA = 0xFFFF00FF; // 255, 0, 255
    public static final int GRAY    = 0xFF808080; // 128, 128, 128

    public static int getAlpha(int color) { return (color >> 24) & 0xFF; }
    public static int getRed(int color)   { return (color >> 16) & 0xFF; }
    public static int getGreen(int color) { return (color >> 8) & 0xFF; }
    public static int getBlue(int color)  { return color & 0xFF; }

    public static int getARGB(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hsbToARGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0: r = (int)(brightness * 255.0f + 0.5f); g = (int)(t * 255.0f + 0.5f); b = (int)(p * 255.0f + 0.5f); break;
                case 1: r = (int)(q * 255.0f + 0.5f); g = (int)(brightness * 255.0f + 0.5f); b = (int)(p * 255.0f + 0.5f); break;
                case 2: r = (int)(p * 255.0f + 0.5f); g = (int)(brightness * 255.0f + 0.5f); b = (int)(t * 255.0f + 0.5f); break;
                case 3: r = (int)(p * 255.0f + 0.5f); g = (int)(q * 255.0f + 0.5f); b = (int)(brightness * 255.0f + 0.5f); break;
                case 4: r = (int)(t * 255.0f + 0.5f); g = (int)(p * 255.0f + 0.5f); b = (int)(brightness * 255.0f + 0.5f); break;
                case 5: r = (int)(brightness * 255.0f + 0.5f); g = (int)(p * 255.0f + 0.5f); b = (int)(q * 255.0f + 0.5f); break;
            }
        }
        return getARGB(255, r, g, b);
    }

    public static int multiplyColors(int texColor, float pixR, float pixG, float pixB) {
        int a = (texColor >> 24) & 0xFF;
        int r = (texColor >> 16) & 0xFF;
        int g = (texColor >> 8) & 0xFF;
        int b = texColor & 0xFF;

        int lightR = (int) pixR;
        int lightG = (int) pixG;
        int lightB = (int) pixB;

        r = (r * lightR) >> 8;
        g = (g * lightG) >> 8;
        b = (b * lightB) >> 8;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int setAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}