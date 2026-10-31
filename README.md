# OpenStreets

<p align="center">
  <img src="res/images/brand/OpenStreets.png" alt="OpenStreets logo" width="180">
</p>

<p align="center">
  A small Java exploration game built from the ground up with Swing and a custom software renderer.
</p>

OpenStreets is an in-progress 3D game prototype focused on learning how a renderer, camera, world, lighting, input, audio, and game UI fit together. There is no graphics engine hiding underneath the project: the scene is transformed and rasterized by the code in this repository.

## What is here?

- **Custom software rendering** for vertices, lines, and textured triangles
- **Depth buffering** with per-pixel color output
- **Mesh loading** from Wavefront `.obj` and `.mtl` resources
- **Camera movement** with mouse look, WASD movement, jumping, and camera switching
- **World composition** through collections, meshes, lights, and an active player
- **Lighting and smooth normals** calculated during the render pass
- **Resource-backed presentation** with a logo intro, Swing UI, block textures, and looping background music

## Controls

| Action | Input |
| --- | --- |
| Move | `W` / `A` / `S` / `D` |
| Jump | `Space` |
| Look around | Mouse |
| Unlock or lock the mouse | `E` |
| Previous camera | `O` |
| Next camera | `P` |

Click **Play** on the home screen to enter the world. Press **Exit** to return to the title screen.

## Rendering details

The renderer runs on a fixed **1280 × 720** logical canvas at a target **60 FPS**. Each frame:

1. The world resolves collection transforms and updates entities.
2. Mesh vertices are scaled, rotated, and translated into world space.
3. Camera projection and near/far clipping convert geometry into screen space.
4. Triangles, lines, and vertices are rasterized into a `BufferedImage`.
5. A z-buffer keeps the closest fragment while the lighting system shades visible geometry.
6. The finished frame and the game interface are drawn through Swing.

The reusable mesh cache avoids rebuilding render data every frame, while the renderer keeps the geometry buffers owned by each mesh rather than by the world.

## Repository layout

```text
src/
  animations/       # Loading and logo presentation
  entities/         # Player and game entities
  exec/
    extras/         # Audio and collection helpers
    file/           # OBJ and MTL parsers
    handler/        # Keyboard, mouse, and collision handling
    renderer/       # Camera, world, and render pipeline
    visuals/        # Texture loading and lighting
  main/             # Application entry point, scripts, and UI
  types/            # Math, geometry, mesh, entity, and texture models
res/
  audio/music/      # Background music
  images/brand/     # OpenStreets logo
  types/            # OBJ and MTL scene resources
  visuals/textures/ # Block textures
```

## Run locally

### Requirements

- Java Development Kit 17 or newer
- IntelliJ IDEA or another Java IDE (optional)

### From an IDE

1. Open the repository.
2. Keep `src` as the source root and `res` as the resources root.
3. Run `main.Main`.

### From a terminal

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out:res main.Main
```

On Windows, use `;` instead of `:` in the final classpath.

## Project standards

See the repository guidance for [contributing](CONTRIBUTING.md), [security](SECURITY.md), and the [code of conduct](CODE_OF_CONDUCT.md).

## License

OpenStreets is available under the MIT License. See [LICENSE](LICENSE).
