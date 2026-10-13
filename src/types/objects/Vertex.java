package types.objects;

import types.datatypes.Vector3;

import java.awt.*;

public class Vertex {

    public Vector3 coordinates;
    public Color color;
    public int radius;
    public boolean opaque;

    public Vertex(Vector3 coordinates, Color color, int radius, boolean opaque) {
        this.coordinates = coordinates;
        this.color = color;
        this.radius = radius;
        this.opaque = opaque;
    }
}
