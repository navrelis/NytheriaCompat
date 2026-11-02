package dev.nytheria.compat.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link CameraRoll} against the expression in NeoForge's {@code Camera} patch
 * ({@code -roll * (float) (Math.PI / 180.0)}). Plain unit tests, no game classes.
 */
public class CameraRollTest {
    /** The third argument of {@code rotationYXZ} exactly as NeoForge writes it. */
    private static float neoForge(float roll) {
        return -roll * (float) (Math.PI / 180.0);
    }

    @Test
    public void noRollChangesNothing() {
        assertEquals(0.0F, CameraRoll.addRoll(0.0F, 0.0F), 0.0F);
        assertEquals(0.25F, CameraRoll.addRoll(0.25F, 0.0F), 0.0F);
    }

    @Test
    public void rollIsConvertedFromDegreesToRadians() {
        // Cataclysm's strongest common shake: 7.5 degrees.
        assertEquals(-0.1309F, CameraRoll.addRoll(0.0F, 7.5F), 0.0001F);
        assertEquals(-(float) Math.PI, CameraRoll.addRoll(0.0F, 180.0F), 1.0E-6F);
        assertEquals(-(float) Math.PI / 2.0F, CameraRoll.addRoll(0.0F, 90.0F), 1.0E-6F);
        // Not 57.3 times too large, as it was when the degrees were taken as radians.
        assertTrue(Math.abs(CameraRoll.addRoll(0.0F, 7.5F)) < 0.2F);
    }

    @Test
    public void positiveRollTurnsTheAngleNegative() {
        assertTrue(CameraRoll.addRoll(0.0F, 10.0F) < 0.0F);
        assertTrue(CameraRoll.addRoll(0.0F, -10.0F) > 0.0F);
        assertEquals(-CameraRoll.addRoll(0.0F, 33.0F), CameraRoll.addRoll(0.0F, -33.0F), 0.0F);
    }

    @Test
    public void incomingValueIsKept() {
        // Another mod's change to the same argument is still there.
        float other = 0.3F;
        assertEquals(other + neoForge(12.0F), CameraRoll.addRoll(other, 12.0F), 0.0F);
        assertEquals(-0.1F + neoForge(-4.0F), CameraRoll.addRoll(-0.1F, -4.0F), 0.0F);
        assertEquals(other, CameraRoll.addRoll(other, 0.0F), 0.0F);
    }

    @Test
    public void equalsNeoForgeForEveryInput() {
        for (int i = -7200; i <= 7200; i++) {
            float roll = i * 0.05F;
            assertEquals(neoForge(roll), CameraRoll.toAngle(roll), 0.0F, "roll " + roll);
            assertEquals(0.0F + neoForge(roll), CameraRoll.addRoll(0.0F, roll), 0.0F, "roll " + roll);
        }
    }
}
