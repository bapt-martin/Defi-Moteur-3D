package graphicEngine.math.tools;
import graphicEngine.math.geometry.Vertex3D;

import static java.lang.Math.*;

public class Matrix {
    // ROW-MAJOR CONVENTION
    private double[][] matrix;
    private static final Matrix TEMP_WORKER = new Matrix();

    public Matrix(int nbRow, int nbCol) {
        this.matrix = new double[nbRow][nbCol];
    }

    public Matrix() {
        this.matrix = new double[4][4];
    }

    public Matrix(double[][] matrix) {
        this.matrix = new double[4][4];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(matrix[i], 0, this.matrix[i], 0, 4);
        }
    }

    public Matrix(Matrix other) {
        this(other.matrix);
    }

    public void copyFrom(Matrix other) {
        double[][] mat = other.getMatrix();

        for (int i = 0; i < 4; i++) {
            System.arraycopy(mat[i], 0, this.matrix[i], 0, 4);
        }
    }

//    public double get(int row, int col) {
//        return matrix[row * 4 + col];
//    }
//
//    public void set(int row, int col, double value) {
//        matrix[row * 4 + col] = value;
//    }

    public static Matrix createProjectionMatrix(double far, double near, double fov, int width, int height, double zoom) {
        double q = far / (far - near);
        double aspectRatio = (double) width / height;
        double scalingFactorRad = zoom / tan((fov * 0.5 * Math.PI) / 180 );

        double[][] matProj = new double[4][4];
        matProj[0][0] = scalingFactorRad * aspectRatio;
        matProj[1][1] = scalingFactorRad;
        matProj[2][2] = q;
        matProj[3][3] = 0;

        matProj[3][2] = -near * q;
        matProj[2][3] = 1;

        return new Matrix(matProj);
    }

    public static Matrix createEulerRotation(double theta, double phi, double psi) {
        Matrix matRotX = Matrix.createRotationX(theta);
        Matrix matRotY = Matrix.createRotationY(phi);
        Matrix matRotZ = Matrix.createRotationZ(psi);

        multiply(matRotX,matRotY,matRotX);
        multiply(matRotX,matRotZ,matRotX);

//        matRotX.multiply(matRotY).multiply(matRotZ);
        return matRotX;
    }

    public static Matrix createWorldTransformMatrix(double sx, double sy, double sz, double theta, double phi, double psi, double x, double y, double z) {
        Matrix matScaling     = Matrix.createScalingMatrix(sx, sy, sz);
        Matrix matRotation    = Matrix.createEulerRotation(theta,phi,psi);
        Matrix matTranslation = Matrix.createTranslation(x,y,z);

        multiply(matScaling,matRotation,matScaling);
        multiply(matScaling,matTranslation,matScaling);

//        matScaling.multiply(matRotation).multiply(matTranslation);
        return matScaling;
    }

    public static Matrix createViewMatrix(Vertex3D pTargetPosition, Vector3D vTargetDirection, Vector3D vUp) {
        //New forward direction
        Vector3D vNewForward = new Vector3D(vTargetDirection);
        vNewForward.normalizeInPlace();

        //New up direction
        Vector3D vScale = vNewForward.scaled(vUp.dotProduct(vNewForward));
        Vector3D vNewUp = vUp.sub(vScale);
        vNewUp.normalizeInPlace();

        //New right direction
        Vector3D vNewRight = vNewUp.crossProduct(vNewForward);
        vNewRight.normalizeInPlace();

        double[][] matOut = new double[4][4];
        matOut[0][0] = vNewRight.getX();       matOut[0][1] = vNewRight.getY();       matOut[0][2] = vNewRight.getZ();       matOut[0][3] = 0;
        matOut[1][0] = vNewUp.getX();          matOut[1][1] = vNewUp.getY();          matOut[1][2] = vNewUp.getZ();          matOut[1][3] = 0;
        matOut[2][0] = vNewForward.getX();     matOut[2][1] = vNewForward.getY();     matOut[2][2] = vNewForward.getZ();     matOut[2][3] = 0;
        matOut[3][0] = pTargetPosition.getX(); matOut[3][1] = pTargetPosition.getY(); matOut[3][2] = pTargetPosition.getZ(); matOut[3][3] = 1;

        return new Matrix(matOut).invertRotationTranslation();
    }

    public Matrix invertRotationTranslation(){ //Only Rotation/Translation matrices
        double[][] mat = this.getMatrix();
        double[][] matInverse = new double[4][4];

        matInverse[0][0] = mat[0][0]; matInverse[0][1] = mat[1][0]; matInverse[0][2] = mat[2][0];
        matInverse[1][0] = mat[0][1]; matInverse[1][1] = mat[1][1]; matInverse[1][2] = mat[2][1];
        matInverse[2][0] = mat[0][2]; matInverse[2][1] = mat[1][2]; matInverse[2][2] = mat[2][2];
                                                                                                  matInverse[3][3] = 1;

        matInverse[3][0] = -(mat[3][0] * matInverse[0][0] + mat[3][1] * matInverse[1][0] + mat[3][2] * matInverse[2][0]);
        matInverse[3][1] = -(mat[3][0] * matInverse[0][1] + mat[3][1] * matInverse[1][1] + mat[3][2] * matInverse[2][1]);
        matInverse[3][2] = -(mat[3][0] * matInverse[0][2] + mat[3][1] * matInverse[1][2] + mat[3][2] * matInverse[2][2]);

        this.setMatrix(matInverse);

        return this;
    }

    public double getDeterminant() {
        double[][] m = this.matrix;

        return  m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
                m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
                m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    }

    public static Matrix createScalingMatrix(double x, double y, double z) {
        Matrix scalingMatrix = Matrix.createIdentity(4, 4);
        double[][] mat = scalingMatrix.getMatrix();

        mat[0][0] = x;
        mat[1][1] = y;
        mat[2][2] = z;
        mat[3][3] = 1;

        return scalingMatrix;
    }

    public static Matrix createIdentity(int nbRow, int nbCol) {
        double[][] matIdentity = new double[nbRow][nbCol];

        for (int i = 0; i< nbRow; i++) {
            for (int j= 0; j< nbCol; j++) {
                if (i == j) {
                    matIdentity[i][j] = 1;
                }
            }
        }

        return new Matrix(matIdentity);
    }

    public static Matrix createRotationX(double theta){
        double[][] matRotX = new double[4][4];
        double radTheta = theta * (Math.PI / 180);

        matRotX[0][0] = 1;
        matRotX[1][1] = cos(radTheta);

        matRotX[1][2] = sin(radTheta);
        matRotX[2][1] = -sin(radTheta);

        matRotX[2][2] = cos(radTheta);
        matRotX[3][3] = 1;

        return new Matrix(matRotX);
    }

    public static Matrix createRotationY(double theta) {
        double[][] matRotY = new double[4][4];
        double radTheta = theta * (Math.PI / 180);

        matRotY[0][0] = Math.cos(radTheta);
        matRotY[0][2] = Math.sin(radTheta);

        matRotY[1][1] = 1;
        matRotY[2][0] = -Math.sin(radTheta);

        matRotY[2][2] = Math.cos(radTheta);
        matRotY[3][3] = 1;

        return new Matrix(matRotY);
    }

    public static Matrix createRotationZ(double theta){
        double[][] matRotZ = new double[4][4];
        double radTheta = theta * (Math.PI / 180);

        matRotZ[0][0] = cos(radTheta);
        matRotZ[0][1] = sin(radTheta);

        matRotZ[1][0] = -sin(radTheta);
        matRotZ[1][1] = cos(radTheta);

        matRotZ[2][2] = 1;
        matRotZ[3][3] = 1;

        return new Matrix(matRotZ);
    }

    public static Matrix createRotationAroundAxis(double theta, Vector3D vAxis) {
        vAxis.normalizeInPlace();
        double[][] matRotationAxis = new double[4][4];

        double dUx = vAxis.getX();
        double dUy = vAxis.getY();
        double dUz = vAxis.getZ();

        double dCos = Math.cos(theta);
        double dSin = Math.sin(theta);

        matRotationAxis[0][0] = dCos + dUx*dUx*(1 - dCos);     matRotationAxis[0][1] = dUx*dUy*(1 - dCos) - dUz*dSin; matRotationAxis[0][2] = dUx*dUz*(1 - dCos) + dUy*dSin; matRotationAxis[0][3] = 0;
        matRotationAxis[1][0] = dUy*dUx*(1 - dCos) + dUz*dSin; matRotationAxis[1][1] = dCos + dUy*dUy*(1 - dCos);     matRotationAxis[1][2] = dUy*dUz*(1 - dCos) - dUx*dSin; matRotationAxis[1][3] = 0;
        matRotationAxis[2][0] = dUz*dUx*(1 - dCos) - dUy*dSin; matRotationAxis[2][1] = dUz*dUy*(1 - dCos) + dUx*dSin; matRotationAxis[2][2] = dCos + dUz*dUz*(1 - dCos);     matRotationAxis[2][3] = 0;
        matRotationAxis[3][0] = 0;                             matRotationAxis[3][1] = 0;                             matRotationAxis[3][2] = 0;                             matRotationAxis[3][3] = 1;

        return new Matrix(matRotationAxis);
    }

    public static void multiply(Matrix matIn1, Matrix matIn2, Matrix matOut) {
        double[][] mat1 = matIn1.getMatrix();
        double[][] mat2 = matIn2.getMatrix();
        double[][] matRes = matOut.getMatrix();

        if (matOut == matIn1 || matOut == matIn2) {
            multiply(matIn1, matIn2, TEMP_WORKER);
            matOut.copyFrom(TEMP_WORKER);
            return;
        }

        rowColMultiplication(mat1, mat2, matRes);
    }

    public Matrix multiply(Matrix matIn) {
        double[][] mat1 = this.getMatrix();
        double[][] mat2 = matIn.getMatrix();
        double[][] matOut = new double[4][4];

        rowColMultiplication(mat1, mat2, matOut);

        return new Matrix(matOut);
    }

    private static void rowColMultiplication(double[][] mat1, double[][] mat2, double[][] matOut) {
        for (int i = 0; i < 4; i++) {
            double[] row = mat1[i];
            for (int col = 0; col < 4; col++) {
                matOut[i][col] = row[0] * mat2[0][col]
                               + row[1] * mat2[1][col]
                               + row[2] * mat2[2][col]
                               + row[3] * mat2[3][col];
            }
        }
    }

    public static Matrix createTranslation(double x, double y, double z) {
        double[][] matrixTranslation = createIdentity(4,4).getMatrix();

        matrixTranslation[3][0] = x;
        matrixTranslation[3][1] = y;
        matrixTranslation[3][2] = z;

        return new Matrix(matrixTranslation);
    }

    public void matPrint() {
        for (int i = 0; i < 4; i++) {
            System.out.printf("| %8.4f %8.4f %8.4f %8.4f |\n",
                    this.getMatrix()[i][0], this.getMatrix()[i][1], this.getMatrix()[i][2], this.getMatrix()[i][3]);
        }
    }

    public double[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(double[][] matrix) {
        this.matrix = matrix;
    }
}
