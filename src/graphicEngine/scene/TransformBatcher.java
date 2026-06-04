package graphicEngine.scene;

import graphicEngine.math.tools.Matrix;
import java.util.List;

public class TransformBatcher {

    public record ObjectVisibilityData(GameObject targetObject, boolean isRendered) {}
    public record ObjectMatrixData(GameObject targetObject, Matrix matrix) {}

    public static void applyVisibilityBatch(List<ObjectVisibilityData> visibilityList) {
        for (ObjectVisibilityData data : visibilityList) {
            if (data.targetObject() != null) {
                data.targetObject().setRendered(data.isRendered());
            }
        }
    }

    public static void setGroupVisibility(List<GameObject> objects, boolean isRendered) {
        for (GameObject obj : objects) {
            if (obj != null) {
                obj.setRendered(isRendered);
            }
        }
    }

    public static void applyMatrixBatch(List<ObjectMatrixData> matrixList) {
        for (ObjectMatrixData data : matrixList) {
            if (data.targetObject() != null && data.matrix() != null) {
                data.targetObject().setWorldTransformMatrix(data.matrix());
            }
        }
    }

    public static void applyMatrices(List<GameObject> objects, List<Matrix> matrices) {
        int size = Math.min(objects.size(), matrices.size());
        for (int i = 0; i < size; i++) {
            GameObject obj = objects.get(i);
            if (obj != null && matrices.get(i) != null) {
                obj.setWorldTransformMatrix(matrices.get(i));
            }
        }
    }
}