package engines.graphicEngine.math.geometry;

import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Texture;

import java.awt.*;
import java.util.List;

public class Plane {
    private Vertex3D origin;
    private Vector3D normal;

    public Plane(Vertex3D origin, Vector3D normal) {
        this.origin = origin;
        this.normal = normal;
        this.normal.normalizeInPlace();
    }

    public Plane() {
        this.origin = new Vertex3D();
        this.normal = new Vector3D();
    }

    public Plane(Plane other) {
        this(other.origin, other.normal);
    }

    private Vertex2D interpolateTexture(Vertex2D v1, Vertex2D v2, double t) {//rasterizer
        double u =  t * (v2.u - v1.u) + v1.u ;
        double v =  t * (v2.v - v1.v) + v1.v ;
        double w =  t * (v2.w - v1.w) + v1.w ;
        return new Vertex2D(u, v, w);
    }

    private Vector3D interpolateNormal(Vector3D n1, Vector3D n2, double t) {//rasterizer
        double x =  t * (n2.getX() - n1.getX()) + n1.getX() ;
        double y =  t * (n2.getY() - n1.getY()) + n1.getY() ;
        double z =  t * (n2.getZ() - n1.getZ()) + n1.getZ() ;
        return new Vector3D(x, y, z).normalizeInPlace();
    }

    private Color interpolateColor(Color c1, Color c2, double t) {//rasterizer
        int r = (int) (t * (c2.getRed()   - c1.getRed()  ) + c1.getRed());
        int g = (int) (t * (c2.getGreen() - c1.getGreen()) + c1.getGreen());
        int b = (int) (t * (c2.getBlue()  - c1.getBlue() ) + c1.getBlue());
        int a = (int) (t * (c2.getAlpha() - c1.getAlpha()) + c1.getAlpha());

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        a = Math.max(0, Math.min(255, a));

        return new Color(r, g, b, a);
    }

    public int clipTriangleAgainstPlane(Triangle triIn, List<Triangle> trisOut) {
        Vertex3D[] vertsIn = triIn.getVertices();

        double d0 = this.signedDistanceToPoint(vertsIn[0]);
        double d1 = this.signedDistanceToPoint(vertsIn[1]);
        double d2 = this.signedDistanceToPoint(vertsIn[2]);

        int nbPointsInside = (d0 >= 0 ? 1 : 0) + (d1 >= 0 ? 1 : 0) + (d2 >= 0 ? 1 : 0);

        if (nbPointsInside == 3) {
            trisOut.add(triIn);
            return 1;
        }
        if (nbPointsInside == 0) {
            return 0;
        }

        Texture texture = triIn.getTexture();

        Vertex2D[] textVertsIn = triIn.getTextVertices();
        Vector3D[] normalsVertsIn = triIn.getNormalsVertices();
        Color[] colorVertsIn = triIn.getLightIntensities();

        Vertex3D[] ptsInside = new Vertex3D[3];    Vertex3D[] ptsOutside = new Vertex3D[3];
        Vertex2D[] textPtsInside = new Vertex2D[3]; Vertex2D[] textPtsOutside = new Vertex2D[3];
        Vector3D[] normalsPtsInside = new Vector3D[3]; Vector3D[] normalsPtsOutside = new Vector3D[3];
        Color[] colorsPtsInside = new Color[3];    Color[] colorsPtsOutside = new Color[3];

        // Vertex classification
        int insideCount = 0;
        int outsideCount = 0;

        for (int i = 0; i < 3; i++) {
            double dDistPoint = this.signedDistanceToPoint(vertsIn[i]);
            if (dDistPoint >= 0) {
                ptsInside[insideCount]        = vertsIn[i];
                textPtsInside[insideCount]    = textVertsIn[i];
                normalsPtsInside[insideCount] = normalsVertsIn[i];
                colorsPtsInside[insideCount]  = colorVertsIn[i];
                insideCount++;
            } else {
                ptsOutside[outsideCount]        = vertsIn[i];
                textPtsOutside[outsideCount]    = textVertsIn[i];
                normalsPtsOutside[outsideCount] = normalsVertsIn[i];
                colorsPtsOutside[outsideCount]  = colorVertsIn[i];
                outsideCount++;
            }
        }

        if (nbPointsInside == 3) {
            // All points are inside the plane
            trisOut.add(triIn);
            return 1;
        }
        if (nbPointsInside == 1) {
            // The triangle simply become a smaller triangle
            Vertex3D[] vertsOut1 = new Vertex3D[3];
            Vertex2D[] textVertsOut1 = new Vertex2D[3];
            Vector3D[] normalsVertsOut1 = new Vector3D[3];
            Color[]    colorsVertsOut1 = new Color[3];

            vertsOut1[0]        = ptsInside[0];
            textVertsOut1[0]    = textPtsInside[0];
            normalsVertsOut1[0] = normalsPtsInside[0];
            colorsVertsOut1[0]  = colorsPtsInside[0];


            double intersectionDistance = this.intersectionDistance(ptsInside[0],ptsOutside[0]);
            vertsOut1[1] = this.intersectSegmentWithPlane(ptsInside[0],ptsOutside[0], intersectionDistance);
            textVertsOut1[1]    = interpolateTexture(textPtsInside[0], textPtsOutside[0], intersectionDistance);
            normalsVertsOut1[1] = interpolateNormal(normalsPtsInside[0], normalsPtsOutside[0], intersectionDistance);
            colorsVertsOut1[1]  = interpolateColor(colorsPtsInside[0], colorsPtsOutside[0], intersectionDistance);

            intersectionDistance = this.intersectionDistance(ptsInside[0],ptsOutside[1]);
            vertsOut1[2] = this.intersectSegmentWithPlane(ptsInside[0],ptsOutside[1], intersectionDistance);
            textVertsOut1[2]    = interpolateTexture(textPtsInside[0], textPtsOutside[1], intersectionDistance);
            normalsVertsOut1[2] = interpolateNormal(normalsPtsInside[0], normalsPtsOutside[1], intersectionDistance);
            colorsVertsOut1[2]  = interpolateColor(colorsPtsInside[0], colorsPtsOutside[1], intersectionDistance);

            trisOut.add(Triangle.createDeepTriangle(vertsOut1, textVertsOut1, normalsVertsOut1, colorsVertsOut1, triIn.getColor(), texture));
//            trisOut.add(new Triangle(vertsOut1, textVertsOut1, Color.RED, texture));

            return 1;
        }
        if (nbPointsInside == 2) {
            // 2 triangles are created
            Vertex3D[] vertsOut1 = new Vertex3D[3];
            Vertex2D[] textVertsOut1 = new Vertex2D[3];
            Vector3D[] normalsVertsOut1 = new Vector3D[3];
            Color[]    colorsVertsOut1 = new Color[3];

            Vertex3D[] vertsOut2 = new Vertex3D[3];
            Vertex2D[] textVertsOut2 = new Vertex2D[3];
            Vector3D[] normalsVertsOut2 = new Vector3D[3];
            Color[]    colorsVertsOut2 = new Color[3];


            vertsOut1[0]        = ptsInside[0];
            textVertsOut1[0]    = textPtsInside[0];
            normalsVertsOut1[0] = normalsPtsInside[0];
            colorsVertsOut1[0]  = colorsPtsInside[0];

            vertsOut1[1]        = ptsInside[1];
            textVertsOut1[1]    = textPtsInside[1];
            normalsVertsOut1[1] = normalsPtsInside[1];
            colorsVertsOut1[1]  = colorsPtsInside[1];

            double intersectionDistance = this.intersectionDistance(ptsInside[0],ptsOutside[0]);
            vertsOut1[2] = this.intersectSegmentWithPlane(ptsInside[0],ptsOutside[0],intersectionDistance);  //=vertsout2[1]
            textVertsOut1[2]    = interpolateTexture(textPtsInside[0], textPtsOutside[0], intersectionDistance);
            normalsVertsOut1[2] = interpolateNormal(normalsPtsInside[0], normalsPtsOutside[0], intersectionDistance);
            colorsVertsOut1[2]  = interpolateColor(colorsPtsInside[0], colorsPtsOutside[0], intersectionDistance);


            vertsOut2[0]        = ptsInside[1];
            textVertsOut2[0]    = textPtsInside[1];
            normalsVertsOut2[0] = normalsPtsInside[1];
            colorsVertsOut2[0]  = colorsPtsInside[1];

            intersectionDistance = this.intersectionDistance(ptsInside[1],ptsOutside[0]);
            vertsOut2[1] = this.intersectSegmentWithPlane(ptsInside[1],ptsOutside[0],intersectionDistance);
            textVertsOut2[1]    = interpolateTexture(textPtsInside[1], textPtsOutside[0], intersectionDistance);
            normalsVertsOut2[1] = interpolateNormal(normalsPtsInside[1], normalsPtsOutside[0], intersectionDistance);
            colorsVertsOut2[1]  = interpolateColor(colorsPtsInside[1], colorsPtsOutside[0], intersectionDistance);


            vertsOut2[2]        = vertsOut1[2];
            textVertsOut2[2]    = textVertsOut1[2];
            normalsVertsOut2[2] = normalsVertsOut1[2];
            colorsVertsOut2[2]  = colorsVertsOut1[2];


            trisOut.add(Triangle.createDeepTriangle(vertsOut1, textVertsOut1, normalsVertsOut1, colorsVertsOut1, triIn.getColor(), texture));
            trisOut.add(Triangle.createDeepTriangle(vertsOut2, textVertsOut2, normalsVertsOut2, colorsVertsOut2, triIn.getColor(), texture));
//            trisOut.add(new Triangle(vertsOut1, textVertsOut1, Color.BLUE, texture));
//            trisOut.add(new Triangle(vertsOut2, textVertsOut2, Color.GREEN, texture));

            return 2;
        }
        else {
            return 0;
        }
    }

    public double signedDistanceToPoint(Vertex3D pPoint) {
        Vertex3D vertPlanePoint = this.getOrigin();
        Vector3D vectPlaneNorm = this.getNormal();

        return vectPlaneNorm.dotProduct(pPoint) - vertPlanePoint.dotProduct(vectPlaneNorm);
    }

    public double intersectionDistance(Vertex3D pLineStart, Vertex3D pLineEnd) {
        Vertex3D vertPlanePoint = this.getOrigin();
        Vector3D vectPlaneNorm = this.getNormal();

        double dPlaneConstant = -vectPlaneNorm.dotProduct(vertPlanePoint);
        double ad = pLineStart.dotProduct(vectPlaneNorm);
        double bd = pLineEnd.dotProduct(vectPlaneNorm);

        return  (-dPlaneConstant - ad) / (bd - ad);
    }

    public Vertex3D intersectSegmentWithPlane(Vertex3D pLineStart, Vertex3D pLineEnd, double intersectionDistance ) {
        Vector3D vectLineStartToEnd = new Vector3D(pLineEnd.sub(pLineStart));
        Vector3D vectLineIntersecting = vectLineStartToEnd.scaled(intersectionDistance);

        return pLineStart.translated(vectLineIntersecting);
    }

    public Vertex3D getOrigin() {
        return origin;
    }

    public Vector3D getNormal() {
        return normal;
    }
}
