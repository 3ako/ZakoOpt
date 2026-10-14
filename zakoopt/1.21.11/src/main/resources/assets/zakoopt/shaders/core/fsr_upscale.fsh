#version 330

#moj_import <minecraft:globals.glsl>

// one-pass upscale: bilinear taps a source texel apart, sharpened with FSR 1 RCAS weights
uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// exp2(-0.2): FSR's default strength of 0.2 stops
const float SHARPNESS = 0.8706;

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize, t = 1.0 / vec2(textureSize(InSampler, 0));
    vec3 b = texture(InSampler, uv - vec2(0.0, t.y)).rgb;
    vec3 d = texture(InSampler, uv - vec2(t.x, 0.0)).rgb;
    vec3 e = texture(InSampler, uv).rgb;
    vec3 f = texture(InSampler, uv + vec2(t.x, 0.0)).rgb;
    vec3 h = texture(InSampler, uv + vec2(0.0, t.y)).rgb;
    vec3 mn4 = min(min(b, d), min(f, h)), mx4 = max(max(b, d), max(f, h));
    vec3 hitMin = min(mn4, e) / max(4.0 * mx4, vec3(1.0 / 65536.0));
    vec3 hitMax = (1.0 - max(mx4, e)) / min(4.0 * mn4 - 4.0, vec3(-1.0 / 65536.0));
    vec3 lobeRGB = max(-hitMin, hitMax);
    // 0.25 - 1/16: FSR_RCAS_LIMIT, keeps the kernel from inverting
    float lobe = max(-0.1875, min(max(lobeRGB.r, max(lobeRGB.g, lobeRGB.b)), 0.0)) * SHARPNESS;
    fragColor = vec4((lobe * (b + d + f + h) + e) / (4.0 * lobe + 1.0), 1.0);
}
