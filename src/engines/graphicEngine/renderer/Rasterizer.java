package engines.graphicEngine.renderer;

import engines.graphicEngine.math.geometry.Triangle;
import engines.graphicEngine.math.geometry.Vertex2D;
import engines.graphicEngine.math.geometry.Vertex3D;
import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.scene.lightRelative.PointLight;
import engines.graphicEngine.scene.lightRelative.SpotLight;

import java.awt.*;
import java.util.List;

public class Rasterizer {
    private Rasterizer() {}

    public static void drawTexturedTriangleTiled(int[] pixels, int winWidth, float[] depthBuffer,int tileMinX, int tileMaxX, int tileMinY, int tileMaxY, Triangle triangle) {
        Vertex3D[] verts = triangle.getVertices();
        Vertex2D[] uvs = triangle.getTextVertices();
        Color[] lights = triangle.getLightIntensities();

        int i0 = 0, i1 = 1, i2 = 2;
        if (verts[i0].y > verts[i1].y) { int temp = i0; i0 = i1; i1 = temp; }
        if (verts[i0].y > verts[i2].y) { int temp = i0; i0 = i2; i2 = temp; }
        if (verts[i1].y > verts[i2].y) { int temp = i1; i1 = i2; i2 = temp; }

        int x1 = (int)verts[i0].x, y1 = (int)verts[i0].y; float u1 = (float)uvs[i0].u, v1 = (float)uvs[i0].v, w1 = (float)uvs[i0].w, r1 = (float)lights[i0].getRed(), g1 = (float)lights[i0].getGreen(), b1 = (float)lights[i0].getBlue();
        int x2 = (int)verts[i1].x, y2 = (int)verts[i1].y; float u2 = (float)uvs[i1].u, v2 = (float)uvs[i1].v, w2 = (float)uvs[i1].w, r2 = (float)lights[i1].getRed(), g2 = (float)lights[i1].getGreen(), b2 = (float)lights[i1].getBlue();
        int x3 = (int)verts[i2].x, y3 = (int)verts[i2].y; float u3 = (float)uvs[i2].u, v3 = (float)uvs[i2].v, w3 = (float)uvs[i2].w, r3 = (float)lights[i2].getRed(), g3 = (float)lights[i2].getGreen(), b3 = (float)lights[i2].getBlue();

        int total_height = y3 - y1;
        if (total_height == 0) return;

        // On borne le dessin sur l'axe vertical (Y)
        int startY = Math.max(tileMinY, y1);
        int endY   = Math.min(tileMaxY, y3);

        if (startY > endY) return;

        int startRow = startY - y1;
        int endRow = endY - y1;

        for (int i = startRow; i <= endRow; i++) {
            int y = y1 + i;

            boolean isSecondHalf = i > (y2 - y1) || y2 == y1;
            int segment_height = isSecondHalf ? (y3 - y2) : (y2 - y1);

            if (segment_height == 0) continue;

            float alpha = (float) i / total_height;
            float beta  = (float) (i - (isSecondHalf ? y2 - y1 : 0)) / segment_height;


            int ax = x1 + (int)((x3 - x1) * alpha);
            float tex_au = u1 + (u3 - u1) * alpha;
            float tex_av = v1 + (v3 - v1) * alpha;
            float tex_aw = w1 + (w3 - w1) * alpha;

            float l_ar = r1 + ((r3 - r1) * alpha);
            float l_ag = g1 + ((g3 - g1) * alpha);
            float l_ab = b1 + ((b3 - b1) * alpha);


            int bx = isSecondHalf ? x2 + (int)((x3 - x2) * beta) : x1 + (int)((x2 - x1) * beta);
            float tex_bu = isSecondHalf ? u2 + (u3 - u2) * beta : u1 + (u2 - u1) * beta;
            float tex_bv = isSecondHalf ? v2 + (v3 - v2) * beta : v1 + (v2 - v1) * beta;
            float tex_bw = isSecondHalf ? w2 + (w3 - w2) * beta : w1 + (w2 - w1) * beta;

            float l_br = isSecondHalf ? r2 + (r3 - r2) * beta : r1 + (r2 - r1) * beta;
            float l_bg = isSecondHalf ? g2 + (g3 - g2) * beta : g1 + (g2 - g1) * beta;
            float l_bb = isSecondHalf ? b2 + (b3 - b2) * beta : b1 + (b2 - b1) * beta;


            drawTiledScanline(y, ax, bx, tex_au, tex_bu, tex_av, tex_bv, tex_aw, tex_bw, l_ar, l_ag, l_ab, l_br, l_bg, l_bb, triangle.getTexture(), pixels, winWidth, depthBuffer, tileMinX, tileMaxX);
        }
    }

    public static void drawTiledScanline(int y, int ax, int bx,
                                         float su, float eu,
                                         float sv, float ev,
                                         float sw, float ew,
                                         float slr, float slg, float slb,
                                         float elr, float elg, float elb,
                                         Texture texture, int[] pixels, int winWidth, float[] depthBuffer,
                                         int tileMinX, int tileMaxX) {

        int winHeight = depthBuffer.length / winWidth;
        if (y < 0 || y >= winHeight) return;

        if (ax > bx) {
            int tempX = ax; ax = bx; bx = tempX;
            float tempU = su; su = eu; eu = tempU;
            float tempV = sv; sv = ev; ev = tempV;
            float tempW = sw; sw = ew; ew = tempW;

            float tempR = slr; slr = elr; elr = tempR;
            float tempG = slg; slg = elg; elg = tempG;
            float tempB = slb; slb = elb; elb = tempB;
        }


        float tStep = 1.0f / (float) Math.max(1, bx - ax);

        float stepU = (eu - su) * tStep;
        float stepV = (ev - sv) * tStep;
        float stepW = (ew - sw) * tStep;

        float stepR = (elr - slr) * tStep;
        float stepG = (elg - slg) * tStep;
        float stepB = (elb - slb) * tStep;


        int startX = Math.max(tileMinX, ax);
        int endX   = Math.min(tileMaxX, bx);

        if (startX > endX) return;
        int diff = startX - ax;

        float tex_u = su + (stepU * diff);
        float tex_v = sv + (stepV * diff);
        float tex_w = sw + (stepW * diff);

        float l_r = slr + (stepR * diff);
        float l_g = slg + (stepG * diff);
        float l_b = slb + (stepB * diff);

        int index = y * winWidth + startX;

        for (int j = startX; j <= endX; j++) {
            if (tex_w > depthBuffer[index]) {
                float inverseW = 1 / tex_w;
                int pixelColor = texture.getPixelRGB(tex_u * inverseW, tex_v * inverseW);
                int finalColor = multiplyColors(pixelColor, l_r, l_g, l_b);

//              System.out.println(index +" "+pixels.length);
                pixels[index] = finalColor;
                depthBuffer[index] = tex_w;
            }
            index++;

            tex_u += stepU;
            tex_v += stepV;
            tex_w += stepW;

            l_r += stepR;
            l_g += stepG;
            l_b += stepB;
        }
    }

    public static void drawTexturedTriangleMultiThreaded(int[] pixels, int winWidth, float[] depthBuffer, int threadMinY, int threadMaxY, Triangle triangle) {
        Vertex3D[] verts = triangle.getVertices();
        Vertex2D[] uvs = triangle.getTextVertices();
        Color[] lights = triangle.getLightIntensities();

        int i0 = 0, i1 = 1, i2 = 2;
        if (verts[i0].y > verts[i1].y) { int temp = i0; i0 = i1; i1 = temp; }
        if (verts[i0].y > verts[i2].y) { int temp = i0; i0 = i2; i2 = temp; }
        if (verts[i1].y > verts[i2].y) { int temp = i1; i1 = i2; i2 = temp; }

        int x1 = (int)verts[i0].x, y1 = (int)verts[i0].y; float u1 = (float)uvs[i0].u, v1 = (float)uvs[i0].v, w1 = (float)uvs[i0].w, r1 = (float)lights[i0].getRed(), g1 = (float)lights[i0].getGreen(), b1 = (float)lights[i0].getBlue();
        int x2 = (int)verts[i1].x, y2 = (int)verts[i1].y; float u2 = (float)uvs[i1].u, v2 = (float)uvs[i1].v, w2 = (float)uvs[i1].w, r2 = (float)lights[i1].getRed(), g2 = (float)lights[i1].getGreen(), b2 = (float)lights[i1].getBlue();
        int x3 = (int)verts[i2].x, y3 = (int)verts[i2].y; float u3 = (float)uvs[i2].u, v3 = (float)uvs[i2].v, w3 = (float)uvs[i2].w, r3 = (float)lights[i2].getRed(), g3 = (float)lights[i2].getGreen(), b3 = (float)lights[i2].getBlue();

        int total_height = y3 - y1;
        if (total_height == 0) return;

        int startY = Math.max(y1, threadMinY);
        int endY = Math.min(y3, threadMaxY);

        if (startY > endY) return;

        int startThread = startY - y1;
        int endThread = endY - y1;

        for (int i = startThread; i <= endThread; i++) {
            int y = y1 + i;

            boolean isSecondHalf = i > (y2 - y1) || y2 == y1;
            int segment_height = isSecondHalf ? (y3 - y2) : (y2 - y1);

            if (segment_height == 0) continue;

            float alpha = (float) i / total_height;
            float beta  = (float) (i - (isSecondHalf ? y2 - y1 : 0)) / segment_height;


            int ax = x1 + (int)((x3 - x1) * alpha);
            float tex_au = u1 + (u3 - u1) * alpha;
            float tex_av = v1 + (v3 - v1) * alpha;
            float tex_aw = w1 + (w3 - w1) * alpha;

            float l_ar = r1 + ((r3 - r1) * alpha);
            float l_ag = g1 + ((g3 - g1) * alpha);
            float l_ab = b1 + ((b3 - b1) * alpha);


            int bx = isSecondHalf ? x2 + (int)((x3 - x2) * beta) : x1 + (int)((x2 - x1) * beta);
            float tex_bu = isSecondHalf ? u2 + (u3 - u2) * beta : u1 + (u2 - u1) * beta;
            float tex_bv = isSecondHalf ? v2 + (v3 - v2) * beta : v1 + (v2 - v1) * beta;
            float tex_bw = isSecondHalf ? w2 + (w3 - w2) * beta : w1 + (w2 - w1) * beta;

            float l_br = isSecondHalf ? r2 + (r3 - r2) * beta : r1 + (r2 - r1) * beta;
            float l_bg = isSecondHalf ? g2 + (g3 - g2) * beta : g1 + (g2 - g1) * beta;
            float l_bb = isSecondHalf ? b2 + (b3 - b2) * beta : b1 + (b2 - b1) * beta;


            drawScanline(y, ax, bx, tex_au, tex_bu, tex_av, tex_bv, tex_aw, tex_bw, l_ar, l_ag, l_ab, l_br, l_bg, l_bb, triangle.getTexture(), pixels, winWidth, depthBuffer);
        }
    }

    public static void drawTexturedTriangle(int[] pixels, int winWidth, float[] depthBuffer, Triangle triangle) {
        Vertex3D[] verts = triangle.getVertices();
        Vertex2D[] uvs = triangle.getTextVertices();
        Color[] lights = triangle.getLightIntensities();

        int i0 = 0, i1 = 1, i2 = 2;
        if (verts[i0].y > verts[i1].y) { int temp = i0; i0 = i1; i1 = temp; }
        if (verts[i0].y > verts[i2].y) { int temp = i0; i0 = i2; i2 = temp; }
        if (verts[i1].y > verts[i2].y) { int temp = i1; i1 = i2; i2 = temp; }

        int x1 = (int)verts[i0].x, y1 = (int)verts[i0].y; float u1 = (float)uvs[i0].u, v1 = (float)uvs[i0].v, w1 = (float)uvs[i0].w, r1 = (float)lights[i0].getRed(), g1 = (float)lights[i0].getGreen(), b1 = (float)lights[i0].getBlue();
        int x2 = (int)verts[i1].x, y2 = (int)verts[i1].y; float u2 = (float)uvs[i1].u, v2 = (float)uvs[i1].v, w2 = (float)uvs[i1].w, r2 = (float)lights[i1].getRed(), g2 = (float)lights[i1].getGreen(), b2 = (float)lights[i1].getBlue();
        int x3 = (int)verts[i2].x, y3 = (int)verts[i2].y; float u3 = (float)uvs[i2].u, v3 = (float)uvs[i2].v, w3 = (float)uvs[i2].w, r3 = (float)lights[i2].getRed(), g3 = (float)lights[i2].getGreen(), b3 = (float)lights[i2].getBlue();

        int total_height = y3 - y1;
        if (total_height == 0) return;

        for (int i = 0; i <= total_height; i++) {
            int y = y1 + i;

            boolean isSecondHalf = i > (y2 - y1) || y2 == y1; //second half of the triangle?
            int segment_height = isSecondHalf ? (y3 - y2) : (y2 - y1);

            if (segment_height == 0) continue;

            float alpha = (float) i / total_height;
            float beta  = (float) (i - (isSecondHalf ? y2 - y1 : 0)) / segment_height;

            int ax = x1 + (int)((x3 - x1) * alpha);

            float tex_au = u1 + (u3 - u1) * alpha;
            float tex_av = v1 + (v3 - v1) * alpha;
            float tex_aw = w1 + (w3 - w1) * alpha;

            float l_ar = r1 + ((r3 - r1) * alpha);
            float l_ag = g1 + ((g3 - g1) * alpha);
            float l_ab = b1 + ((b3 - b1) * alpha);

            int bx = isSecondHalf ? x2 + (int)((x3 - x2) * beta) : x1 + (int)((x2 - x1) * beta);
            float tex_bu = isSecondHalf ? u2 + (u3 - u2) * beta : u1 + (u2 - u1) * beta;
            float tex_bv = isSecondHalf ? v2 + (v3 - v2) * beta : v1 + (v2 - v1) * beta;
            float tex_bw = isSecondHalf ? w2 + (w3 - w2) * beta : w1 + (w2 - w1) * beta;

            float l_br = isSecondHalf ? r2 + (r3 - r2) * beta : r1 + (r2 - r1) * beta;
            float l_bg = isSecondHalf ? g2 + (g3 - g2) * beta : g1 + (g2 - g1) * beta;
            float l_bb = isSecondHalf ? b2 + (b3 - b2) * beta : b1 + (b2 - b1) * beta;


            drawScanline(y, ax, bx, tex_au, tex_bu, tex_av, tex_bv, tex_aw, tex_bw, l_ar, l_ag, l_ab, l_br, l_bg, l_bb, triangle.getTexture(), pixels, winWidth, depthBuffer);
        }
    }

    public static void drawScanline(int y, int ax, int bx, float su, float eu, float sv, float ev, float sw, float ew, float slr, float slg, float slb, float elr, float elg, float elb,  Texture texture, int[] pixels, int winWidth, float[] depthBuffer) {
        int winHeight = depthBuffer.length / winWidth;
        if (y < 0 || y >= winHeight) return;

        if (ax > bx) {
            int tempX = ax; ax = bx; bx = tempX;
            float tempU = su; su = eu; eu = tempU;
            float tempV = sv; sv = ev; ev = tempV;
            float tempW = sw; sw = ew; ew = tempW;

            float tempR = slr; slr = elr; elr = tempR;
            float tempG = slg; slg = elg; elg = tempG;
            float tempB = slb; slb = elb; elb = tempB;
        }


        float tStep = 1.0f / (float) Math.max(1, bx - ax);

        float stepU = (eu - su) * tStep;
        float stepV = (ev - sv) * tStep;
        float stepW = (ew - sw) * tStep;

        float stepR = (elr - slr) * tStep;
        float stepG = (elg - slg) * tStep;
        float stepB = (elb - slb) * tStep;


        int startX = Math.max(0, ax);
        int endX = Math.min(winWidth - 1, bx);

        if (startX > endX) return;
        int diff = startX - ax;

        float tex_u = su + (stepU * diff);
        float tex_v = sv + (stepV * diff);
        float tex_w = sw + (stepW * diff);

        float l_r = slr + (stepR * diff);
        float l_g = slg + (stepG * diff);
        float l_b = slb + (stepB * diff);

        int index = y * winWidth + startX;

        for (int j = ax; j <= bx; j++) {
            if (tex_w > depthBuffer[index]) {
                float inverseW = 1 / tex_w;
                int pixelColor = texture.getPixelRGB(tex_u * inverseW, tex_v * inverseW);
                int finalColor = multiplyColors(pixelColor, l_r, l_g, l_b);

//              System.out.println(index +" "+pixels.length);
                pixels[index] = finalColor;
                depthBuffer[index] = tex_w;
            }
            index++;

            tex_u += stepU;
            tex_v += stepV;
            tex_w += stepW;

            l_r += stepR;
            l_g += stepG;
            l_b += stepB;
        }
    }

    public static void computeGouraudLighting(List<PointLight> lightQueue, boolean isFlipped, Triangle triangle) {
        for (int i = 0; i < 3; i++) {
            Vector3D vertexNormal = triangle.getNormalsVertices()[i];

            Vector3D normalToUse;
            if (isFlipped) {
                normalToUse = vertexNormal.scaleInPlace(-1);
            } else {
                normalToUse = vertexNormal;
            }

            Vertex3D vertex = triangle.getVertices()[i];

            double totalR = 0.15;
            double totalG = 0.15;
            double totalB = 0.15;

            for (PointLight light : lightQueue) {
                if (!light.isOn()) continue;

                Vector3D lightVector = light.getPosition().sub(vertex);
                double distance = lightVector.getLength();
                lightVector.normalizeInPlace();

                double spotIntensity = 1.0;
                if (light instanceof SpotLight) {
                    SpotLight spot = (SpotLight) light;
                    double spotFactor = -lightVector.dotProduct(spot.getDirection());

                    if (spotFactor < spot.getCutOffAngle()) {
                        continue;
                    }

                    double range = 1.0 - spot.getCutOffAngle();
                    spotIntensity = Math.pow((spotFactor - spot.getCutOffAngle()) / range,2);
                }

                double dp = normalToUse.dotProduct(lightVector);
                if (dp > 0) {
                    double attenuation = 1.0 / (1.0 + light.getFallOff() * distance);
                    double impact = dp * attenuation * spotIntensity;

                    Color lightColor = light.getLightColor();
                    double lightR = lightColor.getRed()   * 0.003921568627451;
                    double lightG = lightColor.getGreen() * 0.003921568627451;
                    double lightB = lightColor.getBlue()  * 0.003921568627451;

                    totalR += (impact * lightR);
                    totalG += (impact * lightG);
                    totalB += (impact * lightB);
                }
            }

            totalR = Math.min(1.0, totalR);
            totalG = Math.min(1.0, totalG);
            totalB = Math.min(1.0, totalB);

            int finalRed   = (int) (triangle.getColor().getRed()   * totalR);
            int finalGreen = (int) (triangle.getColor().getGreen() * totalG);
            int finalBlue  = (int) (triangle.getColor().getBlue()  * totalB);

            triangle.getLightIntensities()[i] = new Color(finalRed, finalGreen, finalBlue);
        }
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

    public static boolean isFacing(Camera camera, boolean isFlipped, Triangle triangle) {
        Vertex3D[] verts = triangle.getVertices();
        Vertex3D v0 = verts[0];
        Vertex3D v1 = verts[1];
        Vertex3D v2 = verts[2];

        float rx = (float)(v0.getX() - camera.getCameraPosition().getX());
        float ry = (float)(v0.getY() - camera.getCameraPosition().getY());
        float rz = (float)(v0.getZ() - camera.getCameraPosition().getZ());

        float ax = (float)(v1.getX() - v0.getX());
        float ay = (float)(v1.getY() - v0.getY());
        float az = (float)(v1.getZ() - v0.getZ());

        float bx = (float)(v2.getX() - v0.getX());
        float by = (float)(v2.getY() - v0.getY());
        float bz = (float)(v2.getZ() - v0.getZ());

        float nx = (ay * bz - az * by);
        float ny = (az * bx - ax * bz);
        float nz = (ax * by - ay * bx);

        float dotProduct = (nx * rx) + (ny * ry) + (nz * rz);

        if (isFlipped) {
            return dotProduct > 0.0;
        } else {
            return dotProduct < 0.0;
        }
    }

    public static Triangle convertToWindowSpace(int iWinWidth, int iWinHeight, Triangle triangle) {
        // X/Y Inverted so need to put them back???
        triangle.scaleInPlaceX(-1);
        triangle.scaleInPlaceY(-1);

        // Offset into visible normalized space
        Vertex3D[] vertsIn = triangle.getVertices();

        vertsIn[0].x = vertsIn[0].x + 1;
        vertsIn[0].y = vertsIn[0].y + 1;

        vertsIn[1].x = vertsIn[1].x + 1;
        vertsIn[1].y = vertsIn[1].y + 1;

        vertsIn[2].x = vertsIn[2].x + 1;
        vertsIn[2].y = vertsIn[2].y + 1;

        // Scaling to screen dimension
        triangle.scaleInPlaceX(0.5 * iWinWidth);
        triangle.scaleInPlaceY(0.5 * iWinHeight);

        return triangle;
    }

    public static Triangle homogeneousDivisionInPlace(Triangle triangle) {
        Vertex3D[] vertsIn = triangle.getVertices();
        Vertex2D[] textVertsIn = triangle.getTextVertices();

        double invW0 = 1 / vertsIn[0].w;
        double invW1 = 1 / vertsIn[1].w;
        double invW2 = 1 / vertsIn[2].w;

        textVertsIn[0].u = textVertsIn[0].u * invW0;
        textVertsIn[1].u = textVertsIn[1].u * invW1;
        textVertsIn[2].u = textVertsIn[2].u * invW2;

        textVertsIn[0].v = textVertsIn[0].v * invW0;
        textVertsIn[1].v = textVertsIn[1].v * invW1;
        textVertsIn[2].v = textVertsIn[2].v * invW2;


        textVertsIn[0].w = invW0;
        textVertsIn[1].w = invW1;
        textVertsIn[2].w = invW2;


        vertsIn[0].divideInPlace(vertsIn[0].w);
        vertsIn[1].divideInPlace(vertsIn[1].w);
        vertsIn[2].divideInPlace(vertsIn[2].w);

        return triangle;
    }

    public static Triangle projectToScreenInPlace(Matrix projectionMatrix, int iWinWidth, int iWinHeight, Triangle triangle) {
        triangle.transformVertexInPlace(projectionMatrix);
        homogeneousDivisionInPlace(triangle);
        convertToWindowSpace(iWinWidth, iWinHeight, triangle);

        return triangle;
    }
}
