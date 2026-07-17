#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

out vec4 FragColor;

uniform vec3 viewPos;
uniform vec3 lightDir;
uniform vec3 lightColor;

vec3 fakeEnvironment(vec3 reflectDir)
{
    vec3 skyColor = vec3(0.55, 0.75, 0.95);
    vec3 groundColor = vec3(0.05, 0.05, 0.06);
    float t = clamp(reflectDir.y * 0.5 + 0.5, 0.0, 1.0);
    return mix(groundColor, skyColor, t);
}

void main()
{
    vec3 norm = normalize(Normal);
    vec3 viewDir = normalize(viewPos - FragPos);

    // Checkerboard pattern so the floor doesn't look flat/blank
    float checker = mod(floor(TexCoord.x) + floor(TexCoord.y), 2.0);
    vec3 baseColor = mix(vec3(0.12, 0.12, 0.14), vec3(0.20, 0.20, 0.23), checker);

    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    vec3 dirToLight = normalize(-lightDir);
    float diff = max(dot(norm, dirToLight), 0.0);
    vec3 diffuse = diff * lightColor;

    vec3 reflectDir = reflect(-viewDir, norm);
    vec3 envColor = fakeEnvironment(reflectDir);

    // Fresnel-ish term: floor looks more reflective at grazing angles
    float fresnel = pow(1.0 - max(dot(norm, viewDir), 0.0), 3.0);
    float floorReflectivity = mix(0.25, 0.75, fresnel);

    vec3 lit = (ambient + diffuse) * baseColor;
    vec3 finalColor = mix(lit, envColor, floorReflectivity);

    FragColor = vec4(finalColor, 1.0);
}
