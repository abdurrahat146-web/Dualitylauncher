// ============================================================
//  SHELTER PROTOCOL
//  A solo prototype inspired by bunker-survival / social
//  deduction "vote someone out" games. Original code and
//  original design - not a copy of any commercial title.
// ============================================================

#include <glad/gl.h>
#include <GLFW/glfw3.h>

#include <iostream>
#include <string>
#include <vector>
#include <random>
#include <sstream>
#include <iomanip>

#include "game_types.h"
#include "shader.h"
#include "mini_math.h"
#include "mesh.h"

// ----------------------------------------------------------
//  Globals (kept simple and explicit for a single-file-ish
//  prototype; easy to split into classes later as it grows)
// ----------------------------------------------------------
static Settings g_settings;
static GameState g_state = GameState::MainMenu;
static ShelterStatus g_shelter;
static std::vector<Survivor> g_survivors;
static int g_selectedMenuIndex = 0;
static int g_selectedVoteIndex = 0;
static std::string g_statusMessage;
static double g_lastFrameTime = 0.0;
static float g_cameraAngle = 0.0f;
static bool g_keyPressedLastFrame[GLFW_KEY_LAST + 1] = { false };

// Forward declarations
void startNewGame();
void advanceDay();
void applyWindowSettings(GLFWwindow* window);
bool keyJustPressed(GLFWwindow* window, int key);

// ----------------------------------------------------------
//  Simple text rendering using GL_LINES "vector font" so we
//  don't need to bundle font files or a text library. This
//  keeps the whole project dependency-free beyond GLFW/GLAD.
// ----------------------------------------------------------
#include "vector_text.h"

// ----------------------------------------------------------
//  Input helper: detects a fresh key press (not held down)
// ----------------------------------------------------------
bool keyJustPressed(GLFWwindow* window, int key)
{
    bool isDown = glfwGetKey(window, key) == GLFW_PRESS;
    bool wasDown = g_keyPressedLastFrame[key];
    g_keyPressedLastFrame[key] = isDown;
    return isDown && !wasDown;
}

// ----------------------------------------------------------
//  Game logic: start a new run
// ----------------------------------------------------------
void startNewGame()
{
    g_shelter = ShelterStatus();
    g_survivors.clear();

    g_survivors.push_back({ "You", 0, 60, true, true });
    g_survivors.push_back({ "Marcus", 20, 55, true, false });
    g_survivors.push_back({ "Elena", 35, 70, true, false });
    g_survivors.push_back({ "Dmitri", 50, 40, true, false });
    g_survivors.push_back({ "Priya", 15, 65, true, false });

    g_statusMessage = "Day 1. The shelter is sealed. Resources are limited.";
    g_state = GameState::Playing;
}

// ----------------------------------------------------------
//  Game logic: resource tick + trigger a vote if scarce
// ----------------------------------------------------------
void advanceDay()
{
    g_shelter.daysSurvived++;

    int aliveCount = 0;
    for (auto& s : g_survivors) if (s.alive) aliveCount++;

    // Resources drain faster with more mouths to feed.
    g_shelter.oxygenLevel -= 5 + aliveCount;
    g_shelter.foodLevel   -= 6 + aliveCount;
    g_shelter.powerLevel  -= 4;

    if (g_shelter.oxygenLevel < 0) g_shelter.oxygenLevel = 0;
    if (g_shelter.foodLevel < 0) g_shelter.foodLevel = 0;
    if (g_shelter.powerLevel < 0) g_shelter.powerLevel = 0;

    std::mt19937 rng(std::random_device{}());
    std::uniform_int_distribution<int> suspicionRoll(-5, 10);
    for (auto& s : g_survivors)
    {
        if (s.alive && !s.isPlayer)
        {
            s.suspicion += suspicionRoll(rng);
            if (s.suspicion < 0) s.suspicion = 0;
            if (s.suspicion > 100) s.suspicion = 100;
        }
    }

    // Resources critical -> forces a vote to reduce population
    bool resourcesCritical = (g_shelter.oxygenLevel < 30 || g_shelter.foodLevel < 25);

    if (aliveCount <= g_shelter.survivorsRequired)
    {
        g_state = GameState::GameOver;
        g_statusMessage = "The shelter stabilized. You are among the survivors.";
    }
    else if (resourcesCritical)
    {
        g_state = GameState::VoteScreen;
        g_statusMessage = "Resources critical. The group must vote someone out.";
        g_selectedVoteIndex = 0;
    }
    else
    {
        std::ostringstream oss;
        oss << "Day " << g_shelter.daysSurvived << ". O2: " << g_shelter.oxygenLevel
            << "%  Food: " << g_shelter.foodLevel << "%  Power: " << g_shelter.powerLevel << "%";
        g_statusMessage = oss.str();
    }
}

// ----------------------------------------------------------
//  Apply resolution / fullscreen / vsync settings live
// ----------------------------------------------------------
void applyWindowSettings(GLFWwindow* window)
{
    GLFWmonitor* monitor = g_settings.fullscreen ? glfwGetPrimaryMonitor() : nullptr;

    if (g_settings.fullscreen)
    {
        const GLFWvidmode* mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowMonitor(window, monitor, 0, 0, mode->width, mode->height, mode->refreshRate);
    }
    else
    {
        glfwSetWindowMonitor(window, nullptr, 100, 100, g_settings.windowWidth, g_settings.windowHeight, 0);
    }

    glfwSwapInterval(g_settings.vsync ? 1 : 0);
}

// ----------------------------------------------------------
//  Quality presets: these actually change render cost, so a
//  weaker PC can turn things down and get real performance back.
// ----------------------------------------------------------
void applyQualityPreset()
{
    switch (g_settings.quality)
    {
        case GraphicsQuality::Low:
            g_settings.shadowResolution = 256;
            g_settings.showReflections = false;
            break;
        case GraphicsQuality::Medium:
            g_settings.shadowResolution = 512;
            g_settings.showReflections = true;
            break;
        case GraphicsQuality::High:
            g_settings.shadowResolution = 1024;
            g_settings.showReflections = true;
            break;
    }
}

int main()
{
    // --------------------------------------------------
    //  GLFW / window setup
    // --------------------------------------------------
    if (!glfwInit())
    {
        std::cerr << "Failed to initialize GLFW" << std::endl;
        return -1;
    }

    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
#ifdef __APPLE__
    glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
#endif

    GLFWwindow* window = glfwCreateWindow(
        g_settings.windowWidth, g_settings.windowHeight,
        "SHELTER PROTOCOL", nullptr, nullptr);

    if (!window)
    {
        std::cerr << "Failed to create GLFW window" << std::endl;
        glfwTerminate();
        return -1;
    }

    glfwMakeContextCurrent(window);
    glfwSwapInterval(g_settings.vsync ? 1 : 0);

    if (!gladLoadGL((GLADloadfunc)glfwGetProcAddress))
    {
        std::cerr << "Failed to initialize GLAD" << std::endl;
        return -1;
    }

    glEnable(GL_DEPTH_TEST);

    // --------------------------------------------------
    //  Load shaders
    // --------------------------------------------------
    Shader objectShader;
    if (!objectShader.loadFromFiles("shaders/object.vert", "shaders/object.frag"))
    {
        std::cerr << "Failed to load object shader." << std::endl;
        return -1;
    }

    Shader floorShader;
    if (!floorShader.loadFromFiles("shaders/object.vert", "shaders/floor.frag"))
    {
        std::cerr << "Failed to load floor shader." << std::endl;
        return -1;
    }

    // --------------------------------------------------
    //  Build geometry
    // --------------------------------------------------
    Mesh cubeMesh;
    cubeMesh.uploadAndBuild(generateCubeVertices());

    Mesh floorMesh;
    floorMesh.uploadAndBuild(generatePlaneVertices(8.0f, 8.0f));

    Mesh wallMesh;
    wallMesh.uploadAndBuild(generateCubeVertices());

    applyQualityPreset();

    // --------------------------------------------------
    //  Main loop
    // --------------------------------------------------
    g_lastFrameTime = glfwGetTime();

    while (!glfwWindowShouldClose(window))
    {
        double currentTime = glfwGetTime();
        float deltaTime = (float)(currentTime - g_lastFrameTime);
        g_lastFrameTime = currentTime;

        glfwPollEvents();

        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
        {
            if (g_state == GameState::Playing)
                g_state = GameState::MainMenu;
        }

        // ------------------------------------------------
        //  State-specific input handling
        // ------------------------------------------------
        if (g_state == GameState::MainMenu)
        {
            if (keyJustPressed(window, GLFW_KEY_DOWN)) g_selectedMenuIndex = (g_selectedMenuIndex + 1) % 3;
            if (keyJustPressed(window, GLFW_KEY_UP))   g_selectedMenuIndex = (g_selectedMenuIndex + 2) % 3;

            if (keyJustPressed(window, GLFW_KEY_ENTER))
            {
                if (g_selectedMenuIndex == 0) startNewGame();
                else if (g_selectedMenuIndex == 1) g_state = GameState::Settings;
                else if (g_selectedMenuIndex == 2) glfwSetWindowShouldClose(window, true);
            }
        }
        else if (g_state == GameState::Settings)
        {
            if (keyJustPressed(window, GLFW_KEY_DOWN)) g_selectedMenuIndex = (g_selectedMenuIndex + 1) % 5;
            if (keyJustPressed(window, GLFW_KEY_UP))   g_selectedMenuIndex = (g_selectedMenuIndex + 4) % 5;

            if (keyJustPressed(window, GLFW_KEY_LEFT) || keyJustPressed(window, GLFW_KEY_RIGHT))
            {
                int dir = keyJustPressed(window, GLFW_KEY_RIGHT) ? 1 : -1;

                switch (g_selectedMenuIndex)
                {
                    case 0: // quality
                    {
                        int q = (int)g_settings.quality;
                        q = (q + dir + 3) % 3;
                        g_settings.quality = (GraphicsQuality)q;
                        applyQualityPreset();
                        break;
                    }
                    case 1: // resolution toggle (two presets for simplicity)
                        if (g_settings.windowWidth == 1280) { g_settings.windowWidth = 1920; g_settings.windowHeight = 1080; }
                        else { g_settings.windowWidth = 1280; g_settings.windowHeight = 720; }
                        if (!g_settings.fullscreen) applyWindowSettings(window);
                        break;
                    case 2: // fullscreen toggle
                        g_settings.fullscreen = !g_settings.fullscreen;
                        applyWindowSettings(window);
                        break;
                    case 3: // vsync toggle
                        g_settings.vsync = !g_settings.vsync;
                        glfwSwapInterval(g_settings.vsync ? 1 : 0);
                        break;
                    case 4: // volume
                        g_settings.masterVolume += dir * 0.1f;
                        if (g_settings.masterVolume < 0.0f) g_settings.masterVolume = 0.0f;
                        if (g_settings.masterVolume > 1.0f) g_settings.masterVolume = 1.0f;
                        break;
                }
            }

            if (keyJustPressed(window, GLFW_KEY_ESCAPE) || keyJustPressed(window, GLFW_KEY_BACKSPACE))
            {
                g_state = GameState::MainMenu;
                g_selectedMenuIndex = 0;
            }
        }
        else if (g_state == GameState::Playing)
        {
            if (keyJustPressed(window, GLFW_KEY_SPACE) || keyJustPressed(window, GLFW_KEY_ENTER))
            {
                advanceDay();
            }
            g_cameraAngle += deltaTime * 0.15f; // slow ambient rotation around the bunker
        }
        else if (g_state == GameState::VoteScreen)
        {
            std::vector<int> aliveIndices;
            for (size_t i = 0; i < g_survivors.size(); i++)
                if (g_survivors[i].alive) { aliveIndices.push_back((int)i); }

            if (keyJustPressed(window, GLFW_KEY_DOWN))
                g_selectedVoteIndex = (g_selectedVoteIndex + 1) % (int)aliveIndices.size();
            if (keyJustPressed(window, GLFW_KEY_UP))
                g_selectedVoteIndex = (g_selectedVoteIndex + (int)aliveIndices.size() - 1) % (int)aliveIndices.size();

            if (keyJustPressed(window, GLFW_KEY_ENTER))
            {
                int targetIndex = aliveIndices[g_selectedVoteIndex];
                if (!g_survivors[targetIndex].isPlayer)
                {
                    g_survivors[targetIndex].alive = false;
                    g_statusMessage = g_survivors[targetIndex].name + " was voted out of the shelter.";

                    // Removing a survivor frees up resources immediately.
                    g_shelter.oxygenLevel += 15;
                    g_shelter.foodLevel += 15;

                    g_state = GameState::Playing;
                }
                else
                {
                    g_statusMessage = "The others turn on you instead...";
                    g_state = GameState::GameOver;
                }
            }
        }
        else if (g_state == GameState::GameOver)
        {
            if (keyJustPressed(window, GLFW_KEY_ENTER))
            {
                g_state = GameState::MainMenu;
                g_selectedMenuIndex = 0;
            }
        }

        // ------------------------------------------------
        //  Render
        // ------------------------------------------------
        glViewport(0, 0, g_settings.windowWidth, g_settings.windowHeight);
        glClearColor(0.02f, 0.02f, 0.03f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (g_state == GameState::Playing || g_state == GameState::VoteScreen)
        {
            float aspect = (float)g_settings.windowWidth / (float)g_settings.windowHeight;
            Mat4 projection = Mat4::perspective(45.0f * 3.14159f / 180.0f, aspect, 0.1f, 100.0f);

            float camRadius = 6.0f;
            Vec3 eye(std::sin(g_cameraAngle) * camRadius, 2.5f, std::cos(g_cameraAngle) * camRadius);
            Vec3 target(0.0f, 1.0f, 0.0f);
            Mat4 view = Mat4::lookAt(eye, target, Vec3(0, 1, 0));

            Vec3 lightDir = Vec3::normalize(Vec3(-0.4f, -1.0f, -0.3f));
            Vec3 lightColor(1.0f, 0.95f, 0.85f);
            Vec3 pointLightPos(0.0f, 3.0f, 0.0f);
            Vec3 pointLightColor(0.3f, 0.6f, 1.0f); // cold emergency-light blue

            // ---- Floor (reflective) ----
            floorShader.use();
            Mat4 floorModel; // identity, floor mesh already at y=0
            float normalMat[9];
            floorModel.toNormalMatrix3(normalMat);

            floorShader.setMat4("model", floorModel.m);
            floorShader.setMat4("view", view.m);
            floorShader.setMat4("projection", projection.m);
            floorShader.setMat3("normalMatrix", normalMat);
            floorShader.setVec3("viewPos", eye.x, eye.y, eye.z);
            floorShader.setVec3("lightDir", lightDir.x, lightDir.y, lightDir.z);
            floorShader.setVec3("lightColor", lightColor.x, lightColor.y, lightColor.z);
            floorMesh.draw();

            // ---- Bunker walls (cubes arranged in a ring) ----
            objectShader.use();
            objectShader.setMat4("view", view.m);
            objectShader.setMat4("projection", projection.m);
            objectShader.setVec3("viewPos", eye.x, eye.y, eye.z);
            objectShader.setVec3("lightDir", lightDir.x, lightDir.y, lightDir.z);
            objectShader.setVec3("lightColor", lightColor.x, lightColor.y, lightColor.z);
            objectShader.setVec3("pointLightPos", pointLightPos.x, pointLightPos.y, pointLightPos.z);
            objectShader.setVec3("pointLightColor", pointLightColor.x, pointLightColor.y, pointLightColor.z);
            objectShader.setFloat("reflectivity", g_settings.showReflections ? 0.25f : 0.0f);
            objectShader.setVec3("objectColor", 0.35f, 0.36f, 0.4f);

            const int wallCount = 8;
            for (int i = 0; i < wallCount; i++)
            {
                float angle = (float)i / wallCount * 2.0f * 3.14159f;
                float radius = 7.5f;
                Vec3 pos(std::sin(angle) * radius, 1.5f, std::cos(angle) * radius);

                Mat4 model = Mat4::multiply(Mat4::translate(pos),
                             Mat4::multiply(Mat4::rotateY(-angle), Mat4::scale(Vec3(0.3f, 3.0f, 4.0f))));
                float wallNormalMat[9];
                model.toNormalMatrix3(wallNormalMat);

                objectShader.setMat4("model", model.m);
                objectShader.setMat3("normalMatrix", wallNormalMat);
                wallMesh.draw();
            }

            // ---- Survivor markers (small reflective cubes) ----
            objectShader.setFloat("reflectivity", g_settings.showReflections ? 0.5f : 0.0f);
            int aliveDrawn = 0;
            for (size_t i = 0; i < g_survivors.size(); i++)
            {
                if (!g_survivors[i].alive) continue;

                float angle = (float)aliveDrawn / (float)g_survivors.size() * 2.0f * 3.14159f;
                float radius = 3.0f;
                Vec3 pos(std::sin(angle) * radius, 0.5f, std::cos(angle) * radius);

                Mat4 model = Mat4::multiply(Mat4::translate(pos), Mat4::scale(Vec3(0.6f, 1.0f, 0.6f)));
                float survivorNormalMat[9];
                model.toNormalMatrix3(survivorNormalMat);

                if (g_survivors[i].isPlayer)
                    objectShader.setVec3("objectColor", 0.2f, 0.7f, 0.3f);
                else if (g_state == GameState::VoteScreen && aliveDrawn == g_selectedVoteIndex)
                    objectShader.setVec3("objectColor", 0.8f, 0.2f, 0.2f);
                else
                    objectShader.setVec3("objectColor", 0.6f, 0.55f, 0.5f);

                objectShader.setMat4("model", model.m);
                objectShader.setMat3("normalMatrix", survivorNormalMat);
                cubeMesh.draw();

                aliveDrawn++;
            }
        }

        // ------------------------------------------------
        //  UI overlay (vector text, drawn in screen space)
        // ------------------------------------------------
        drawUIForState(window, g_state, g_settings, g_shelter, g_survivors,
                        g_selectedMenuIndex, g_selectedVoteIndex, g_statusMessage);

        glfwSwapBuffers(window);
    }

    cubeMesh.destroy();
    floorMesh.destroy();
    wallMesh.destroy();

    glfwTerminate();
    return 0;
}
