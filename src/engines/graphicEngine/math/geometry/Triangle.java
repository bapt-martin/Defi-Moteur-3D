package engines.graphicEngine.math.geometry;

import engines.graphicEngine.renderer.Camera;
import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Texture;

import java.awt.*;
import java.util.Arrays;

public class Triangle {
    private final Vertex3D[] vertices = new Vertex3D[3];

    private final Vertex2D[] textVertices = new Vertex2D[3];
    private Texture texture;

    private Vector3D[] normalsVertices = new Vector3D[3];
    private Vector3D normal = null;

    private Color[] lightIntensities = new Color[3];
    private Color color;

    private Matrix parentWorldTransformMatrix;
    private Triangle parentTriangle = null;

    private final int[] xs = new int[3];
    private final int[] ys = new int[3];


    public Triangle(Vertex3D[] verts, Vertex2D[] textVerts, Vector3D[] normals, Color[] lights, Color color, Texture texture) {
        this.color = (color != null) ? color : Color.WHITE;
        this.texture = (texture != null) ? texture : Texture.WHITE_PIXEL;

        for (int i = 0; i < 3; i++) {
            this.vertices[i] =  verts[i];
            this.textVertices[i] = textVerts[i];
            this.normalsVertices[i] = normals[i];
            this.lightIntensities[i] = (lights == null) ? color : lights[i];
        }
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

    public void transformInPool(Matrix matTransform, Triangle poolTriangle) {
        Vertex3D[] vertsTriIn = this.getVertices();
        Vertex2D[] textsVertsTriIn = this.getTextVertices();
        Vector3D[] normalsTriIn = this.getNormalsVertices();

        Vertex3D[] vertsTriOut = poolTriangle.getVertices();
        Vertex2D[] textsVertsTriOut = poolTriangle.getTextVertices();
        Vector3D[] normalsTriOut = poolTriangle.getNormalsVertices();

        double[][] m = matTransform.getMatrix();
        for (int i = 0; i < 3; i++) {
            vertsTriIn[i].transformAndStoreIn(m, vertsTriOut[i]);

            textsVertsTriOut[i].u = textsVertsTriIn[i].u;
            textsVertsTriOut[i].v = textsVertsTriIn[i].v;
            textsVertsTriOut[i].w = textsVertsTriIn[i].w;

            normalsTriIn[i].transformAndStoreIn(m, normalsTriOut[i]);
        }
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

    public Triangle getParentTriangle() {
        return parentTriangle;
    }

    public void setParentTriangle(Triangle parentTriangle) {
        this.parentTriangle = parentTriangle;
    }

    @Override
    public String toString() {
        return "engine.math.geometry.Triangle{" +
                "vertices=" + Arrays.toString(vertices) +
                '}';
    }
}
