package graphicEngine.math.geometry;

import graphicEngine.renderer.Camera;
import graphicEngine.math.tools.Matrix;
import graphicEngine.math.tools.Vector3D;

import java.awt.*;
import java.util.Arrays;

import static java.awt.Color.*;

public class Triangle {
    private Vertex3D[] vertices = new Vertex3D[3];
    private Vertex2D[] textVertices = new Vertex2D[3];
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

    public Triangle(Vertex3D p1, Vertex3D p2, Vertex3D p3, Vertex2D t1, Vertex2D t2, Vertex2D t3, Color color) {
        vertices[0] = new Vertex3D(p1);
        vertices[1] = new Vertex3D(p2);
        vertices[2] = new Vertex3D(p3);

        textVertices[0] = new Vertex2D(t1);
        textVertices[1] = new Vertex2D(t2);
        textVertices[2] = new Vertex2D(t3);

        this.color = color;
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
        this(verts[0], verts[1], verts[2],textVerts[0], textVerts[1], textVerts[2], color);
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

        vertsIn[0].divideInPlace(vertsIn[0].getW());
        vertsIn[1].divideInPlace(vertsIn[1].getW());
        vertsIn[2].divideInPlace(vertsIn[2].getW());

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

    public void drawTriangle(Graphics g, boolean isOutlineRender) {
        // Getting back the coordinate to draw the 2D triangle
        this.get2DCoordinates(xs, ys);

        g.setColor(this.getColor()); // Setting the correct color
        g.fillPolygon(xs, ys, 3);

        if (isOutlineRender) {
            g.setColor(Color.BLACK); // Drawing the outline
            g.drawPolygon(xs, ys, 3);
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

    @Override
    public String toString() {
        return "engine.math.geometry.Triangle{" +
                "vertices=" + Arrays.toString(vertices) +
                '}';
    }
}
