package graphicEngine.math.geometry;

import graphicEngine.math.tools.Matrix;

public class Vertex2D {
    public double u, v;

    public Vertex2D(double u, double v) {
        this.u = u;
        this.v = v;
    }

    public Vertex2D() {
        this.u = 0;
        this.v = 0;
    }

    public Vertex2D(Vertex2D other) {
        this.u = other.u;
        this.v = other.v;
    }

    public Vertex2D transformVertex2DInPlace(Matrix mat) {
        double[][] M = mat.getMatrix();
        double uIn = this.u;
        double vIn = this.v;

        this.u = uIn * M[0][0] + vIn * M[1][0];
        this.v = uIn * M[0][1] + vIn * M[1][1];

        return this;
    }
}
