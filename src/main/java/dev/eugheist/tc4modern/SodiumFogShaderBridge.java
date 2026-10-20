package dev.eugheist.tc4modern;

import java.util.regex.Pattern;

/** Adds TC4's EXP branch while leaving Sodium's own shader and ordinary fog intact. */
public final class SodiumFogShaderBridge {
    private static final String NAME = "([A-Za-z_][A-Za-z_0-9]*)";
    private static final Pattern FUNCTION = Pattern.compile("vec4\\s+_linearFog\\s*\\(\\s*vec4\\s+" + NAME
        + "\\s*,\\s*float\\s+" + NAME + "\\s*,\\s*vec4\\s+" + NAME + "\\s*,\\s*float\\s+" + NAME
        + "\\s*,\\s*float\\s+" + NAME + "\\s*\\)\\s*\\{");
    private SodiumFogShaderBridge() {}
    public static String apply(String shader) {
        if (shader.contains("TC4_EXP_FOG")) return shader;
        var function = FUNCTION.matcher(shader);
        if (!function.find()) return shader; // Unknown Sodium shader API: never rewrite an unrelated function.
        String color = function.group(1), distance = function.group(2), fog = function.group(3);
        String start = function.group(4), end = function.group(5);
        String branch = "\n// TC4_EXP_FOG: positive marker keeps section culling valid.\n#ifdef USE_FOG\n"
            + "if (" + end + " == 16384.0 && " + start + " < 0.0) {\n"
            + "float tc4Visibility = clamp(exp(-max(" + distance + ", 0.0) * -" + start + "), 0.0, 1.0);\n"
            + "return vec4(mix(" + fog + ".rgb, " + color + ".rgb, tc4Visibility), " + color + ".a);\n}\n#endif\n";
        return shader.substring(0, function.end()) + branch + shader.substring(function.end());
    }
}
