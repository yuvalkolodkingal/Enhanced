#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

// (Enhanced: 1.21.1's colour convolution, as the slow motion effect used it; 1.21.11's has neither uniform)
layout(std140) uniform SlowMotionConfig {
    float Saturation;
    vec3 ColorScale;
};

const vec3 Gray = vec3(0.3, 0.59, 0.11);

out vec4 fragColor;

void main() {
    vec4 InTexel = texture(InSampler, texCoord);
    vec3 OutColor = InTexel.rgb * ColorScale;
    float Luma = dot(OutColor, Gray);
    vec3 Chroma = OutColor - Luma;
    OutColor = (Chroma * Saturation) + Luma;
    fragColor = vec4(OutColor, 1.0);
}
