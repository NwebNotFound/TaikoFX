package de.thu.taiko2d.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

class Matrix3x3Test {
    private static void assertSame(Transform expected, Matrix3x3 actual, double x, double y) {
        Point2D e = expected.transform(x, y);
        double[] a = actual.transform(x, y);
        assertEquals(e.getX(), a[0], 1e-9);
        assertEquals(e.getY(), a[1], 1e-9);
    }

    @Test
    void translateMatchesAffine() {
        assertSame(Affine.translate(5, -3),
                Matrix3x3.translate(5, -3), 2, 7);
    }

    @Test
    void rotateMatchesAffine() {
        assertSame(Affine.rotate(Math.toDegrees(0.7), 0, 0),
                Matrix3x3.rotate(0.7), 4, 1);
    }

    @Test
    void pivotRotationIsTranslateRotateTranslateBack() {
        // T(p) * R * T(-p): rotating about (10, 20) must leave (10, 20) fixed.
        Matrix3x3 m = Matrix3x3.translate(10, 20)
                .multiply(Matrix3x3.rotate(1.2))
                .multiply(Matrix3x3.translate(-10, -20));
        double[] p = m.transform(10, 20);
        assertEquals(10, p[0], 1e-9);
        assertEquals(20, p[1], 1e-9);
    }

    @Test
    void scaleMatchesAffine() {
        assertSame(Affine.scale(2, 3), Matrix3x3.scale(2, 3), 4, 5);
    }
    @Test
    void shearMatchesAffine() {
        assertSame(Affine.shear(0.5, 0.25), Matrix3x3.shear(0.5, 0.25), 4, 2);
    }
    @Test
    void multiplyOrderMatters() {
        Matrix3x3 translateThenScale = Matrix3x3.translate(5, 2).multiply(Matrix3x3.scale(2, 3));
        Matrix3x3 scaleThenTranslate = Matrix3x3.scale(2, 3).multiply(Matrix3x3.translate(5, 2));

        double[] a = translateThenScale.transform(1, 1);
        double[] b = scaleThenTranslate.transform(1, 1);

        assertEquals(7, a[0], 1e-9);    // T·S: scale first, then move
        assertEquals(5, a[1], 1e-9);
        assertEquals(12, b[0], 1e-9);   // S·T: move first, then scale
        assertEquals(9, b[1], 1e-9);
    }
}
