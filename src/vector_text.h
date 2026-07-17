#pragma once

#include <glad/gl.h>
#include <GLFW/glfw3.h>
#include <string>
#include <vector>
#include <cctype>

#include "game_types.h"

// ============================================================
//  Minimal built-in "vector font" UI renderer.
//  Draws simple blocky glyphs using GL_LINES so the whole
//  project needs zero font files or text libraries.
//  Not pretty typography - functional HUD/menu text.
// ============================================================

namespace VectorText
{
    // Segment-based glyphs on a 3x5 grid (like a simple LED display).
    // Each glyph is a list of line segments: {x1,y1,x2,y2} in glyph space (0-3, 0-5).
    inline std::vector<float> getGlyphSegments(char c)
    {
        c = (char)std::toupper((unsigned char)c);
        switch (c)
        {
            case 'A': return {0,5,0,2, 0,2,1.5f,0, 1.5f,0,3,2, 3,2,3,5, 0,3.2f,3,3.2f};
            case 'B': return {0,0,0,5, 0,0,2,0.5f, 2,0.5f,0,2.5f, 0,2.5f,2.2f,3.5f, 2.2f,3.5f,0,5};
            case 'C': return {3,0.5f,1,0, 1,0,0,1.5f, 0,1.5f,0,3.5f, 0,3.5f,1,5, 1,5,3,4.5f};
            case 'D': return {0,0,0,5, 0,0,1.8f,0.8f, 1.8f,0.8f,3,2.5f, 3,2.5f,1.8f,4.2f, 1.8f,4.2f,0,5};
            case 'E': return {3,0,0,0, 0,0,0,5, 0,5,3,5, 0,2.5f,2,2.5f};
            case 'F': return {0,5,0,0, 0,0,3,0, 0,2.5f,2,2.5f};
            case 'G': return {3,0.5f,1,0, 1,0,0,1.5f, 0,1.5f,0,3.5f, 0,3.5f,1,5, 1,5,3,4.5f, 3,4.5f,3,2.5f, 3,2.5f,1.5f,2.5f};
            case 'H': return {0,0,0,5, 3,0,3,5, 0,2.5f,3,2.5f};
            case 'I': return {1.5f,0,1.5f,5, 0,0,3,0, 0,5,3,5};
            case 'J': return {3,0,3,4, 3,4,1.5f,5, 1.5f,5,0,4};
            case 'K': return {0,0,0,5, 3,0,0,2.5f, 0,2.5f,3,5};
            case 'L': return {0,0,0,5, 0,5,3,5};
            case 'M': return {0,5,0,0, 0,0,1.5f,2, 1.5f,2,3,0, 3,0,3,5};
            case 'N': return {0,5,0,0, 0,0,3,5, 3,5,3,0};
            case 'O': return {1,0,2,0, 2,0,3,1.5f, 3,1.5f,3,3.5f, 3,3.5f,2,5, 2,5,1,5, 1,5,0,3.5f, 0,3.5f,0,1.5f, 0,1.5f,1,0};
            case 'P': return {0,5,0,0, 0,0,2.5f,0.5f, 2.5f,0.5f,2.5f,2, 2.5f,2,0,2.5f};
            case 'Q': return {1,0,2,0, 2,0,3,1.5f, 3,1.5f,3,3.5f, 3,3.5f,2,5, 2,5,1,5, 1,5,0,3.5f, 0,3.5f,0,1.5f, 0,1.5f,1,0, 1.5f,3.5f,3,5};
            case 'R': return {0,5,0,0, 0,0,2.5f,0.5f, 2.5f,0.5f,2.5f,2, 2.5f,2,0,2.5f, 0,2.5f,3,5};
            case 'S': return {3,0.5f,1,0, 1,0,0,1, 0,1,2,2.5f, 2,2.5f,3,3.5f, 3,3.5f,2,5, 2,5,0,4.5f};
            case 'T': return {0,0,3,0, 1.5f,0,1.5f,5};
            case 'U': return {0,0,0,3.5f, 0,3.5f,1.5f,5, 1.5f,5,3,3.5f, 3,3.5f,3,0};
            case 'V': return {0,0,1.5f,5, 1.5f,5,3,0};
            case 'W': return {0,0,0.7f,5, 0.7f,5,1.5f,3, 1.5f,3,2.3f,5, 2.3f,5,3,0};
            case 'X': return {0,0,3,5, 3,0,0,5};
            case 'Y': return {0,0,1.5f,2.5f, 3,0,1.5f,2.5f, 1.5f,2.5f,1.5f,5};
            case 'Z': return {0,0,3,0, 3,0,0,5, 0,5,3,5};
            case '0': return {1,0,2,0, 2,0,3,1.5f, 3,1.5f,3,3.5f, 3,3.5f,2,5, 2,5,1,5, 1,5,0,3.5f, 0,3.5f,0,1.5f, 0,1.5f,1,0, 0,4.5f,3,0.5f};
            case '1': return {0.5f,1,1.5f,0, 1.5f,0,1.5f,5, 0.5f,5,2.5f,5};
            case '2': return {0,1,1,0, 1,0,2.5f,0, 2.5f,0,3,1, 3,1,0,5, 0,5,3,5};
            case '3': return {0,0,3,0, 3,0,1.5f,2.5f, 1.5f,2.5f,3,3, 3,3,3,4, 3,4,2,5, 2,5,0,4.5f};
            case '4': return {2,5,2,0, 2,0,0,3.2f, 0,3.2f,3,3.2f};
            case '5': return {3,0,0,0, 0,0,0,2.5f, 0,2.5f,2.5f,2.5f, 2.5f,2.5f,3,3.5f, 3,3.5f,2,5, 2,5,0,4.5f};
            case '6': return {2.5f,0,1,0.5f, 1,0.5f,0,2, 0,2,0,4, 0,4,1.5f,5, 1.5f,5,3,4, 3,4,3,2.7f, 3,2.7f,1.5f,2.2f, 1.5f,2.2f,0,3};
            case '7': return {0,0,3,0, 3,0,1,5};
            case '8': return {1,0,2,0, 2,0,3,1, 3,1,2,2.5f, 2,2.5f,1,2.5f, 1,2.5f,0,1, 0,1,1,0, 1,2.5f,0,4, 0,4,1,5, 1,5,2,5, 2,5,3,4, 3,4,2,2.5f};
            case '9': return {2,2.8f,0.5f,2.3f, 0.5f,2.3f,0,1, 0,1,1.5f,0, 1.5f,0,3,1, 3,1,3,3.5f, 3,3.5f,1.5f,5, 1.5f,5,0.5f,4.5f};
            case '%': return {0,0,3,5, 0.3f,0.3f,0.7f,0.3f, 2.3f,4.7f,2.7f,4.7f};
            case ':': return {1.5f,1.2f,1.5f,1.5f, 1.5f,3.5f,1.5f,3.8f};
            case '.': return {1.4f,4.6f,1.6f,4.9f};
            case ',': return {1.4f,4.6f,1.2f,5.2f};
            case '-': return {0.3f,2.5f,2.7f,2.5f};
            case '!': return {1.5f,0,1.5f,3.3f, 1.5f,4.6f,1.5f,4.9f};
            case '?': return {0,1,1,0, 1,0,2.5f,0, 2.5f,0,3,1, 3,1,1.5f,2.5f, 1.5f,2.5f,1.5f,3.3f, 1.5f,4.6f,1.5f,4.9f};
            case '\'': return {1.3f,0,1.7f,1};
            case '/': return {3,0,0,5};
            default: return {}; // space and unhandled chars: blank
        }
    }

    inline void drawGlyph(float x, float y, float scale, char c,
                          float r, float g, float b,
                          std::vector<float>& outVertices)
    {
        auto segs = getGlyphSegments(c);
        for (size_t i = 0; i < segs.size(); i += 4)
        {
            float x1 = x + segs[i]     * scale;
            float y1 = y + segs[i + 1] * scale;
            float x2 = x + segs[i + 2] * scale;
            float y2 = y + segs[i + 3] * scale;

            outVertices.push_back(x1); outVertices.push_back(y1);
            outVertices.push_back(r);  outVertices.push_back(g); outVertices.push_back(b);

            outVertices.push_back(x2); outVertices.push_back(y2);
            outVertices.push_back(r);  outVertices.push_back(g); outVertices.push_back(b);
        }
    }

    // Appends line-segment vertex data (screen-space pixels) for a string.
    inline void appendText(const std::string& text, float x, float y, float scale,
                            float r, float g, float b, std::vector<float>& outVertices)
    {
        float cursorX = x;
        const float glyphWidth = 4.0f * scale;
        for (char c : text)
        {
            if (c == ' ') { cursorX += glyphWidth; continue; }
            drawGlyph(cursorX, y, scale, c, r, g, b, outVertices);
            cursorX += glyphWidth;
        }
    }
}

// ============================================================
//  Simple immediate-mode-style 2D line renderer for the UI.
//  Builds a VBO fresh each frame (fine at this UI text volume).
// ============================================================
class UIRenderer
{
public:
    unsigned int VAO = 0, VBO = 0;
    unsigned int shaderProgram = 0;
    bool initialized = false;

    void init()
    {
        const char* vs = R"(
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec3 aColor;
            uniform vec2 screenSize;
            out vec3 vColor;
            void main() {
                vec2 ndc = vec2(
                    (aPos.x / screenSize.x) * 2.0 - 1.0,
                    1.0 - (aPos.y / screenSize.y) * 2.0
                );
                gl_Position = vec4(ndc, 0.0, 1.0);
                vColor = aColor;
            }
        )";

        const char* fs = R"(
            #version 330 core
            in vec3 vColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vColor, 1.0);
            }
        )";

        unsigned int vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, 1, &vs, nullptr);
        glCompileShader(vertex);

        unsigned int fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, 1, &fs, nullptr);
        glCompileShader(fragment);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertex);
        glAttachShader(shaderProgram, fragment);
        glLinkProgram(shaderProgram);

        glDeleteShader(vertex);
        glDeleteShader(fragment);

        glGenVertexArrays(1, &VAO);
        glGenBuffers(1, &VBO);

        initialized = true;
    }

    void render(const std::vector<float>& vertices, int screenWidth, int screenHeight)
    {
        if (!initialized) init();
        if (vertices.empty()) return;

        glDisable(GL_DEPTH_TEST);

        glUseProgram(shaderProgram);
        glUniform2f(glGetUniformLocation(shaderProgram, "screenSize"), (float)screenWidth, (float)screenHeight);

        glBindVertexArray(VAO);
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, vertices.size() * sizeof(float), vertices.data(), GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 5 * sizeof(float), (void*)0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 5 * sizeof(float), (void*)(2 * sizeof(float)));
        glEnableVertexAttribArray(1);

        glLineWidth(2.0f);
        glDrawArrays(GL_LINES, 0, (int)(vertices.size() / 5));

        glBindVertexArray(0);
        glEnable(GL_DEPTH_TEST);
    }
};

static UIRenderer g_uiRenderer;

inline const char* qualityName(GraphicsQuality q)
{
    switch (q)
    {
        case GraphicsQuality::Low: return "LOW";
        case GraphicsQuality::Medium: return "MEDIUM";
        case GraphicsQuality::High: return "HIGH";
    }
    return "?";
}

// ============================================================
//  Draws the correct UI for whichever GameState we're in.
// ============================================================
inline void drawUIForState(GLFWwindow* window, GameState state, const Settings& settings,
                            const ShelterStatus& shelter, const std::vector<Survivor>& survivors,
                            int selectedMenuIndex, int selectedVoteIndex,
                            const std::string& statusMessage)
{
    int screenWidth, screenHeight;
    glfwGetFramebufferSize(window, &screenWidth, &screenHeight);

    std::vector<float> verts;
    float scale = 6.0f;

    auto addLine = [&](const std::string& text, float x, float y, float sc, float r, float g, float b)
    {
        VectorText::appendText(text, x, y, sc, r, g, b, verts);
    };

    if (state == GameState::MainMenu)
    {
        addLine("SHELTER PROTOCOL", 60, 100, 10.0f, 0.9f, 0.85f, 0.2f);

        const char* items[3] = { "NEW GAME", "SETTINGS", "QUIT" };
        for (int i = 0; i < 3; i++)
        {
            bool sel = (i == selectedMenuIndex);
            addLine(std::string(sel ? "> " : "  ") + items[i], 80, 260.0f + i * 50, scale,
                    sel ? 1.0f : 0.7f, sel ? 0.9f : 0.7f, sel ? 0.3f : 0.7f);
        }

        addLine("USE UP DOWN ENTER", 60, screenHeight - 60.0f, 4.0f, 0.5f, 0.5f, 0.55f);
    }
    else if (state == GameState::Settings)
    {
        addLine("SETTINGS", 60, 80, 8.0f, 0.9f, 0.85f, 0.2f);

        std::string qualityLine = std::string("GRAPHICS QUALITY   ") + qualityName(settings.quality);
        std::string resLine = std::string("RESOLUTION   ") + std::to_string(settings.windowWidth) + "X" + std::to_string(settings.windowHeight);
        std::string fsLine = std::string("FULLSCREEN   ") + (settings.fullscreen ? "ON" : "OFF");
        std::string vsyncLine = std::string("VSYNC   ") + (settings.vsync ? "ON" : "OFF");
        std::string volLine = std::string("VOLUME   ") + std::to_string((int)(settings.masterVolume * 100)) + "%";

        std::string lines[5] = { qualityLine, resLine, fsLine, vsyncLine, volLine };
        for (int i = 0; i < 5; i++)
        {
            bool sel = (i == selectedMenuIndex);
            addLine(std::string(sel ? "> " : "  ") + lines[i], 80, 220.0f + i * 50, scale * 0.85f,
                    sel ? 1.0f : 0.7f, sel ? 0.9f : 0.7f, sel ? 0.3f : 0.7f);
        }

        addLine("LEFT RIGHT TO CHANGE  ESC TO BACK", 60, screenHeight - 60.0f, 4.0f, 0.5f, 0.5f, 0.55f);
    }
    else if (state == GameState::Playing)
    {
        std::string dayLine = std::string("DAY ") + std::to_string(shelter.daysSurvived);
        addLine(dayLine, 40, 40, 5.0f, 0.9f, 0.9f, 0.9f);

        std::string o2Line = std::string("OXYGEN ") + std::to_string(shelter.oxygenLevel) + "%";
        std::string foodLine = std::string("FOOD ") + std::to_string(shelter.foodLevel) + "%";
        std::string powerLine = std::string("POWER ") + std::to_string(shelter.powerLevel) + "%";

        addLine(o2Line, 40, 90, 4.0f, 0.4f, 0.8f, 1.0f);
        addLine(foodLine, 40, 130, 4.0f, 0.9f, 0.8f, 0.3f);
        addLine(powerLine, 40, 170, 4.0f, 1.0f, 0.6f, 0.2f);

        addLine(statusMessage, 40, (float)screenHeight - 100, 4.0f, 0.85f, 0.85f, 0.85f);
        addLine("PRESS SPACE TO ADVANCE THE DAY", 40, (float)screenHeight - 60, 4.0f, 0.5f, 0.5f, 0.55f);
    }
    else if (state == GameState::VoteScreen)
    {
        addLine("A VOTE IS REQUIRED", 40, 60, 6.0f, 0.9f, 0.2f, 0.2f);
        addLine(statusMessage, 40, 120, 4.0f, 0.85f, 0.85f, 0.85f);

        int row = 0;
        for (size_t i = 0; i < survivors.size(); i++)
        {
            if (!survivors[i].alive) continue;
            bool sel = (row == selectedVoteIndex);
            std::string line = std::string(sel ? "> " : "  ") + survivors[i].name
                              + (survivors[i].isPlayer ? " (YOU)" : "");
            addLine(line, 60, 200.0f + row * 40, 4.5f, sel ? 1.0f : 0.7f, sel ? 0.3f : 0.7f, sel ? 0.3f : 0.7f);
            row++;
        }

        addLine("UP DOWN TO SELECT  ENTER TO VOTE OUT", 40, (float)screenHeight - 60, 4.0f, 0.5f, 0.5f, 0.55f);
    }
    else if (state == GameState::GameOver)
    {
        addLine("GAME OVER", 60, 200, 9.0f, 0.9f, 0.2f, 0.2f);
        addLine(statusMessage, 60, 300, 4.5f, 0.85f, 0.85f, 0.85f);
        addLine("PRESS ENTER TO RETURN TO MENU", 60, 400, 4.0f, 0.6f, 0.6f, 0.65f);
    }

    g_uiRenderer.render(verts, screenWidth, screenHeight);
}
