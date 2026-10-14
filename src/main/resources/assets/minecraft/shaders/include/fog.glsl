#version 150

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor) {
    // TC4 GL_EXP bridge: negative far-plane sentinel, near-plane encodes -density.
    if (fogEnd == -420.0) {
        float visibility = clamp(exp(-max(vertexDistance, 0.0) * -fogStart), 0.0, 1.0);
        return vec4(mix(fogColor.rgb, inColor.rgb, visibility), inColor.a);
    }
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogEnd, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (fogEnd == -420.0) {
        return clamp(exp(-max(vertexDistance, 0.0) * -fogStart), 0.0, 1.0);
    }
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

float fog_distance(vec3 pos, int shape) {
    if (shape == 0) {
        return length(pos);
    } else {
        float distXZ = length(pos.xz);
        float distY = abs(pos.y);
        return max(distXZ, distY);
    }
}
