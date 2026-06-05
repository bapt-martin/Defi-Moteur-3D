package graphicEngine.math.geometry;

import graphicEngine.renderer.Camera;
import graphicEngine.math.tools.Matrix;
import graphicEngine.math.tools.Vector3D;
import graphicEngine.renderer.Texture;
import graphicEngine.scene.lightRelative.PointLight;
import graphicEngine.scene.lightRelative.SpotLight;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

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


//    public void drawTexturedTriangle(int[] pixels, Texture texture, int winWidth, float[] depthBuffer) {
//
//        Vertex3D[] verts = this.getVertices();
//        Vertex2D[] uvs = this.getTextVertices();
//
//        int x1 = (int) verts[0].x;
//        int y1 = (int) verts[0].y;
//
//        int x2 = (int) verts[1].x;
//        int y2 = (int) verts[1].y;
//
//        int x3 = (int) verts[2].x;
//        int y3 = (int) verts[2].y;
//
//        float u1 = (float) uvs[0].u;
//        float v1 = (float) uvs[0].v;
//        float w1 = (float) uvs[0].w;
//
//        float u2 = (float) uvs[1].u;
//        float v2 = (float) uvs[1].v;
//        float w2 = (float) uvs[1].w;
//
//        float u3 = (float) uvs[2].u;
//        float v3 = (float) uvs[2].v;
//        float w3 = (float) uvs[2].w;
//
//        if (y1 > y2) {
//            int tempX = x1; x1 = x2; x2 = tempX;
//            int tempY = y1; y1 = y2; y2 = tempY;
//            float tempU = u1; u1 = u2; u2 = tempU;
//            float tempV = v1; v1 = v2; v2 = tempV;
//            float tempW = w1; w1 = w2; w2 = tempW;
//        }
//
//        if (y1 > y3) {
//            int tempX = x1; x1 = x3; x3 = tempX;
//            int tempY = y1; y1 = y3; y3 = tempY;
//            float tempU = u1; u1 = u3; u3 = tempU;
//            float tempV = v1; v1 = v3; v3 = tempV;
//            float tempW = w1; w1 = w3; w3 = tempW;
//        }
//
//        if (y2 > y3) {
//            int tempX = x2; x2 = x3; x3 = tempX;
//            int tempY = y2; y2 = y3; y3 = tempY;
//            float tempU = u2; u2 = u3; u3 = tempU;
//            float tempV = v2; v2 = v3; v3 = tempV;
//            float tempW = w2; w2 = w3; w3 = tempW;
//        }
//
//        int   dy1 = y2 - y1;
//        int   dx1 = x2 - x1;
//        float dv1 = v2 - v1;
//        float du1 = u2 - u1;
//        float dw1 = w2 - w1;
//
//
//        int   dy2 = y3 - y1;
//        int   dx2 = x3 - x1;
//        float dv2 = v3 - v1;
//        float du2 = u3 - u1;
//        float dw2 = w3 - w1;
//
//        float tex_u; float tex_v; float tex_w;
//
//        float dax_step = 0; float dbx_step = 0;
//        float du1_step = 0; float du2_step = 0;
//        float dv1_step = 0; float dv2_step = 0;
//        float dw1_step = 0; float dw2_step = 0;
//
//        if (dy1 != 0) dax_step = dx1 / (float) abs(dy1);
//        if (dy2 != 0) dbx_step = dx2 / (float) abs(dy2);
//
//        if (dy1 != 0) du1_step = du1 / (float) abs(dy1);
//        if (dy1 != 0) dv1_step = dv1 / (float) abs(dy1);
//        if (dy1 != 0) dw1_step = dw1 / (float) abs(dy1);
//
//        if (dy2 != 0) du2_step = du2 / (float) abs(dy2);
//        if (dy2 != 0) dv2_step = dv2 / (float) abs(dy2);
//        if (dy2 != 0) dw2_step = dw2 / (float) abs(dy2);
//
//        if (dy1 != 0) {
//            for (int i = y1; i <= y2; i++) {
//                int ax = (int) (x1 + (i-y1) * dax_step);
//                int bx = (int) (x1 + (i-y1) * dbx_step);
//
//                float tex_su = (u1 + (i-y1) * du1_step);
//                float tex_sv = (v1 + (i-y1) * dv1_step);
//                float tex_sw = (w1 + (i-y1) * dw1_step);
//
//                float tex_eu = (u1 + (i-y1) * du2_step);
//                float tex_ev = (v1 + (i-y1) * dv2_step);
//                float tex_ew = (w1 + (i-y1) * dw2_step);
//
//                if (ax > bx) {
//                    int   tempX = ax;ax = bx;bx = tempX;
//                    float tempU = tex_su; tex_su = tex_eu; tex_eu = tempU;
//                    float tempV = tex_sv; tex_sv = tex_ev; tex_ev = tempV;
//                    float tempW = tex_sw; tex_sw = tex_ew; tex_ew = tempW;
//                }
//
//                tex_u = tex_su;
//                tex_v = tex_sv;
//                tex_w = tex_sw;
//
//                float tstep = 1 / (float) (Math.max(1,bx-ax));
//                float t = 0;
//
//                for (int j = ax; j <= bx; j++) {
//                    tex_u = (1 - t) * tex_su  + t * tex_eu;
//                    tex_v = (1 - t) * tex_sv  + t * tex_ev;
//                    tex_w = (1 - t) * tex_sw + t * tex_ew;
//
//                    int pixelColor = texture.getPixelRGB(tex_u/tex_w, tex_v/tex_w);
//                    int finalColor = multiplyColors(pixelColor, this.color);
//                    int index = i * winWidth + j;
//                    if (tex_w > depthBuffer[index]) {
//                        pixels[index] = finalColor;
//                        depthBuffer[index] = tex_w;
//                    }
//                    t += tstep;
//                }
//            }
//        }
//        //2nd part of the triangle
//        dy1 = y3 - y2;
//        dx1 = x3 - x2;
//        dv1 = v3 - v2;
//        du1 = u3 - u2;
//        dw1 = w3 - w2;
//
//        if (dy1 != 0) dax_step = dx1 / (float) abs(dy1);
//        if (dy2 != 0) dbx_step = dx2 / (float) abs(dy2);
//
//        du1_step = 0; dv1_step = 0; dw1_step = 0;
//        if (dy1 != 0) du1_step = du1 / (float) abs(dy1);
//        if (dy1 != 0) dv1_step = dv1 / (float) abs(dy1);
//        if (dy1 != 0) dw1_step = dw1 / (float)abs(dy1);
//
//        if (dy1 != 0) {
//            for (int i = y2; i <= y3; i++) {
//                int ax = (int) (x2 + (i-y2) * dax_step);
//                int bx = (int) (x1 + (i-y1) * dbx_step);
//
//                float tex_su = (u2 + (i-y2) * du1_step);
//                float tex_sv = (v2 + (i-y2) * dv1_step);
//                float tex_sw = w2 + (float)(i - y2) * dw1_step;
//
//                float tex_eu = (u1 + (i-y1) * du2_step);
//                float tex_ev = (v1 + (i-y1) * dv2_step);
//                float tex_ew = w1 + (float)(i - y1) * dw2_step;
//
//                if (ax > bx) {
//                    int   tempX = ax;ax = bx;bx = tempX;
//                    float tempU = tex_su; tex_su = tex_eu; tex_eu = tempU;
//                    float tempV = tex_sv; tex_sv = tex_ev; tex_ev = tempV;
//                    float tempW = tex_sw; tex_sw = tex_ew; tex_ew = tempW;
//                }
//
//                float tstep = 1 / (float) (Math.max(1,bx-ax));
//                float t = 0;
//
//                for (int j = ax; j <= bx; j++) {
//                    tex_u = (1 - t) * tex_su  + t * tex_eu;
//                    tex_v = (1 - t) * tex_sv  + t * tex_ev;
//                    tex_w = (1 - t) * tex_sw + t * tex_ew;
//
//                    int pixelColor = texture.getPixelRGB(tex_u/tex_w, tex_v/tex_w);
//                    int finalColor = multiplyColors(pixelColor, this.color);
//                    int index = i * winWidth + j;
//                    if (tex_w > depthBuffer[index]) {
//                        pixels[index] = finalColor;
//                        depthBuffer[index] = tex_w;
//                    }
//                    t += tstep;
//                }
//            }
//        }
//    }

//    public void drawTexturedTriangle(int[] pixels, Texture texture, int winWidth, float[] depthBuffer) {
//        Vertex3D[] verts = this.getVertices();
//        Vertex2D[] uvs = this.getTextVertices();
//
//        int x1 = (int) verts[0].x; int y1 = (int) verts[0].y;
//        int x2 = (int) verts[1].x; int y2 = (int) verts[1].y;
//        int x3 = (int) verts[2].x; int y3 = (int) verts[2].y;
//
//        float u1 = (float) uvs[0].u; float v1 = (float) uvs[0].v; float w1 = (float) uvs[0].w;
//        float u2 = (float) uvs[1].u; float v2 = (float) uvs[1].v; float w2 = (float) uvs[1].w;
//        float u3 = (float) uvs[2].u; float v3 = (float) uvs[2].v; float w3 = (float) uvs[2].w;
//
//        if (y1 > y2) { int tX=x1;x1=x2;x2=tX; int tY=y1;y1=y2;y2=tY; float tU=u1;u1=u2;u2=tU; float tV=v1;v1=v2;v2=tV; float tW=w1;w1=w2;w2=tW; }
//        if (y1 > y3) { int tX=x1;x1=x3;x3=tX; int tY=y1;y1=y3;y3=tY; float tU=u1;u1=u3;u3=tU; float tV=v1;v1=v3;v3=tV; float tW=w1;w1=w3;w3=tW; }
//        if (y2 > y3) { int tX=x2;x2=x3;x3=tX; int tY=y2;y2=y3;y3=tY; float tU=u2;u2=u3;u3=tU; float tV=v2;v2=v3;v3=tV; float tW=w2;w2=w3;w3=tW; }
//
//        int dy1 = y2 - y1, dx1 = x2 - x1;
//        int dy2 = y3 - y1, dx2 = x3 - x1;
//        float du1 = u2 - u1, dv1 = v2 - v1, dw1 = w2 - w1;
//        float du2 = u3 - u1, dv2 = v3 - v1, dw2 = w3 - w1;
//
//        float dax_step = 0, dbx_step = 0;
//        float du1_step = 0, dv1_step = 0, dw1_step = 0;
//        float du2_step = 0, dv2_step = 0, dw2_step = 0;
//
//        if (dy1 != 0) { dax_step = dx1 / (float) Math.abs(dy1); du1_step = du1 / Math.abs(dy1); dv1_step = dv1 / Math.abs(dy1); dw1_step = dw1 / Math.abs(dy1); }
//        if (dy2 != 0) { dbx_step = dx2 / (float) Math.abs(dy2); du2_step = du2 / Math.abs(dy2); dv2_step = dv2 / Math.abs(dy2); dw2_step = dw2 / Math.abs(dy2); }
//
//        //First triangle part
//        if (dy1 != 0) {
//            for (int i = y1; i <= y2; i++) {
//                int ax = (int) (x1 + (i - y1) * dax_step);
//                int bx = (int) (x1 + (i - y1) * dbx_step);
//                float tex_su = u1 + (i - y1) * du1_step; float tex_sv = v1 + (i - y1) * dv1_step; float tex_sw = w1 + (i - y1) * dw1_step;
//                float tex_eu = u1 + (i - y1) * du2_step; float tex_ev = v1 + (i - y1) * dv2_step; float tex_ew = w1 + (i - y1) * dw2_step;
//
//                drawScanline(i, ax, bx, tex_su, tex_eu, tex_sv, tex_ev, tex_sw, tex_ew, texture, pixels, winWidth, depthBuffer);
//            }
//        }
//
//        //Second triangle part
//        dy1 = y3 - y2; dx1 = x3 - x2;
//        du1 = u3 - u2; dv1 = v3 - v2; dw1 = w3 - w2;
//
//        if (dy1 != 0) { dax_step = dx1 / (float) Math.abs(dy1); du1_step = du1 / Math.abs(dy1); dv1_step = dv1 / Math.abs(dy1); dw1_step = dw1 / Math.abs(dy1); }
//
//        if (dy1 != 0) {
//            for (int i = y2; i <= y3; i++) {
//                int ax = (int) (x2 + (i - y2) * dax_step);
//                int bx = (int) (x1 + (i - y1) * dbx_step);
//                float tex_su = u2 + (i - y2) * du1_step; float tex_sv = v2 + (i - y2) * dv1_step; float tex_sw = w2 + (i - y2) * dw1_step;
//                float tex_eu = u1 + (i - y1) * du2_step; float tex_ev = v1 + (i - y1) * dv2_step; float tex_ew = w1 + (i - y1) * dw2_step;
//
//                drawScanline(i, ax, bx, tex_su, tex_eu, tex_sv, tex_ev, tex_sw, tex_ew, texture, pixels, winWidth, depthBuffer);
//            }
//        }
//    }

    public void drawTexturedTriangle(int[] pixels, Texture texture, int winWidth, float[] depthBuffer) {
        Vertex3D[] verts = this.getVertices();
        Vertex2D[] uvs = this.getTextVertices();

        int x1 = (int) verts[0].x, y1 = (int) verts[0].y; float u1 = (float) uvs[0].u, v1 = (float) uvs[0].v, w1 = (float) uvs[0].w;
        int x2 = (int) verts[1].x, y2 = (int) verts[1].y; float u2 = (float) uvs[1].u, v2 = (float) uvs[1].v, w2 = (float) uvs[1].w;
        int x3 = (int) verts[2].x, y3 = (int) verts[2].y; float u3 = (float) uvs[2].u, v3 = (float) uvs[2].v, w3 = (float) uvs[2].w;

        if (y1 > y2) { int tX=x1;x1=x2;x2=tX; int tY=y1;y1=y2;y2=tY; float tU=u1;u1=u2;u2=tU; float tV=v1;v1=v2;v2=tV; float tW=w1;w1=w2;w2=tW; }
        if (y1 > y3) { int tX=x1;x1=x3;x3=tX; int tY=y1;y1=y3;y3=tY; float tU=u1;u1=u3;u3=tU; float tV=v1;v1=v3;v3=tV; float tW=w1;w1=w3;w3=tW; }
        if (y2 > y3) { int tX=x2;x2=x3;x3=tX; int tY=y2;y2=y3;y3=tY; float tU=u2;u2=u3;u3=tU; float tV=v2;v2=v3;v3=tV; float tW=w2;w2=w3;w3=tW; }

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

            int bx = isSecondHalf ? x2 + (int)((x3 - x2) * beta) : x1 + (int)((x2 - x1) * beta);
            float tex_bu = isSecondHalf ? u2 + (u3 - u2) * beta : u1 + (u2 - u1) * beta;
            float tex_bv = isSecondHalf ? v2 + (v3 - v2) * beta : v1 + (v2 - v1) * beta;
            float tex_bw = isSecondHalf ? w2 + (w3 - w2) * beta : w1 + (w2 - w1) * beta;

            drawScanline(y, ax, bx, tex_au, tex_bu, tex_av, tex_bv, tex_aw, tex_bw, texture, pixels, winWidth, depthBuffer);
        }
    }

    private void drawScanline(int y, int ax, int bx, float su, float eu, float sv, float ev, float sw, float ew, Texture texture, int[] pixels, int winWidth, float[] depthBuffer) {
        if (ax > bx) {
            int tempX = ax; ax = bx; bx = tempX;
            float tempU = su; su = eu; eu = tempU;
            float tempV = sv; sv = ev; ev = tempV;
            float tempW = sw; sw = ew; ew = tempW;
        }

        float tstep = 1.0f / (float) Math.max(1, bx - ax);
        float t = 0;

        for (int j = ax; j <= bx; j++) {
            float tex_u = (1 - t) * su + t * eu;
            float tex_v = (1 - t) * sv + t * ev;
            float tex_w = (1 - t) * sw + t * ew;

            int pixelColor = texture.getPixelRGB(tex_u / tex_w, tex_v / tex_w);
            int finalColor = multiplyColors(pixelColor, this.color);
            int index = y * winWidth + j;

            if (j >= 0 && j < winWidth && index >= 0 && index < pixels.length) {
                if (tex_w > depthBuffer[index]) {
                    pixels[index] = finalColor;
                    depthBuffer[index] = tex_w;
                }
            }
            t += tstep;
        }
    }

    private int multiplyColors(int texColor, Color triColor) {
        int r = (texColor >> 16) & 0xFF;
        int g = (texColor >> 8) & 0xFF;
        int b = texColor & 0xFF;

        r = (int)(r * (triColor.getRed() / 255.0f));
        g = (int)(g * (triColor.getGreen() / 255.0f));
        b = (int)(b * (triColor.getBlue() / 255.0f));

        return (r << 16) | (g << 8) | b;
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

//    public void grayScale(double t) {
//        // Clamp t to [0, 1] in case the caller passes invalid values
//        t = Math.max(0, Math.min(1, t));
//
//        int v = (int) (t * 255);   // convert 0–1 to 0–255
//        setColor(new Color(v, v, v));
//    }

//    public void setLighting(Vector3D lightDirection, boolean isFlipped) {
//        lightDirection.normalizeInPlace();
//
//        double dpLightNorm;
//        if (isFlipped) {
//            dpLightNorm = this.getNormal().scaleInPlace(-1).dotProduct(lightDirection);
//        } else {
//            dpLightNorm = this.getNormal().dotProduct(lightDirection);
//        }
//        this.grayScale(dpLightNorm);
//    }

    public Vector3D getCenter() {
        Vertex3D[] v = this.getVertices();
        double cx = (v[0].x + v[1].x + v[2].x) / 3.0;
        double cy = (v[0].y + v[1].y + v[2].y) / 3.0;
        double cz = (v[0].z + v[1].z + v[2].z) / 3.0;
        return new Vector3D(cx, cy, cz);
    }

    public void setLighting(List<PointLight> lightQueue, boolean isFlipped) {
        double totalR = 0.15;
        double totalG = 0.15;
        double totalB = 0.15;

        Vector3D triCenter = this.getCenter();
        Vector3D triNormal = this.getNormal();
        if (isFlipped) triNormal.scaleInPlace(-1);

        for (PointLight light : lightQueue) {
            if (!light.isOn()) continue;

            Vector3D lightVector = light.getPosition().sub(triCenter);
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

            double dp = triNormal.dotProduct(lightVector);
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

        int finalRed = (int) (this.color.getRed() * totalR);
        int finalGreen = (int) (this.color.getGreen() * totalG);
        int finalBlue = (int) (this.color.getBlue() * totalB);

        this.color = new Color(finalRed, finalGreen, finalBlue);
    }


//    public void setLighting(List<PointLight> lightQueue, boolean isFlipped) {
//        double totalIntensity = 0.15;
//
//        Vector3D triCenter = this.getCenter();
//        Vector3D triNormal = this.getNormal();
//        if (isFlipped) triNormal.scaleInPlace(-1);
//
//        for (PointLight light : lightQueue) {
//            if (!light.isOn()) continue;
//
//            Vector3D lightVector = light.getPosition().sub(triCenter);
//            double distance = lightVector.getLength();
//            lightVector.normalizeInPlace();
//
//            if (light instanceof SpotLight) {
//                SpotLight spot = (SpotLight) light;
//
//                double spotFactor = -lightVector.dotProduct(spot.getDirection());
//
//                if (spotFactor < spot.getCutOffAngle()) {
//                    continue;
//                }
//            }
//
//            double dp = triNormal.dotProduct(lightVector);
//            if (dp > 0) {
//                double attenuation = 1.0 / (1.0 + light.getFallOff() * distance);
//                totalIntensity += (dp * attenuation);
//            }
//        }
//
//        totalIntensity = Math.min(1.0, totalIntensity);
//
//        int r = (int) (this.color.getRed() * totalIntensity);
//        int g = (int) (this.color.getGreen() * totalIntensity);
//        int b = (int) (this.color.getBlue() * totalIntensity);
//
//        this.setColor(new Color(r, g, b));
//    }


//    public void setLighting(Vector3D lightDirection, boolean isFlipped) {
//        lightDirection.normalizeInPlace();
//
//        double dpLightNorm;
//        if (isFlipped) {
//            dpLightNorm = this.getNormal().scaleInPlace(-1).dotProduct(lightDirection);
//        } else {
//            dpLightNorm = this.getNormal().dotProduct(lightDirection);
//        }
//
//        double intensity = Math.max(0.15, Math.min(1.0, dpLightNorm));
//
//        int r = (int) (this.color.getRed() * intensity);
//        int g = (int) (this.color.getGreen() * intensity);
//        int b = (int) (this.color.getBlue() * intensity);
//
//        this.setColor(new Color(r, g, b));
//    }

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
