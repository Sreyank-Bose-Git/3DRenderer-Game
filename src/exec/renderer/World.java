package exec.renderer;

import exec.renderer.Camera;
import main.RendererPanel;
import types.datatypes.Vector2;
import types.datatypes.Vector3;
import types.meshes.Mesh;
import types.visuals.Light;
import types.meshes.Object;

import java.util.Arrays;

public class World {

    public Mesh[] meshes;
    public Light[] lights;
    public Camera[] cameras;

    public int activeCameraIndex;
    public RendererPanel rendererPanel;

    public World(
            int meshesSize,
            int lightsSize,
            int camerasSize,
            RendererPanel rendererPanel) {

        meshes = new Mesh[Math.max(1, meshesSize)];
        lights = new Light[Math.max(1, lightsSize)];
        cameras = new Camera[Math.max(1, camerasSize)];

        this.rendererPanel = rendererPanel;

        createBasicWorld();
    }

    // =========================================================
    // INITIAL WORLD
    // =========================================================

    private void createBasicWorld() {

        storeCamera(
                new Vector3(0, 50, -200),
                new Vector2(0, 0)
        );

        activeCameraIndex = 0;

        storeLight(
                "point",
                5.0,
                new Vector3(0, 50, -200),
                new Vector3(0, 0, 180)
        );

        storeObject("/types/objects/cube.obj");

        Mesh mesh = getMesh(0);

        if (mesh != null) {
            mesh.scaleX *= 1000;
            mesh.scaleY *= 10;
            mesh.scaleZ *= 1000;
            mesh.updateTransform();
        }
    }

    // =========================================================
    // FIND AVAILABLE SLOTS
    // =========================================================

    private int findNextCameraIndex() {
        for (int i = 0; i < cameras.length; i++) {
            if (cameras[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextLightIndex() {
        for (int i = 0; i < lights.length; i++) {
            if (lights[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private int findNextMeshIndex() {
        for (int i = 0; i < meshes.length; i++) {
            if (meshes[i] == null) {
                return i;
            }
        }

        return -1;
    }

    // =========================================================
    // GROW ARRAYS
    // =========================================================

    private void growCameras() {
        cameras = Arrays.copyOf(
                cameras,
                Math.max(1, cameras.length * 2)
        );
    }

    private void growLights() {
        lights = Arrays.copyOf(
                lights,
                Math.max(1, lights.length * 2)
        );
    }

    private void growMeshes() {
        meshes = Arrays.copyOf(
                meshes,
                Math.max(1, meshes.length * 2)
        );
    }

    // =========================================================
    // STORE CAMERA
    // =========================================================

    public Camera storeCamera(
            Vector3 coordinates,
            Vector2 rotation) {

        int index = findNextCameraIndex();

        if (index == -1) {
            growCameras();
            index = findNextCameraIndex();
        }

        Camera camera = new Camera(
                rendererPanel,
                coordinates,
                rotation
        );

        cameras[index] = camera;

        return camera;
    }

    // =========================================================
    // STORE LIGHT
    // =========================================================

    public Light storeLight(
            String lightType,
            double lightStrength,
            Vector3 coordinates,
            Vector3 rotation) {

        int index = findNextLightIndex();

        if (index == -1) {
            growLights();
            index = findNextLightIndex();
        }

        Light light = new Light(
                lightType,
                lightStrength,
                coordinates,
                rotation
        );

        lights[index] = light;

        return light;
    }

    // =========================================================
    // STORE OBJECT / MESH
    // =========================================================

    public Mesh storeObject(String objPath) {

        int index = findNextMeshIndex();

        if (index == -1) {
            growMeshes();
            index = findNextMeshIndex();
        }

        try {
            Mesh mesh = new Object(this, objPath);
            mesh.generate();

            meshes[index] = mesh;

            return mesh;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public Mesh getMesh(int index) {
        if (index < 0 || index >= meshes.length) {
            return null;
        }

        return meshes[index];
    }

    public Camera getActiveCamera() {
        if (activeCameraIndex < 0
                || activeCameraIndex >= cameras.length) {
            return null;
        }

        return cameras[activeCameraIndex];
    }

    public int getMeshCount() {
        int count = 0;

        for (Mesh mesh : meshes) {
            if (mesh != null) {
                count++;
            }
        }

        return count;
    }

    public int getLightCount() {
        int count = 0;

        for (Light light : lights) {
            if (light != null) {
                count++;
            }
        }

        return count;
    }

    public int getCameraCount() {
        int count = 0;

        for (Camera camera : cameras) {
            if (camera != null) {
                count++;
            }
        }

        return count;
    }
}