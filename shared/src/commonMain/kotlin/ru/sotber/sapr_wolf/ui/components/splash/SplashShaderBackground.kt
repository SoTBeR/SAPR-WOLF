package ru.sotber.sapr_wolf.ui.components.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cross-platform AGSL / SkSL shader rendering an animated monochrome liquid metal surface
 * (domain-warped FBM height field with studio chrome reflections and Fresnel rim highlights),
 * adapted to dark and light themes.
 */
internal const val LIQUID_METAL_SHADER: String = """
uniform float2 iResolution;
uniform float iTime;
uniform float iDarkTheme;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    // Quintic smoothstep eliminates grid-line crease artifacts in derivatives
    float2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float2 rotOctave(float2 p) {
    return float2(p.x * 0.80 - p.y * 0.60, p.x * 0.60 + p.y * 0.80);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.55;
    v += a * valueNoise(p);
    p = rotOctave(p) * 1.92 + float2(13.7, 7.1);
    a *= 0.45;
    v += a * valueNoise(p);
    p = rotOctave(p) * 1.95 + float2(5.3, 19.4);
    a *= 0.40;
    v += a * valueNoise(p);
    return v;
}

float liquidHeight(float2 p, float t) {
    float2 q = float2(
        fbm(p * 0.85 + float2(t * 0.12, -t * 0.08)),
        fbm(p * 0.85 + float2(5.2 - t * 0.09, 1.3 + t * 0.11))
    );
    float2 r = float2(
        fbm(p * 0.90 + 1.15 * q + float2(1.7 + t * 0.13, 9.2 - t * 0.08)),
        fbm(p * 0.90 + 1.15 * q + float2(8.3 - t * 0.10, 2.8 + t * 0.12))
    );
    float ripples = 0.10 * sin(p.x * 1.9 + r.x * 2.6 + t * 0.45)
                  + 0.09 * cos(p.y * 1.7 - r.y * 2.4 - t * 0.38);
    return fbm(p * 0.82 + 1.05 * r) + ripples;
}

float3 studioChromeEnv(float3 refl, float t, float darkMode) {
    // Procedural studio softboxes for crisp liquid-mercury reflections
    float angle = refl.x * 2.1 + refl.y * 1.5 + sin(t * 0.22) * 0.30;
    float bandA = smoothstep(0.22, 0.86, sin(angle * 1.9 + refl.z * 1.6) * 0.5 + 0.5);
    float bandB = smoothstep(0.58, 0.98, cos(refl.x * 3.4 - refl.y * 2.6 - t * 0.25) * 0.5 + 0.5);
    float horizon = smoothstep(-0.35, 0.55, refl.y * 0.75 + refl.x * 0.35);

    // Dark titanium / obsidian mercury palette
    float3 darkDeep = float3(0.032, 0.035, 0.044);
    float3 darkMid  = float3(0.145, 0.155, 0.182);
    float3 darkHi   = float3(0.66, 0.69, 0.75);
    float3 darkPeak = float3(0.90, 0.92, 0.96);

    float3 darkEnv = mix(darkDeep, darkMid, horizon);
    darkEnv = mix(darkEnv, darkHi, bandA * 0.58);
    darkEnv = mix(darkEnv, darkPeak, bandB * 0.48);

    // Light platinum / silver mercury palette
    float3 lightDeep = float3(0.58, 0.60, 0.65);
    float3 lightMid  = float3(0.83, 0.84, 0.88);
    float3 lightHi   = float3(0.95, 0.96, 0.98);
    float3 lightPeak = float3(1.00, 1.00, 1.00);

    float3 lightEnv = mix(lightDeep, lightMid, horizon);
    lightEnv = mix(lightEnv, lightHi, bandA * 0.70);
    lightEnv = mix(lightEnv, lightPeak, bandB * 0.65);

    return mix(lightEnv, darkEnv, darkMode);
}

half4 main(float2 fragCoord) {
    float minDim = max(min(iResolution.x, iResolution.y), 1.0);
    float2 uv = fragCoord / max(iResolution, float2(1.0));
    float2 p = (fragCoord - 0.5 * iResolution) / minDim * 1.85;

    float t = iTime;
    float darkMode = clamp(iDarkTheme, 0.0, 1.0);

    // Surface normal via central differences
    float eps = 0.018;
    float hC = liquidHeight(p, t);
    float hX = liquidHeight(p + float2(eps, 0.0), t) - liquidHeight(p - float2(eps, 0.0), t);
    float hY = liquidHeight(p + float2(0.0, eps), t) - liquidHeight(p - float2(0.0, eps), t);

    float3 normal = normalize(float3(-hX * 2.8, -hY * 2.8, 1.0));
    float3 viewDir = float3(0.0, 0.0, 1.0);
    float3 refl = reflect(-viewDir, normal);

    // Studio environment reflection
    float3 col = studioChromeEnv(refl, t, darkMode);

    // Dual studio key & rim specular lights
    float3 keyLight = normalize(float3(0.55 + 0.25 * cos(t * 0.30), -0.48 + 0.18 * sin(t * 0.25), 0.78));
    float3 rimLight = normalize(float3(-0.62, 0.52, 0.58));

    float specKey = pow(max(dot(normal, normalize(keyLight + viewDir)), 0.0), 34.0);
    float specBroad = pow(max(dot(normal, normalize(keyLight + viewDir)), 0.0), 8.0);
    float specRim = pow(max(dot(normal, normalize(rimLight + viewDir)), 0.0), 20.0);

    // Fresnel metallic grazing sheen
    float fresnel = pow(1.0 - clamp(dot(normal, viewDir), 0.0, 1.0), 3.0);

    float3 specTint = mix(float3(0.92, 0.94, 0.98), float3(0.96, 0.98, 1.00), darkMode);
    col += specTint * (specKey * mix(0.32, 0.68, darkMode) + specBroad * mix(0.08, 0.16, darkMode));
    col += float3(0.82, 0.86, 0.93) * specRim * mix(0.14, 0.34, darkMode);
    col = mix(col, mix(float3(0.98, 0.99, 1.0), float3(0.76, 0.80, 0.88), darkMode), fresnel * 0.42);

    // Subtle ridge depth shading from height field
    col *= mix(0.92, 1.06, clamp(hC, 0.0, 1.0));

    // Gentle vignette toward bottom-left for clean text contrast
    float textVignette = smoothstep(0.0, 0.95, length(uv - float2(0.16, 0.84)));
    float vignetteFactor = mix(mix(0.90, 1.0, textVignette), mix(0.64, 1.0, textVignette), darkMode);
    col *= vignetteFactor;

    return half4(clamp(col, 0.0, 1.0), 1.0);
}
"""

@Composable
internal fun rememberSplashShaderTime(): Float {
    var elapsedSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startNanos = -1L
        while (true) {
            withFrameNanos { frameNanos ->
                if (startNanos < 0L) {
                    startNanos = frameNanos
                }
                elapsedSeconds = ((frameNanos - startNanos) / 1_000_000_000.0).toFloat()
            }
        }
    }

    return elapsedSeconds
}

@Composable
expect fun SplashShaderBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
)

/**
 * Software fallback used on platforms without runtime shader support (e.g. Android < API 33).
 */
@Composable
internal fun SplashShaderFallback(
    isDarkTheme: Boolean,
    timeSeconds: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val driftX = cos(timeSeconds * 0.45f) * size.width * 0.18f
        val driftY = sin(timeSeconds * 0.35f) * size.height * 0.18f

        val baseColors = if (isDarkTheme) {
            listOf(Color(0xFF2B2E36), Color(0xFF14161C), Color(0xFF0A0B0E))
        } else {
            listOf(Color(0xFFFFFFFF), Color(0xFFE4E6ED), Color(0xFFC5C8D3))
        }
        drawRect(
            brush = Brush.linearGradient(
                colors = baseColors,
                start = Offset(size.width * 0.8f + driftX, driftY),
                end = Offset(size.width * 0.15f - driftX, size.height - driftY),
            ),
        )

        val sheenCenter = Offset(
            x = size.width * 0.55f + cos(timeSeconds * 0.5f) * size.width * 0.22f,
            y = size.height * 0.38f + sin(timeSeconds * 0.4f) * size.height * 0.20f,
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0x66D8DFEC), Color(0x188C95A8), Color.Transparent)
                } else {
                    listOf(Color(0xAAFFFFFF), Color(0x289DA4B5), Color.Transparent)
                },
                center = sheenCenter,
                radius = size.maxDimension * 0.65f,
            ),
        )
    }
}
