package graphicEngine.scene.lightRelative;

import graphicEngine.math.tools.Vector3D;
import java.awt.Color;

public class SpotLight extends PointLight {

    private Vector3D direction;
    private double cutOffAngle;

    public SpotLight(Vector3D position, double fallOff, Color lightColor, Vector3D direction, double cutoffAngleDegrees) {
        super(position, fallOff, lightColor);

        this.direction = direction;
        this.direction.normalizeInPlace();

        this.cutOffAngle = Math.cos(Math.toRadians(cutoffAngleDegrees));
    }

    public Vector3D getDirection() {
        return direction;
    }

    public void setDirection(Vector3D direction) {
        this.direction = direction;
        this.direction.normalizeInPlace();
    }

    public double getCutOffAngle() {
        return cutOffAngle;
    }

    public void setCutOffAngle(double cutOffAngle) {
        this.cutOffAngle = cutOffAngle;
    }
}