package exec.visuals;

import types.things.Light;
import types.things.Thing;

import java.awt.Color;

public class LightManager {

    public double ambientStrength = 0.2;

    private static final double EPSILON_SQUARED = 1e-20;

    /**
     * Per-pixel lighting overload. nx, ny and nz should be a normalized normal.
     * Uses Lambert diffuse lighting and distance attenuation for point lights.
     */
    public double calculateIntensity(
            double px, double py, double pz,
            double nx, double ny, double nz,
            Thing[] things) {

        double intensity = Math.max(0.0, ambientStrength);
        if (things == null) return clampIntensity(intensity);

        for (Thing thing : things) {
            if (!(thing instanceof Light light)) {
                continue;
            }

            if (light.lightStrength <= 0.0
                    || !Double.isFinite(light.lightStrength)) {
                continue;
            }

            double lx;
            double ly;
            double lz;
            double attenuation = 1.0;

            if ("directional".equalsIgnoreCase(light.lightType)) {
                if (light.actualRotation == null) continue;

                lx = -light.actualRotation.x;
                ly = -light.actualRotation.y;
                lz = -light.actualRotation.z;

                double lengthSquared = lx * lx + ly * ly + lz * lz;
                if (lengthSquared < EPSILON_SQUARED) continue;

                double inverseLength = 1.0 / Math.sqrt(lengthSquared);
                lx *= inverseLength;
                ly *= inverseLength;
                lz *= inverseLength;

            } else if ("point".equalsIgnoreCase(light.lightType)) {
                if (light.actualCoordinates == null) continue;

                lx = light.actualCoordinates.x - px;
                ly = light.actualCoordinates.y - py;
                lz = light.actualCoordinates.z - pz;

                double distanceSquared = lx * lx + ly * ly + lz * lz;

                if (distanceSquared < EPSILON_SQUARED
                        || !Double.isFinite(distanceSquared)) {
                    continue;
                }

                double distance = Math.sqrt(distanceSquared);
                double inverseDistance = 1.0 / distance;

                lx *= inverseDistance;
                ly *= inverseDistance;
                lz *= inverseDistance;

                attenuation = 1.0 /
                        (1.0 + 0.02 * distance + 0.001 * distanceSquared);

            } else {
                continue;
            }

            double diffuse = Math.max(0.0, nx * lx + ny * ly + nz * lz);
            intensity += light.lightStrength * diffuse * attenuation;

            if (intensity >= 1.0) return 1.0;
        }

        return clampIntensity(intensity);
    }

    public Color applyLighting(Color base, double intensity) {
        if (base == null) return Color.BLACK;
        intensity = clampIntensity(intensity);

        return new Color(
                clampChannel((int) Math.round(base.getRed() * intensity)),
                clampChannel((int) Math.round(base.getGreen() * intensity)),
                clampChannel((int) Math.round(base.getBlue() * intensity)),
                base.getAlpha()
        );
    }

    private static double clampIntensity(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.clamp(value, 0.0, 1.0);
    }

    private static int clampChannel(int value) {
        return Math.clamp(value, 0, 255);
    }
}
