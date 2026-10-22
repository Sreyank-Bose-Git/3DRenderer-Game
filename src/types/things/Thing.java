package types.things;

import types.datatypes.Vector3;

public class Thing {

    public Vector3 actualCoordinates = new Vector3(0, 0, 0);
    public Vector3 actualRotation = new Vector3(0, 0, 0);

    public Vector3 localCoordinates = new Vector3(0, 0, 0);
    public Vector3 localRotation = new Vector3(0, 0, 0);

    public String collectionName = "default";
}
