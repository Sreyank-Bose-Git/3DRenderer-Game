# 🎮 3DRenderer-Game

A lightweight Java 3D terrain exec.renderer and exploration prototype built with Swing and custom software rendering logic.

## ✨ Highlights

- Custom triangle, line, and vertex rasterization pipeline
- Z-buffer based depth handling
- Mouse-look camera with WASD + jump movement
- Procedurally generated terrain with texturing
- Background audio playback support

## 🕹️ Controls

| Action | Key / Input |
| --- | --- |
| Move forward/backward | `W` / `S` |
| Strafe left/right | `A` / `D` |
| Jump | `Space` |
| Toggle mouse unlock | `E` |
| Look around | Mouse (after click-lock) |

## 🏗️ Project Structure

```text
src/
  types.datatypes/   # Core math and pixel data models
  exec.extras/      # Audio support
  exec.handler/     # Keyboard, mouse, collision handling
  main/        # Entry and rendering scripts
  types.objects/     # Scene primitives (Vertex, Line, Triangle)
  exec.renderer/    # Camera, world, and render pipeline
  exec.visuals/     # Texture loading and management
res/
  audio/
  textures/
```

## 🚀 Getting Started

### Prerequisites

- Java 17+ (JDK)
- IntelliJ IDEA (recommended) or any Java IDE

### Run from IDE

1. Open the project in your IDE.
2. Mark `src` as the source root if required.
3. Run the `main.Main` entry class.

### Compile from Terminal

```bash
javac -d out $(find src -name "*.java")
```

## 🔒 Security and Community

- Security policy: see [SECURITY.md](SECURITY.md)
- Contribution guidelines: see [CONTRIBUTING.md](CONTRIBUTING.md)
- Community standards: see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
