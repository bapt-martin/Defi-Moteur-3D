package engines.graphicEngine.math.geometry;

import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Texture;
import engines.graphicEngine.scene.lightRelative.PointLight;
import engines.graphicEngine.scene.lightRelative.SpotLight;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Triangle {
    private final Vertex3D[] vertices = new Vertex3D[3];

    private final Vertex2D[] textVertices = new Vertex2D[3];
    private Texture texture;

    private Vector3D[] normalsVertices = new Vector3D[3];
    private Vector3D normal = null;

    private Color[] lightIntensities = new Color[3];
    private Color color;

    private Matrix parentWorldTransformMatrix;

    private final int[] xs = new int[3];
    private final int[] ys = new int[3];


    public Triangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color[] lights, Color color, Texture texture) {
        for (int i = 0; i < 3; i++) {
            this.vertices[i] =  verts[i];
            this.textVertices[i] = textVerts[i];
            this.normalsVertices[i] = normals[i];
            this.lightIntensities[i] = (lights==null) ? color : lights[i];
        }
        this.color = color != null ? color : Color.WHITE;
        this.texture = texture;
    }

    public static Triangle createShallowTriangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color[] lights, Color color, Texture texture) {
        return new Triangle(verts, textVerts, normals, lights, color, texture);
    }

    public static Triangle createShallowTriangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color color) {
        return createShallowTriangle(verts, textVerts, normals, null, color, null);
    }

    public static Triangle createDeepTriangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color[] lights, Color color, Texture texture) {
        Vertex3D[] newVerts = new Vertex3D[3];
        Vertex2D[] newUVs = new Vertex2D[3];
        Vector3D[] newNormals = new Vector3D[3];
        Color[] newLights = new Color[3];

        for (int i = 0; i < 3; i++) {
            newVerts[i] = new Vertex3D(verts[i].getX(), verts[i].getY(), verts[i].getZ());
            newUVs[i] = new Vertex2D(textVerts[i].u, textVerts[i].v, textVerts[i].w);
            newNormals[i] = (normals==null) ? new Vector3D(0,0,0) :new Vector3D(normals[i].getX(), normals[i].getY(), normals[i].getZ());
            newLights[i] = (lights==null) ? color : lights[i];
        }
        return new Triangle(newVerts, newUVs, newNormals, newLights, color, texture);
    }

    public static Triangle createDeepTriangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color color) {
        return createDeepTriangle(verts, textVerts, normals, null, color, null);
    }

    public static Triangle createDeepTriangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color color, Texture texture) {
        return createDeepTriangle(verts, textVerts, normals, null, color, texture);
    }

    public static Triangle createDeepTriangle(Vertex3D[] verts, Vertex2D[] textVerts,  Color[] lights, Color color, Texture texture) {
        return createDeepTriangle(verts, textVerts, null, lights, color, texture);
    }

    public Triangle shallowClone() {
        return new Triangle(
                this.vertices,
                this.textVertices,
                this.normalsVertices,
                this.lightIntensities,
                this.color,
                this.texture
        );
    }

    public Triangle deepClone() {
        Vertex3D[] newVerts = new Vertex3D[3];
        Vertex2D[] newUVs = new Vertex2D[3];
        Vector3D[] newNormals = new Vector3D[3];
        Color[] newLights = new Color[3];

        for (int i = 0; i < 3; i++) {
            newVerts[i] = new Vertex3D(this.vertices[i].getX(), this.vertices[i].getY(), this.vertices[i].getZ());
            newUVs[i] = new Vertex2D(this.textVertices[i].u, this.textVertices[i].v, this.textVertices[i].w);
            newNormals[i] = new Vector3D(this.normalsVertices[i].getX(), this.normalsVertices[i].getY(), this.normalsVertices[i].getZ());
            newLights[i] = this.lightIntensities[i];
        }

        return new Triangle(newVerts, newUVs, newNormals, newLights, this.color, this.texture);
    }

    public Vector3D getNormal() {
        updateNormal();
        return normal;
    }

    public Triangle homogeneousDivisionInPlace() {
        Vertex3D[] vertsIn = this.getVertices();
        Vertex2D[] textVertsIn = this.getTextVertices();

        textVertsIn[0].u = textVertsIn[0].u / vertsIn[0].w;
        textVertsIn[1].u = textVertsIn[1].u / vertsIn[1].w;
        textVertsIn[2].u = textVertsIn[2].u / vertsIn[2].w;

        textVertsIn[0].v = textVertsIn[0].v / vertsIn[0].w;
        textVertsIn[1].v = textVertsIn[1].v / vertsIn[1].w;
        textVertsIn[2].v = textVertsIn[2].v / vertsIn[2].w;


        textVertsIn[0].w = 1 / vertsIn[0].w;
        textVertsIn[1].w = 1 / vertsIn[1].w;
        textVertsIn[2].w = 1 / vertsIn[2].w;


        vertsIn[0].divideInPlace(vertsIn[0].w);
        vertsIn[1].divideInPlace(vertsIn[1].w);
        vertsIn[2].divideInPlace(vertsIn[2].w);

        return this;
    }

    public Triangle scaleInPlace(double factor) {
        Vertex3D[] vertsIn = this.getVertices();

        vertsIn[0].scaleInPlace(factor);
        vertsIn[1].scaleInPlace(factor);
        vertsIn[2].scaleInPlace(factor);

        return this;
    }

    public Triangle scaleInPlaceX(double factor) {
        Vertex3D[] vertsIn = this.getVertices();

        vertsIn[0].setX(vertsIn[0].getX() * factor);
        vertsIn[1].setX(vertsIn[1].getX() * factor);
        vertsIn[2].setX(vertsIn[2].getX() * factor);

        return this;
    }

    public Triangle scaleInPlaceY(double factor) {
        Vertex3D[] vertsIn = this.getVertices();

        vertsIn[0].setY(vertsIn[0].getY() * factor);
        vertsIn[1].setY(vertsIn[1].getY() * factor);
        vertsIn[2].setY(vertsIn[2].getY() * factor);

        return this;
    }

    public Triangle translateInPlace(Vector3D vectTranslation) {
        Vertex3D[] vertsIn = this.getVertices();

        vertsIn[0].translateInPlace(vectTranslation);
        vertsIn[1].translateInPlace(vectTranslation);
        vertsIn[2].translateInPlace(vectTranslation);

        return this;
    }

    public void drawTriangle(int[] pixels, boolean isOutlineRender, int winWidth, float[] depthBuffer) {
        // Getting back the coordinate to draw the 2D triangle
        this.get2DCoordinates(xs, ys);

        if (this.texture == null) {
            this.drawTexturedTriangle(pixels, Texture.WHITE_PIXEL, winWidth, depthBuffer);
        } else {
            this.drawTexturedTriangle(pixels, this.texture, winWidth, depthBuffer);
        }

//        if (isOutlineRender) {
//            g.setColor(Color.BLACK);
//            g.drawPolygon(xs, ys, 3);
//        }
    }

    public void drawTexturedTriangle(int[] pixels, Texture texture, int winWidth, float[] depthBuffer) {
        record RasterVertex(int x, int y, float u, float v, float w, float r, float g, float b) {
            static void swap(RasterVertex[] arr, int i, int j) {
                RasterVertex temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }

        Vertex3D[] verts = this.vertices;
        Vertex2D[] uvs = this.textVertices;
        Color[] lights = this.lightIntensities;

        RasterVertex[] rv = {
                new RasterVertex((int) verts[0].x, (int) verts[0].y, (float) uvs[0].u, (float) uvs[0].v, (float) uvs[0].w, (float) lights[0].getRed(), (float) lights[0].getGreen(), (float) lights[0].getBlue()),
                new RasterVertex((int) verts[1].x, (int) verts[1].y, (float) uvs[1].u, (float) uvs[1].v, (float) uvs[1].w, (float) lights[1].getRed(), (float) lights[1].getGreen(), (float) lights[1].getBlue()),
                new RasterVertex((int) verts[2].x, (int) verts[2].y, (float) uvs[2].u, (float) uvs[2].v, (float) uvs[2].w, (float) lights[2].getRed(), (float) lights[2].getGreen(), (float) lights[2].getBlue())
        };

        if (rv[0].y() > rv[1].y()) RasterVertex.swap(rv, 0, 1);
        if (rv[0].y() > rv[2].y()) RasterVertex.swap(rv, 0, 2);
        if (rv[1].y() > rv[2].y()) RasterVertex.swap(rv, 1, 2);

        int x1 = rv[0].x(), y1 = rv[0].y(); float u1 = rv[0].u(), v1 = rv[0].v(), w1 = rv[0].w(), r1 = rv[0].r(), g1 = rv[0].g(), b1 = rv[0].b();
        int x2 = rv[1].x(), y2 = rv[1].y(); float u2 = rv[1].u(), v2 = rv[1].v(), w2 = rv[1].w(), r2 = rv[1].r(), g2 = rv[1].g(), b2 = rv[1].b();
        int x3 = rv[2].x(), y3 = rv[2].y(); float u3 = rv[2].u(), v3 = rv[2].v(), w3 = rv[2].w(), r3 = rv[2].r(), g3 = rv[2].g(), b3 = rv[2].b();

        int total_height = y3 - y1;

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


            drawScanline(y, ax, bx, tex_au, tex_bu, tex_av, tex_bv, tex_aw, tex_bw, l_ar, l_ag, l_ab, l_br, l_bg, l_bb, texture, pixels, winWidth, depthBuffer);
        }
    }

    private void drawScanline(int y, int ax, int bx, float su, float eu, float sv, float ev, float sw, float ew, float slr, float slg, float slb, float elr, float elg, float elb,  Texture texture, int[] pixels, int winWidth, float[] depthBuffer) {
        if (ax > bx) {
            int tempX = ax; ax = bx; bx = tempX;
            float tempU = su; su = eu; eu = tempU;
            float tempV = sv; sv = ev; ev = tempV;
            float tempW = sw; sw = ew; ew = tempW;

            float tempR = slr; slr = elr; elr = tempR;
            float tempG = slg; slg = elg; elg = tempG;
            float tempB = slb; slb = elb; elb = tempB;
        }


        float tstep = 1.0f / (float) Math.max(1, bx - ax);

        float stepU = (eu - su) * tstep;
        float stepV = (ev - sv) * tstep;
        float stepW = (ew - sw) * tstep;

        float stepR = (elr - slr) * tstep;
        float stepG = (elg - slg) * tstep;
        float stepB = (elb - slb) * tstep;


        float tex_u = su;
        float tex_v = sv;
        float tex_w = sw;

        float l_r = slr;
        float l_g = slg;
        float l_b = slb;

        for (int j = ax; j <= bx; j++) {
            int pixelColor = texture.getPixelRGB(tex_u / tex_w, tex_v / tex_w);
            int finalColor = multiplyColors(pixelColor, l_r, l_g, l_b);

            int index = y * winWidth + j;
//            System.out.println(index +" "+pixels.length);

            if (j >= 0 && j < winWidth && index >= 0 && index < pixels.length) {
                if (tex_w > depthBuffer[index]) {
                    pixels[index] = finalColor;
                    depthBuffer[index] = tex_w;
                }
            }
            tex_u += stepU;
            tex_v += stepV;
            tex_w += stepW;

            l_r += stepR;
            l_g += stepG;
            l_b += stepB;
        }
    }

    private int multiplyColors(int texColor, float pixR, float pixG, float pixB) {
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

    public Triangle convertToWindowSpace(int iWinWidth, int iWinHeight) {
        // X/Y Inverted so need to put them back???
        this.scaleInPlaceX(-1);
        this.scaleInPlaceY(-1);

        // Offset into visible normalized space
        Vector3D vOffsetView = new Vector3D(1,1,0);
        this.translateInPlace(vOffsetView);

        // Scaling to screen dimension
        this.scaleInPlaceX(0.5 * iWinWidth);
        this.scaleInPlaceY(0.5 * iWinHeight);

        return this;
    }


    public Triangle projectToScreenInPlace(Matrix projectionMatrix, int iWinWidth, int iWinHeight) {
        this.transformVertexInPlace(projectionMatrix);
        this.homogeneousDivisionInPlace();
        this.convertToWindowSpace(iWinWidth, iWinHeight);

        return this;
    }

    public boolean isFacing(Camera camera, boolean isFlipped) {
        // On récupère les 3 sommets du triangle (Lecture seule, 0 allocation)
        Vertex3D[] verts = this.getVertices();
        Vertex3D v0 = verts[0];
        Vertex3D v1 = verts[1];
        Vertex3D v2 = verts[2];

        // 1. Rayon de la caméra (Calcul des composantes X, Y, Z directement)
        double rx = v0.getX() - camera.getCameraPosition().getX();
        double ry = v0.getY() - camera.getCameraPosition().getY();
        double rz = v0.getZ() - camera.getCameraPosition().getZ();

        // 2. Calcul de la normale de la face (Cross Product en ligne)
        // Vecteur de l'Arête A (v1 - v0)
        double ax = v1.getX() - v0.getX();
        double ay = v1.getY() - v0.getY();
        double az = v1.getZ() - v0.getZ();

        // Vecteur de l'Arête B (v2 - v0)
        double bx = v2.getX() - v0.getX();
        double by = v2.getY() - v0.getY();
        double bz = v2.getZ() - v0.getZ();

        // Produit vectoriel (Cross Product) pour obtenir la Normale de la face (nx, ny, nz)
        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;

        // 3. Produit scalaire (Dot Product) entre la Normale et le Rayon Caméra
        double dotProduct = (nx * rx) + (ny * ry) + (nz * rz);

        // 4. Résultat immédiat
        if (isFlipped) {
            return dotProduct > 0.0;
        } else {
            return dotProduct < 0.0;
        }
    }

    public void updateNormal() {
        Vector3D edge1 = vertices[1].sub(vertices[0]);
        Vector3D edge2 = vertices[2].sub(vertices[0]);
        normal = edge1.crossProduct(edge2).normalizeInPlace();
    }

    public Vector3D getCenter() {
        Vertex3D[] v = this.getVertices();
        double cx = (v[0].x + v[1].x + v[2].x) / 3.0;
        double cy = (v[0].y + v[1].y + v[2].y) / 3.0;
        double cz = (v[0].z + v[1].z + v[2].z) / 3.0;
        return new Vector3D(cx, cy, cz);
    }

    public void setLighting(List<PointLight> lightQueue, boolean isFlipped) {
        for (int i = 0; i < 3; i++) {
            Vector3D vertexNormal = this.normalsVertices[i];

            Vector3D normalToUse;
            if (isFlipped) {
                normalToUse = vertexNormal.scaleInPlace(-1);
            } else {
                normalToUse = vertexNormal;
            }

            Vertex3D vertex = this.vertices[i];

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
                    double lightR = lightColor.getRed() / 255.0;
                    double lightG = lightColor.getGreen() / 255.0;
                    double lightB = lightColor.getBlue() / 255.0;

                    totalR += (impact * lightR);
                    totalG += (impact * lightG);
                    totalB += (impact * lightB);
                }
            }

            totalR = Math.min(1.0, totalR);
            totalG = Math.min(1.0, totalG);
            totalB = Math.min(1.0, totalB);

            int finalRed = (int) (this.color.getRed()     * totalR);
            int finalGreen = (int) (this.color.getGreen() * totalG);
            int finalBlue = (int) (this.color.getBlue()   * totalB);

            this.lightIntensities[i] = new Color(finalRed, finalGreen, finalBlue);
        }
    }

    public void transformInPool(Matrix matTransform, Color color, Texture texture, Triangle poolTriangle) {
        Vertex3D[] vertsTriIn = this.getVertices();
        Vertex2D[] textsVertsTriIn = this.getTextVertices();
        Vector3D[] normalsTriIn = this.getNormalsVertices();
        Color[] colorsTriIn = this.getLightIntensities();

        Vertex3D[] vertsTriOut = poolTriangle.getVertices();
        Vertex2D[] textsVertsTriOut = poolTriangle.getTextVertices();
        Vector3D[] normalsTriOut = poolTriangle.getNormalsVertices();
        Color[] colorsTriOut = poolTriangle.getLightIntensities();

        double[][] m = matTransform.getMatrix();
        for (int i = 0; i < 3; i++) {
            vertsTriIn[i].transformAndStoreIn(m, vertsTriOut[i]);

            textsVertsTriOut[i].u = textsVertsTriIn[i].u;
            textsVertsTriOut[i].v = textsVertsTriIn[i].v;
            textsVertsTriOut[i].w = textsVertsTriIn[i].w;

            normalsTriIn[i].transformAndStoreIn(m, normalsTriOut[i]);

            colorsTriOut[i] = (colorsTriIn==null) ? color : colorsTriIn[i];
        }

        poolTriangle.setColor(color);
        poolTriangle.setTexture(texture);
    }

    public Triangle transformed(Matrix matTransform, Color color, Texture texture) {
        Vertex3D[] vertsTriTransformed = new Vertex3D[3];
        Vertex2D[] textVerticesCopy = new Vertex2D[3];
        Vector3D[] normalsCopy = new Vector3D[3];

        Vertex3D[] vertsTriIn = this.getVertices();
        Vertex2D[] meshTextVertices = this.getTextVertices();
        Vector3D[] normalsTriIn = this.getNormalsVertices();

        for (int i = 0; i < 3; i++) {
            vertsTriTransformed[i] = vertsTriIn[i].transformed(matTransform);
            textVerticesCopy[i] = new Vertex2D(meshTextVertices[i]);
            normalsCopy[i] = normalsTriIn[i];
        }

        return createDeepTriangle(vertsTriTransformed, textVerticesCopy, normalsCopy, color, texture);
    }

    public Triangle transformVertexInPlace(Matrix matTransform) {
        Vertex3D[] vertsTriIn = this.getVertices();

        for (int i = 0; i < 3; i++) {
            vertsTriIn[i].transformTuple3DInPlace(matTransform);
        }

        return this;
    }

    public Triangle transformTextVertexInPlace(Matrix matTransform) {
        Vertex2D[] textVertsTriIn = this.getTextVertices();

        for (int i = 0; i < 3; i++) {
            textVertsTriIn[i].transformVertex2DInPlace(matTransform);
        }

        return this;
    }

    public void get2DCoordinates(int[] xs, int[] ys) {
        for (int i = 0; i < 3; i++) {
            Vertex3D[] vert = this.getVertices();
            xs[i] = (int) Math.round(vert[i].getX());
            ys[i] = (int) Math.round(vert[i].getY());
        }
    }

    public Vertex3D[] getVertices() {
        return vertices;
    }

    public Vertex2D[] getTextVertices() {
        return textVertices;
    }

    public Color[] getLightIntensities() {
        return lightIntensities;
    }

    public Vector3D[] getNormalsVertices() {
        return normalsVertices;
    }

    public void setLightIntensities(Color[] lightIntensities) {
        this.lightIntensities = lightIntensities;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Matrix getParentWorldTransformMatrix() {
        return parentWorldTransformMatrix;
    }

    public void setParentWorldTransformMatrix(Matrix parentWorldTransformMatrix) {
        this.parentWorldTransformMatrix = parentWorldTransformMatrix;
    }

    @Override
    public String toString() {
        return "engine.math.geometry.Triangle{" +
                "vertices=" + Arrays.toString(vertices) +
                '}';
    }
}
