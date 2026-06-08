package engines.graphicEngine.math.tools;

public class Quaternion {
    private double w;
    private double x;
    private double y;
    private double z;

    public Quaternion() {
        this.w = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public Quaternion(double angle, Vector3D axis) {
        axis.normalizeInPlace();
        double halfAngle = angle / 2;
        double s = Math.sin(halfAngle);

        this.w = Math.cos(halfAngle);
        this.x = axis.getX() * s;
        this.y = axis.getY() * s;
        this.z = axis.getZ() * s;
    }

    public Quaternion(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quaternion(double x, double y, double z) {
        this.w = 0;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quaternion(double w) {
        this.w = w;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }


}
