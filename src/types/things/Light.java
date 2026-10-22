package types.things;

import types.datatypes.Vector3;

public class Light extends Thing{

    public String lightType;
    public double lightStrength;

    public Light(String lightType, double lightStrength, Vector3 coordinates, Vector3 rotation, String collectionName) {

        this.lightType = lightType;
        this.lightStrength = lightStrength;
        this.localCoordinates = coordinates;
        this.actualRotation = rotation;
        this.collectionName = collectionName;
    }
}
