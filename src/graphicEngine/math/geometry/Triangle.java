package graphicEngine.math.geometry;

import graphicEngine.renderer.Camera;
import graphicEngine.math.tools.Matrix;
import graphicEngine.math.tools.Vector3D;
import graphicEngine.renderer.Texture;

import java.awt.*;
import java.util.Arrays;

import static java.awt.Color.*;
import static java.lang.Math.abs;

public class Triangle {
    private Vertex3D[] vertices = new Vertex3D[3];
    private Vertex2D[] textVertices = new Vertex2D[3];
    private Texture texture = new Texture();
    private Color color;
    private Vector3D normal = null;
    private final int[] xs = new int[3];
    private final int[] ys = new int[3];


    public Triangle() {
        vertices[0] = new Vertex3D();
        vertices[1] = new Vertex3D();
        vertices[2] = new Vertex3D();
        this.color = BLUE;
    }

    public Triangle(Vertex3D p1, Vertex3D p2, Vertex3D p3, Vertex2D t1, Vertex2D t2, Vertex2D t3) {
        vertices[0] = new Vertex3D(p1);
        vertices[1] = new Vertex3D(p2);
        vertices[2] = new Vertex3D(p3);

        textVertices[0] = new Vertex2D(t1);
        textVertices[1] = new Vertex2D(t2);
        textVertices[2] = new Vertex2D(t3);

        color = BLACK;
    }

    public Triangle(Vertex3D p1, Vertex3D p2, Vertex3D p3, Vertex2D t1, Vertex2D t2, Vertex2D t3, Color color, Texture texture) {
        vertices[0] = new Vertex3D(p1);
        vertices[1] = new Vertex3D(p2);
        vertices[2] = new Vertex3D(p3);

        textVertices[0] = new Vertex2D(t1);
        textVertices[1] = new Vertex2D(t2);
        textVertices[2] = new Vertex2D(t3);

        this.color = color;
        this.texture = texture;
    }

    public Triangle(Vertex3D p1, Vertex3D p2, Vertex3D p3, Color color) {
        this(p1, p2, p3, new Vertex2D(), new Vertex2D(), new Vertex2D());
        this.color = color;
    }

    public Triangle(Vertex2D t1, Vertex2D t2, Vertex2D t3, Color color) {
        this(new Vertex3D(), new Vertex3D(), new Vertex3D(), t1, t2, t3);
        this.color = color;
    }

    public Triangle(Vertex3D[] verts, Color color) {
        this(verts[0], verts[1], verts[2], color);
    }

    public Triangle(Vertex2D[] textVerts, Color color) {
        this(textVerts[0], textVerts[1], textVerts[2], color);
    }

    public Triangle(Vertex3D[] verts, Vertex2D[] textVerts, Color color) {
        this(verts[0], verts[1], verts[2],textVerts[0], textVerts[1], textVerts[2], color, null);
    }

    public Triangle(Vertex3D[] verts, Vertex2D[] textVerts, Color color, Texture texture) {
        this(verts[0], verts[1], verts[2],textVerts[0], textVerts[1], textVerts[2], color, texture);
    }

    public void copyFrom(Triangle other) {
        this.vertices[0].copyFrom(other.vertices[0]);
        this.vertices[1].copyFrom(other.vertices[1]);
        this.vertices[2].copyFrom(other.vertices[2]);
        this.color = other.color;
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

    public void drawTriangle(Graphics g, boolean isOutlineRender, int winWidth, float[] depthBuffer) {
        // Getting back the coordinate to draw the 2D triangle
        this.get2DCoordinates(xs, ys);

        if (this.texture == null) {
            this.drawTexturedTriangle(g, Texture.WHITE_PIXEL, winWidth, depthBuffer);
        } else {
            this.drawTexturedTriangle(g, this.texture, winWidth, depthBuffer);
        }

        if (isOutlineRender) {
            g.setColor(Color.BLACK);
            g.drawPolygon(xs, ys, 3);
        }
    }


    public void drawTexturedTriangle(Graphics g, Texture texture, int winWidth, float[] depthBuffer) {

        Vertex3D[] verts = this.getVertices();
        Vertex2D[] uvs = this.getTextVertices();

        int x1 = (int) verts[0].x;
        int y1 = (int) verts[0].y;

        int x2 = (int) verts[1].x;
        int y2 = (int) verts[1].y;

        int x3 = (int) verts[2].x;
        int y3 = (int) verts[2].y;

        float u1 = (float) uvs[0].u;
        float v1 = (float) uvs[0].v;
        float w1 = (float) uvs[0].w;

        float u2 = (float) uvs[1].u;
        float v2 = (float) uvs[1].v;
        float w2 = (float) uvs[1].w;

        float u3 = (float) uvs[2].u;
        float v3 = (float) uvs[2].v;
        float w3 = (float) uvs[2].w;

        if (y1 > y2) {
            int tempX = x1; x1 = x2; x2 = tempX;
            int tempY = y1; y1 = y2; y2 = tempY;
            float tempU = u1; u1 = u2; u2 = tempU;
            float tempV = v1; v1 = v2; v2 = tempV;
            float tempW = w1; w1 = w2; w2 = tempW;
        }

        if (y1 > y3) {
            int tempX = x1; x1 = x3; x3 = tempX;
            int tempY = y1; y1 = y3; y3 = tempY;
            float tempU = u1; u1 = u3; u3 = tempU;
            float tempV = v1; v1 = v3; v3 = tempV;
            float tempW = w1; w1 = w3; w3 = tempW;
        }

        if (y2 > y3) {
            int tempX = x2; x2 = x3; x3 = tempX;
            int tempY = y2; y2 = y3; y3 = tempY;
            float tempU = u2; u2 = u3; u3 = tempU;
            float tempV = v2; v2 = v3; v3 = tempV;
            float tempW = w2; w2 = w3; w3 = tempW;
        }

        int   dy1 = y2 - y1;
        int   dx1 = x2 - x1;
        float dv1 = v2 - v1;
        float du1 = u2 - u1;
        float dw1 = w2 - w1;


        int   dy2 = y3 - y1;
        int   dx2 = x3 - x1;
        float dv2 = v3 - v1;
        float du2 = u3 - u1;
        float dw2 = w3 - w1;

        float tex_u; float tex_v; float tex_w;

        float dax_step = 0; float dbx_step = 0;
        float du1_step = 0; float du2_step = 0;
        float dv1_step = 0; float dv2_step = 0;
        float dw1_step = 0; float dw2_step = 0;

        if (dy1 != 0) dax_step = dx1 / (float) abs(dy1);
        if (dy2 != 0) dbx_step = dx2 / (float) abs(dy2);

        if (dy1 != 0) du1_step = du1 / (float) abs(dy1);
        if (dy1 != 0) dv1_step = dv1 / (float) abs(dy1);
        if (dy1 != 0) dw1_step = dw1 / (float) abs(dy1);

        if (dy2 != 0) du2_step = du2 / (float) abs(dy2);
        if (dy2 != 0) dv2_step = dv2 / (float) abs(dy2);
        if (dy2 != 0) dw2_step = dw2 / (float) abs(dy2);

        if (dy1 != 0) {
            for (int i = y1; i <= y2; i++) {
                int ax = (int) (x1 + (i-y1) * dax_step);
                int bx = (int) (x1 + (i-y1) * dbx_step);

                float tex_su = (u1 + (i-y1) * du1_step);
                float tex_sv = (v1 + (i-y1) * dv1_step);
                float tex_sw = (w1 + (i-y1) * dw1_step);

                float tex_eu = (u1 + (i-y1) * du2_step);
                float tex_ev = (v1 + (i-y1) * dv2_step);
                float tex_ew = (w1 + (i-y1) * dw2_step);

                if (ax > bx) {
                    int   tempX = ax;ax = bx;bx = tempX;
                    float tempU = tex_su; tex_su = tex_eu; tex_eu = tempU;
                    float tempV = tex_sv; tex_sv = tex_ev; tex_ev = tempV;
                    float tempW = tex_sw; tex_sw = tex_ew; tex_ew = tempW;
                }

                tex_u = tex_su;
                tex_v = tex_sv;
                tex_w = tex_sw;

                float tstep = 1 / (float) (Math.max(1,bx-ax));
                float t = 0;

                for (int j = ax; j <= bx; j++) {
                    tex_u = (1 - t) * tex_su  + t * tex_eu;
                    tex_v = (1 - t) * tex_sv  + t * tex_ev;
                    tex_w = (1 - t) * tex_sw + t * tex_ew;

                    int pixelColor = texture.getPixelRGB(tex_u/tex_w, tex_v/tex_w);
//                    frameBuffer.setRGB(j, i, pixelColor);
                    if (depthBuffer[i*winWidth + j] < tex_w) {
                        g.setColor(new Color(pixelColor, true));
                        g.fillRect(j, i, 1, 1);
                        depthBuffer[i * winWidth + j] = tex_w;
                    }

                    t += tstep;
                }
            }
        }

        //2nd part of the triangle
        dy1 = y3 - y2;
        dx1 = x3 - x2;
        dv1 = v3 - v2;
        du1 = u3 - u2;
        dw1 = w3 - w2;

        if (dy1 != 0) dax_step = dx1 / (float) abs(dy1);
        if (dy2 != 0) dbx_step = dx2 / (float) abs(dy2);

        du1_step = 0; dv1_step = 0; dw1_step = 0;
        if (dy1 != 0) du1_step = du1 / (float) abs(dy1);
        if (dy1 != 0) dv1_step = dv1 / (float) abs(dy1);
        if (dy1 != 0) dw1_step = dw1 / (float)abs(dy1);

        if (dy1 != 0) {
            for (int i = y2; i <= y3; i++) {
                int ax = (int) (x2 + (i-y2) * dax_step);
                int bx = (int) (x1 + (i-y1) * dbx_step);

                float tex_su = (u2 + (i-y2) * du1_step);
                float tex_sv = (v2 + (i-y2) * dv1_step);
                float tex_sw = w2 + (float)(i - y2) * dw1_step;

                float tex_eu = (u1 + (i-y1) * du2_step);
                float tex_ev = (v1 + (i-y1) * dv2_step);
                float tex_ew = w1 + (float)(i - y1) * dw2_step;

                if (ax > bx) {
                    int   tempX = ax;ax = bx;bx = tempX;
                    float tempU = tex_su; tex_su = tex_eu; tex_eu = tempU;
                    float tempV = tex_sv; tex_sv = tex_ev; tex_ev = tempV;
                    float tempW = tex_sw; tex_sw = tex_ew; tex_ew = tempW;
                }

                tex_u = tex_su;
                tex_v = tex_sv;
                tex_w = tex_sw;

                float tstep = 1 / (float) (Math.max(1,bx-ax));
                float t = 0;

                for (int j = ax; j <= bx; j++) {
                    tex_u = (1 - t) * tex_su  + t * tex_eu;
                    tex_v = (1 - t) * tex_sv  + t * tex_ev;
                    tex_w = (1 - t) * tex_sw + t * tex_ew;

                    int pixelColor = texture.getPixelRGB(tex_u/tex_w, tex_v/tex_w);
//                    frameBuffer.setRGB(j, i, pixelColor);
                    if (depthBuffer[i*winWidth + j] < tex_w) {
                        g.setColor(new Color(pixelColor, true));
                        g.fillRect(j, i, 1, 1);
                        depthBuffer[i * winWidth + j] = tex_w;
                    }

                    t += tstep;
                }
            }
        }
    }

    public Triangle convertToWindowSpace(int iWinWidth, int iWinHeight) {
        // X/Y Inverted so need to put them back???
        this.scaleInPlaceX(-1);
        this.scaleInPlaceY(-1);

        // Offset into visible normalized space
        Vector3D vOffsetView = new Vector3D(1,1,0);
        this.translateInPlace(vOffsetView);

        // Scaling to screen dimension
        this.scaleInPlaceX(0.5 * iWinHeight);
        this.scaleInPlaceY(0.5 * iWinWidth);

        return this;
    }


    public Triangle projectToScreenInPlace(Matrix projectionMatrix, int iWinWidth, int iWinHeight) {
        this.transformVertexInPlace(projectionMatrix);
        this.homogeneousDivisionInPlace();
        this.convertToWindowSpace(iWinWidth, iWinHeight);

        return this;
    }

    public boolean isFacing(Camera camera, boolean isFlipped) {
        // Casting the ray of the camera
        Vector3D vCameraRay = this.getVertices()[0].sub(camera.getCameraPosition());

        // Checking if the ray of the camera is in sight of the normale
        if (isFlipped) {
            return (this.getNormal().dotProduct(vCameraRay) > 0);
        } else {
            return (this.getNormal().dotProduct(vCameraRay) < 0);
        }
    }

    public void updateNormal() {
        Vector3D edge1 = vertices[1].sub(vertices[0]);
        Vector3D edge2 = vertices[2].sub(vertices[0]);
        normal = edge1.crossProduct(edge2).normalizeInPlace();
    }

    public void grayScale(double t) {
        // Clamp t to [0, 1] in case the caller passes invalid values
        t = Math.max(0, Math.min(1, t));

        int v = (int) (t * 255);   // convert 0–1 to 0–255
        setColor(new Color(v, v, v));
    }

    public void setLighting(Vector3D lightDirection, boolean isFlipped) {
        lightDirection.normalizeInPlace();

        double dpLightNorm;
        if (isFlipped) {
            dpLightNorm = this.getNormal().scaleInPlace(-1).dotProduct(lightDirection);
        } else {
            dpLightNorm = this.getNormal().dotProduct(lightDirection);
        }
        this.grayScale(dpLightNorm);
    }

    public Triangle VertexTransformed(Matrix matTransform) {
        Vertex3D[] vertsTriIn = this.getVertices();
        Vertex3D[] vertsTriTransformed = new Vertex3D[3];

        for (int i = 0; i < 3; i++) {
            vertsTriTransformed[i] = vertsTriIn[i].transformed(matTransform);
        }

        return new Triangle(vertsTriTransformed, this.getTextVertices(),this.getColor());
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

    @Override
    public String toString() {
        return "engine.math.geometry.Triangle{" +
                "vertices=" + Arrays.toString(vertices) +
                '}';
    }
}
