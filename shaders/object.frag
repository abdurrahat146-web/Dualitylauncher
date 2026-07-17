#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

out vec4 FragColor;

uniform vec3 viewPos;
uniform vec3 objectColor;

// Simple directional light (sun-like)
uniform vec3 lightDir;
uniform vec3 lightColor;

// Point light (adds a bit of local color/highlight)
uniform vec3 pointLightPos;
uniform vec3 pointLightColor;

// How reflective the surface is (0 = matte, 1 = mirror-like)
uniform float reflectivity;

// A simple procedural "sky" color used to fake environment reflections
// without needing a real cubemap texture.
vec3 fakeEnvironment(vec3 reflectDir)
{
    // Blend between a ground color and sky color based on the
    // vertical component of the reflection vector.
    vec3 skyColor = vec3(0.55, 0.75, 0.95);
    vec3 groundColor = vec3(0.15, 0.13, 0.12);
    float t = clamp(reflectDir.y * 0.5 + 0.5, 0.0, 1.0);
    return mix(groundColor, skyColor, t);
}

void main()
{
    vec3 norm = normalize(Normal);

    // ---- Ambient ----
    float ambientStrength = 0.15;
    vec3 ambient = ambientStrength * lightColor;

    // ---- Directional diffuse ----
    vec3 dirToLight = normalize(-lightDir);
    float diff = max(dot(norm, dirToLight), 0.0);
    vec3 diffuse = diff * lightColor;

    // ---- Directional specular (Blinn-Phong) ----
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 halfwayDir = normalize(dirToLight + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), 64.0);
    vec3 specular = spec * lightColor * 0.8;

    // ---- Point light contribution ----
    vec3 toPoint = pointLightPos - FragPos;
    float dist = length(toPoint);
    vec3 pointDir = normalize(toPoint);
    float pointDiff = max(dot(norm, pointDir), 0.0);
    float attenuation = 1.0 / (1.0 + 0.09 * dist + 0.032 * dist * dist);
    vec3 pointDiffuse = pointDiff * pointLightColor * attenuation;

    vec3 pointHalfway = normalize(pointDir + viewDir);
    float pointSpec = pow(max(dot(norm, pointHalfway), 0.0), 64.0);
    vec3 pointSpecular = pointSpec * pointLightColor * attenuation;

    // ---- Fake reflection term ----
    vec3 reflectDir = reflect(-viewDir, norm);
    vec3 envColor = fakeEnvironment(reflectDir);

    vec3 lit = (ambient + diffuse + pointDiffuse) * objectColor
             + specular + pointSpecular;

    // Blend the lit surface color with the reflected "environment"
    // based on the reflectivity of the material.
    vec3 finalColor = mix(lit, envColor, reflectivity);

    FragColor = vec4(finalColor, 1.0);
}
