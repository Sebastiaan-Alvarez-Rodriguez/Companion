package com.python.companion.util;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

public class ColorUtil {
    //TODO: create own color distance function based on: https://en.wikipedia.org/wiki/Color_difference
    // OR: create line around icon?

    /**
     * Call when wanting to use {@link #deltaECMC(double, double, double, double, double, double, double, double)} with
     * often used values lightness=1 and chroma=2
     */
    private static double deltaECMC(double L1, double a1, double b1, double L2, double a2, double b2) {
        return deltaECMC(L1, a1, b1, L2, a2, b2, 2, 1);
    }


    private static double deltaECMC(double L1, double a1, double b1, double L2, double a2, double b2, double lightness, double chroma) {
        double c1 = Math.sqrt(Math.pow(a1, 2) + Math.pow(b1, 2)),
                c2 = Math.sqrt(Math.pow(a2, 2) + Math.pow(b2, 2)),
                s1 = L1 < 16 ? 0.511 : (0.040975 * L1) / (1 + 0.01765 * L1),
                sc = 0.0638 * c1 / (1 + 0.0131 * c1) + 0.638,
                h1 = Math.toDegrees(Math.atan2(b1, a1) % 360), //Mogelijk b1 en a1 omdraaien
                t = (h1 >= 164 && h1 <= 345) ? 0.56 + Math.abs(0.2 * Math.cos(Math.toRadians(h1+168))) : 0.36 + Math.abs(0.4 * Math.cos(Math.toRadians(h1+35))),
                c4 = Math.pow(c1, 4), //c1 * c1 * c1 * c1,
                f = Math.sqrt(c4 / (c4 + 1900)),
                sh = sc * (f * t + 1 - f),
                deltaL = L1 - L2,
                deltaC = c1 - c2,
                deltaA = a1 - a2,
                deltaB = b1 - b2,
                deltaH2 = Math.pow(deltaA, 2) + Math.pow(deltaB, 2) - Math.pow(deltaC, 2),
                v1 = deltaL / (1 * s1),
                v2 = deltaC / (chroma * sc),
                v3 = sh;
        return Math.sqrt(Math.pow(v1, 2) + Math.pow(v2, 2) + (deltaH2 / (v3 * v3)));
    }

    public static double computeDiff(@ColorInt int a, @ColorInt int b) {
        double[] aLab = new double[3], bLab = new double[3];
        ColorUtils.colorToLAB(a, aLab);
        ColorUtils.colorToLAB(b, bLab);
        return deltaECMC(aLab[0], aLab[1], aLab[2], bLab[0], bLab[1], bLab[2]);
    }
}
