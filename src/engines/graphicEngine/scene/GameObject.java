package engines.graphicEngine.scene;

import engines.graphicEngine.math.geometry.Mesh;
import engines.graphicEngine.math.geometry.Triangle;
import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Texture;

import java.awt.*;
import java.util.List;

public class GameObject {
    private Mesh mesh;
    private Triangle[] poolTriangle;
    private Texture texture = Texture.WHITE_PIXEL;
    private Color basedColor = Color.WHITE;
    private Matrix worldTransformMatrix;
    private String name = "NaN";
    private int id;

    private Vector3D scale;
    private Vector3D rotation;
    private Vector3D position;

    private boolean isDirty = true;
    private boolean isRendered = true;

    public GameObject(Mesh mesh) {
        this.mesh = mesh;
        this.scale    = new Vector3D(1,1,1);
        this.rotation = new Vector3D();
        this.position = new Vector3D();
        this.creatWorldTransformMatrix();
        this.createTrianglePool();
    }

    public GameObject(Mesh mesh, Texture texture) {
        this.mesh = mesh;
        this.texture = texture;
        this.scale    = new Vector3D(1,1,1);
        this.rotation = new Vector3D();
        this.position = new Vector3D();
        this.creatWorldTransformMatrix();
        this.createTrianglePool();
    }

    public GameObject(Mesh mesh, Color color) {
        this.mesh = mesh;
        this.basedColor = color;
        this.scale    = new Vector3D(1,1,1);
        this.rotation = new Vector3D();
        this.position = new Vector3D();
        this.creatWorldTransformMatrix();
        this.createTrianglePool();
    }

    public void createTrianglePool() {
        List<Triangle> meshTriangle = this.mesh.getMeshTriangle();
        int size = meshTriangle.size();
        Triangle[] trianglePool = new Triangle[size];

        Matrix parentWorldTransformMatrix = this.getWorldTransformMatrix();

        for (int i=0; i<size; i++) {
            trianglePool[i] = meshTriangle.get(i).deepClone();
            trianglePool[i].setTexture(texture);
            trianglePool[i].setColor(basedColor);
            trianglePool[i].setParentTriangle(meshTriangle.get(i));
            trianglePool[i].setParentWorldTransformMatrix(parentWorldTransformMatrix);
        }

//        System.out.println(trianglePool[0].getTexture().getTextureName());

        this.poolTriangle = trianglePool;
    }

    public void updateTrianglePool(Matrix worldTransformMatrix, Color baseColor, Texture texture) {
        List<Triangle> meshTriangle = this.mesh.getMeshTriangle();
        int size = meshTriangle.size();
        for (int i = 0; i < size; i++) {

            Triangle originalTri = meshTriangle.get(i);

            Triangle pooledTri = this.poolTriangle[i];

            originalTri.transformInPool(worldTransformMatrix, pooledTri);
        }
    }

    public void creatWorldTransformMatrix() {
        this.worldTransformMatrix = Matrix.createWorldTransformMatrix(
                scale.getX(),    scale.getY(),    scale.getZ(),
                rotation.getX(), rotation.getY(), rotation.getZ(),
                position.getX(), position.getY(), position.getZ()
        );
    }

    public void updateWorldTransformMatrix() {
        this.worldTransformMatrix.updateWorldTransformMatrix(
                scale.getX(),    scale.getY(),    scale.getZ(),
                rotation.getX(), rotation.getY(), rotation.getZ(),
                position.getX(), position.getY(), position.getZ()
        );
    }

    public void setScale(double x, double y, double z) {
        this.scale.setX(x);
        this.scale.setY(y);
        this.scale.setZ(z);
        this.isDirty = true;
    }

    public void setPosition(double x, double y, double z) {
        this.position.setX(x);
        this.position.setY(y);
        this.position.setZ(z);
        this.isDirty = true;
    }

    public void setRotation(double x, double y, double z) {
        this.rotation.setX(x);
        this.rotation.setY(y);
        this.rotation.setZ(z);
        this.isDirty = true;
    }

    public void move(double dx, double dy, double dz) {
        this.position.setX(this.position.getX() + dx);
        this.position.setY(this.position.getY() + dy);
        this.position.setZ(this.position.getZ() + dz);
        this.isDirty = true;
    }

    public void rotate(double rx, double ry, double rz) {
        this.rotation.setX(this.rotation.getX() + rx);
        this.rotation.setY(this.rotation.getY() + ry);
        this.rotation.setZ(this.rotation.getZ() + rz);
        this.isDirty = true;
    }

    public void scale(double sx, double sy, double sz) {
        this.scale.setX(this.scale.getX() * sx);
        this.scale.setY(this.scale.getY() * sy);
        this.scale.setZ(this.scale.getZ() * sz);
        this.isDirty = true;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Matrix getWorldTransformMatrix() {
        if (isDirty) {
            updateWorldTransformMatrix();
            isDirty = false;
        }
        return worldTransformMatrix;
    }

    public void setWorldTransformMatrix(Matrix worldTransformMatrix) {
        this.worldTransformMatrix = worldTransformMatrix;
        isDirty = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRendered() {
        return isRendered;
    }

    public void setRendered(boolean rendered) {
        isRendered = rendered;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Color getBasedColor() {
        return basedColor;
    }

    public void setBasedColor(Color basedColor) {
        this.basedColor = basedColor;
    }

    public Vector3D getScale() {
        return scale;
    }

    public Vector3D getRotation() {
        return rotation;
    }

    public Vector3D getPosition() {
        return position;
    }

    public Triangle[] getPoolTriangle() {
        return poolTriangle;
    }
}
