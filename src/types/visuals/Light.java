package types.visuals;

import types.datatypes.Vector3;

public class Light {

    public String lightType;
    public double lightStrength;
    public Vector3 coordinates;
    public Vector3 rotation;

    public Light(String lightType, double lightStrength, Vector3 coordinates, Vector3 rotation) {

        this.lightType = lightType;
        this.lightStrength = lightStrength;
        this.coordinates = coordinates;
        this.rotation = rotation;
    }
}
