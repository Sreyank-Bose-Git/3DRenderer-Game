package types.parts;

import types.visuals.Texture;

public class Triangle {

    public int index1, index2, index3;
    public boolean opaque;

    // UV actualCoordinates for vertex 1
    public double u1, v1;

    // UV actualCoordinates for vertex 2
    public double u2, v2;

    // UV actualCoordinates for vertex 3
    public double u3, v3;

    // Texture assigned to this triangle
    public Texture texture;

    public Triangle(int index1, int index2, int index3, Texture texture, boolean opaque,
                    double u1, double v1,
                    double u2, double v2,
                    double u3, double v3) {

        this.index1 = index1;
        this.index2 = index2;
        this.index3 = index3;
        this.texture = texture;
        this.opaque = opaque;

        this.u1 = u1;
        this.u2 = u2;
        this.u3 = u3;

        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }
}
