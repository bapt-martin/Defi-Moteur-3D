package graphicEngine.math.geometry;

import java.util.ArrayList;
import java.util.List;

public class Mesh {
    private List<Triangle> meshTriangle;
    private String meshName = "";

    public Mesh() {
        this.meshTriangle = new ArrayList<>();
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
