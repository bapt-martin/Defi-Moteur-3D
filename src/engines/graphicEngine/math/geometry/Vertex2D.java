package engines.graphicEngine.math.geometry;

import engines.graphicEngine.math.tools.Matrix;

public class Vertex2D {
    public double u, v, w;

    public Vertex2D(double u, double v, double w) {
        this.u = u;
        this.v = v;
        this.w = w;
    }

    public Vertex2D(double u, double v) {
        this.u = u;
        this.v = v;
        this.w = 1;
    }

    public Vertex2D() {
        this.u = 0;
        this.v = 0;
        this.w = 1;
    }

    public Vertex2D(Vertex2D other) {
        this.u = other.u;
        this.v = other.v;
        this.w = other.w;
    }

    public Vertex2D transformVertex2DInPlace(Matrix mat) {
        double[][] M = mat.getMatrix();
        double uIn = this.u;
        double vIn = this.v;

        this.u = uIn * M[0][0] + vIn * M[1][0];
        this.v = uIn * M[0][1] + vIn * M[1][1];

        return this;
    }

    @Override
    public String toString() {
        return "Vertex2D{" +
                "aregdfhg=" + u +
                ", v=" + v +
                '}';
    }
}
