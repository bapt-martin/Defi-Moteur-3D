package engines.graphicEngine.math.tools;

import engines.graphicEngine.math.geometry.Vertex3D;

public class Vector3D extends Tuple3D{
    public Vector3D() {
        super(0,0,0,0);
    }

    public Vector3D(double x, double y, double z) {
        super(x,y,z,0);
    }

    public Vector3D(Tuple3D tuple3D) {
        super(tuple3D);
    }

    public static void rotateBasisInPlace(Matrix matrix, Vector3D[] localAxes) {
        for (int i = 0; i<3; i++) {
            localAxes[i].transformInPlace(matrix);
            localAxes[i].normalizeInPlace();
        }
    }

    @Override
    public Vector3D crossProduct(Tuple3D other) {
        return new Vector3D(super.crossProduct(other));
    }

    public Vector3D normalizeInPlace() {
        if (this.getLength() != 0) {
            double length = this.getLength();

            double invLen = 1.0 / length;
            this.x *= invLen;
            this.y *= invLen;
            this.z *= invLen;
        }
        return this;
    }

    public Vector3D normalized() {
        double length = this.getLength();
        if (length == 0) {
            return new Vector3D(0, 0, 0);
        }
        double invLen = 1.0 / length;
        return new Vector3D(this.x * invLen, this.y * invLen, this.z * invLen);
    }

    @Override
    public Vector3D scaled(double factor) {
        return new Vector3D(super.scaled(factor));
    }

    @Override
    public Vector3D divided(double divisor) {
        return new Vector3D(super.divided(divisor));
    }

    @Override
    public Vector3D divideInPlace(double divisor) {
        super.divideInPlace(divisor);
        return this;
    }

    @Override
    public Vector3D scaleInPlace(double factor) {
        super.scaleInPlace(factor);
        return this;
    }

    @Override
    public Vector3D add(Tuple3D other) {
        return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public void transformAndStoreIn(double[][] m, Vector3D destinationVertex) {

        double newX = this.x * m[0][0] + this.y * m[1][0] + this.z * m[2][0];
        double newY = this.x * m[0][1] + this.y * m[1][1] + this.z * m[2][1];
        double newZ = this.x * m[0][2] + this.y * m[1][2] + this.z * m[2][2];

        destinationVertex.x = newX;
        destinationVertex.y = newY;
        destinationVertex.z = newZ;

        if (newX != 0.0 || newY != 0.0 || newZ != 0.0) {
            destinationVertex.normalizeInPlace();
        }
    }

    public void addInPlace(Tuple3D other) {
        this.x += other.x;
        this.y += other.y;
        this.z += other.z;
    }

    @Override
    public Vector3D sub(Tuple3D other) {
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    @Override
    public Vector3D transformed(Matrix matIn) {
        return new Vector3D(super.transformed(matIn));
    }

    @Override
    public Vector3D transformInPlace(Matrix mat) {
        super.transformInPlace(mat);
        return this;
    }
}
