
package exec.renderer;

import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.things.meshes.Mesh;
import types.things.meshes.Mesh.TriangleData;
import types.things.Thing;

public class Camera extends Thing {

    public Camera(
            RendererPanel rendererPanel,
            Vector3 coordinates,
            Vector2 rotation,
            String collectionName
    ) {
        this.collectionName = collectionName;

        this.localCoordinates = coordinates;

        this.localRotation.x = rotation.x;
        this.localRotation.y = rotation.y;
    }
}
