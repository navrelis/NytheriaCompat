package dev.nytheria.compat.util;

/**
 * Nytheria Compat: the camera roll that NeoForge mods set through {@code ViewportEvent.ComputeCameraAngles} and
 * {@code Camera#setRotation(float, float, float)}, as it reaches the camera's rotation.
 *
 * <p>The roll is in degrees, like yaw and pitch. The third argument of {@code Quaternionf#rotationYXZ} in vanilla's
 * {@code Camera#setRotation} is in radians, and NeoForge passes {@code -roll * (float) (Math.PI / 180.0)} there: it
 * converts the unit and also negates the value. Vanilla
 * passes 0 there, so the roll is added to whatever the argument already is, which keeps other mods' changes to the same
 * argument working.
 */
public final class CameraRoll {
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    private CameraRoll() {
    }

    /** The angle in radians that {@code rollDegrees} adds to the third {@code rotationYXZ} argument. */
    public static float toAngle(float rollDegrees) {
        return -rollDegrees * DEGREES_TO_RADIANS;
    }

    /** The third {@code rotationYXZ} argument with the roll applied: what NeoForge passes, plus what is already there. */
    public static float addRoll(float angleRadians, float rollDegrees) {
        return angleRadians + toAngle(rollDegrees);
    }
}
