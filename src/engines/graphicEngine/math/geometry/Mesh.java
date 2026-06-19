package engines.graphicEngine.math.geometry;

import engines.graphicEngine.math.tools.Matrix;
import engines.graphicEngine.math.tools.Vector3D;
import engines.graphicEngine.renderer.Texture;

import java.awt.*;
    import java.util.ArrayList;
    import java.util.List;

public class Mesh {
    private List<Triangle> meshTriangle;
    private String meshName = "";

    public Mesh() {
        this.meshTriangle = new ArrayList<>();
    }

    public void createTriangleNormal() {
        List<Triangle> meshTriangle = this.meshTriangle;
        for (Triangle triangle : meshTriangle) {
            Vector3D normal = triangle.getNormal();

            Vector3D[] normalVertices = triangle.getNormalsVertices();
            for (int i=0; i<3; i++) {
                normalVertices[i].addInPlace(normal);
            }
        }

        for (Triangle triangle : meshTriangle) {
            Vector3D[] normalVertices = triangle.getNormalsVertices();
            for (int i=0; i<3; i++) {
                normalVertices[i].normalizeInPlace();
            }
        }
    }

    public List<Triangle> getMeshTriangle() {
        return meshTriangle;
    }

    public String getMeshName() {
        return meshName;
    }

    public void setMeshName(String meshName) {
        this.meshName = meshName;
    }
}
