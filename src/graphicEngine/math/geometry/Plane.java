package graphicEngine.math.geometry;

import graphicEngine.math.tools.Vector3D;
import graphicEngine.renderer.Texture;

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

    public int clipTriangleAgainstPlane(Triangle triIn, List<Triangle> trisOut) {
        Texture texture = triIn.getTexture();

        Vertex3D[] ptsInside  = new Vertex3D[3]; int nbPointsInside = 0;
        Vertex3D[] ptsOutside = new Vertex3D[3]; int nbPointsOutside = 0;

        Vertex2D[] textPtsInside  = new Vertex2D[3]; int nbTextPointsInside = 0;
        Vertex2D[] textPtsOutside = new Vertex2D[3]; int nbTextPointsOutside = 0;

        Vertex3D[] vertsIn = triIn.getVertices();
        Vertex2D[] textVertsIn = triIn.getTextVertices();

        double u;
        double v;

        // Vertex classification
        for (int i = 0; i < 3; i++) {
            double dDistPoint = this.signedDistanceToPoint(triIn.getVertices()[i]);
            if (dDistPoint >= 0) {
                ptsInside[nbPointsInside++]         = vertsIn[i];
                textPtsInside[nbTextPointsInside++] = textVertsIn[i];
            } else {
                ptsOutside[nbPointsOutside++]       = vertsIn[i];
                textPtsOutside[nbTextPointsOutside++] = textVertsIn[i];
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

            vertsOut1[0] = ptsInside[0];
            textVertsOut1[0] = textPtsInside[0];

            double intersectionDistance = this.intersectionDistance(ptsInside[0],ptsOutside[0]);
            vertsOut1[1] = this.intersectSegmentWithPlane(ptsInside[0],ptsOutside[0], intersectionDistance);
            u = intersectionDistance * (textPtsOutside[0].u - textPtsInside[0].u) + textPtsInside[0].u;
            v = intersectionDistance * (textPtsOutside[0].v - textPtsInside[0].v) + textPtsInside[0].v;
            textVertsOut1[1] = new Vertex2D(u,v);

            intersectionDistance = this.intersectionDistance(ptsInside[0],ptsOutside[1]);
            vertsOut1[2] = this.intersectSegmentWithPlane(ptsInside[0],ptsOutside[1], intersectionDistance);
            u = intersectionDistance * (textPtsOutside[1].u - textPtsInside[0].u) + textPtsInside[0].u;
            v = intersectionDistance * (textPtsOutside[1].v - textPtsInside[0].v) + textPtsInside[0].v;
            textVertsOut1[2] = new Vertex2D(u,v);

//            trisOut.add(new Triangle(vertsOut1, triIn.getColor()));
            trisOut.add(new Triangle(vertsOut1, textVertsOut1, Color.RED, texture));

            return 1;
        }
        if (nbPointsInside == 2) {
            // 2 triangles are created
            Vertex3D[] vertsOut1 = new Vertex3D[3];
            Vertex3D[] vertsOut2 = new Vertex3D[3];

            Vertex2D[] textVertsOut1 = new Vertex2D[3];
            Vertex2D[] textVertsOut2 = new Vertex2D[3];

            vertsOut1[0] = ptsInside[0];
            textVertsOut1[0] = textPtsInside[0];
            vertsOut1[1] = ptsInside[1];
            textVertsOut1[1] = textPtsInside[1];

            double intersectionDistance = this.intersectionDistance(ptsInside[0],ptsOutside[0]);
            vertsOut1[2] = this.intersectSegmentWithPlane(ptsInside[0],ptsOutside[0],intersectionDistance);  //=vertsout2[1]
            u = intersectionDistance * (textPtsOutside[0].u - textPtsInside[0].u) + textPtsInside[0].u;
            v = intersectionDistance * (textPtsOutside[0].v - textPtsInside[0].v) + textPtsInside[0].v;;
            textVertsOut1[2] = new Vertex2D(u,v);


            vertsOut2[0] = ptsInside[1];
            textVertsOut2[0] = textPtsInside[1];

            intersectionDistance = this.intersectionDistance(ptsInside[1],ptsOutside[0]);
            vertsOut2[1] = this.intersectSegmentWithPlane(ptsInside[1],ptsOutside[0],intersectionDistance);
            u = intersectionDistance * (textPtsOutside[0].u - textPtsInside[1].u) + textPtsInside[1].u;
            v = intersectionDistance * (textPtsOutside[0].v - textPtsInside[1].v) + textPtsInside[1].v;
            textVertsOut2[1] = new Vertex2D(u,v);


            vertsOut2[2] = vertsOut1[2];
            textVertsOut2[2] = textVertsOut1[2];


//            trisOut.add(new Triangle(vertsOut1, triIn.getColor()));
//            trisOut.add(new Triangle(vertsOut2, triIn.getColor()));
            trisOut.add(new Triangle(vertsOut1, textVertsOut1, Color.BLUE, texture));
            trisOut.add(new Triangle(vertsOut2, textVertsOut2, Color.GREEN, texture));

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
