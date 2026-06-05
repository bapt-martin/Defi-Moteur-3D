package graphicEngine.scene.lightRelative;

import graphicEngine.math.tools.Vector3D;

import java.awt.*;

public class PointLight {
    private String name;
    private int id;
    private Vector3D position;
    private double fallOff;
    private Color lightColor;
    private boolean isOn = true;

    public PointLight(Vector3D position, double fallOff, Color lightColor) {
        this.position = position;
        this.fallOff = fallOff;
        this.lightColor = lightColor;
    }

    public Vector3D getPosition() {
        return position;
    }

    public void setPosition(Vector3D position) {
        this.position = position;
    }

    public void setPosition(double x, double y, double z) {
        this.position.setX(x);
        this.position.setY(y);
        this.position.setZ(z);
    }

    public void move(double dx, double dy, double dz) {
        this.position.setX(this.position.getX() + dx);
        this.position.setY(this.position.getY() + dy);
        this.position.setZ(this.position.getZ() + dz);
    }

    public double getFallOff() {
        return fallOff;
    }

    public void setFallOff(double fallOff) {
        this.fallOff = fallOff;
    }

    public Color getLightColor() {
        return lightColor;
    }

    public void setLightColor(Color lightColor) {
        this.lightColor = lightColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}