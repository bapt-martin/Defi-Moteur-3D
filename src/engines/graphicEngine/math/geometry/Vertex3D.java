package engines.graphicEngine.math.geometry;

import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Tuple3D;
import engines.graphicEngine.math.tools.Vector3D;

import java.awt.*;

public class Vertex3D extends Tuple3D {
    public Vertex3D() {
        super(0,0,0,1);
    }

    public Vertex3D(double x, double y, double z) {
        super(x,y,z,1);
    }

    public Vertex3D(Tuple3D tuple3D) {
        super(tuple3D);
    }

    public Vertex3D(Point point) {
        super(point.x,point.y,0,1);
    }

    @Override
    public Vertex3D crossProduct(Tuple3D other) {
        return new Vertex3D(super.crossProduct(other));
    }

    @Override
    public Vertex3D scaled(double factor) {
        return new Vertex3D(super.scaled(factor));
    }

    @Override
    public Vertex3D divideInPlace(double divisor) {
        super.divideInPlace(divisor);
        return this;
    }

    @Override
    public Vertex3D scaleInPlace(double factor) {
        super.scaleInPlace(factor);
        return this;
    }

    @Override
    public Vertex3D divided(double divisor) {
        return new Vertex3D(super.divided(divisor));
    }

    public Vertex3D translated(Vector3D vectTranslation) {
        double x = this.getX() + vectTranslation.getX();
        double y = this.getY() + vectTranslation.getY();
        double z = this.getZ() + vectTranslation.getZ();

        return new Vertex3D(x,y,z);
    }

    public Vertex3D translateInPlace(Vector3D vectTranslation) {
        this.setX(this.getX() + vectTranslation.getX());
        this.setY(this.getY() + vectTranslation.getY());
        this.setZ(this.getZ() + vectTranslation.getZ());

        return this;
    }

    @Override
    public Vertex3D add(Tuple3D other) {
        return new Vertex3D(super.add(other));
    }

    @Override
    public Vector3D sub(Tuple3D other) {
        return new Vector3D(super.sub(other));
    }

    @Override
    public Vertex3D transformed(Matrix matIn) {
        return new Vertex3D(super.transformed(matIn));
    }

    public Vertex3D transformTuple3DInPlace(Matrix mat) {
        super.transformInPlace(mat);
        return this;
    }

    public void transformAndStoreIn(double[][] m, Vertex3D destinationVertex) {

        double newX = this.x * m[0][0] + this.y * m[1][0] + this.z * m[2][0] + this.w * m[3][0];
        double newY = this.x * m[0][1] + this.y * m[1][1] + this.z * m[2][1] + this.w * m[3][1];
        double newZ = this.x * m[0][2] + this.y * m[1][2] + this.z * m[2][2] + this.w * m[3][2];
        double newW = this.x * m[0][3] + this.y * m[1][3] + this.z * m[2][3] + this.w * m[3][3];

        destinationVertex.x = newX;
        destinationVertex.y = newY;
        destinationVertex.z = newZ;
        destinationVertex.w = newW;
    }

    @Override
    public String toString() {
        return "engine.math.geometry.Vertex3D{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

}
