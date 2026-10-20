package types.parts;

import java.awt.*;

public class Line {

    public int index1, index2;
    public Color color;
    public float thickness;
    public boolean opaque;

    public Line(int index1, int index2, Color color, float thickness, boolean opaque) {

        this.index1 = index1;
        this.index2 = index2;
        this.color = color;
        this.thickness = thickness;
        this.opaque = opaque;
    }
}
