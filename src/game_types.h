#pragma once

#include <string>
#include <vector>

// ============================================================
//  RED FLAG PROTOTYPE - Shared types
// ============================================================
// This is a solo, single-player prototype inspired by the
// bunker-survival / vote-to-eliminate theme. It is an original
// game and not a copy of any commercial title.
// ============================================================

enum class GameState
{
    MainMenu,
    Settings,
    Playing,
    VoteScreen,
    GameOver
};

enum class GraphicsQuality
{
    Low,
    Medium,
    High
};

struct Settings
{
    int windowWidth      = 1280;
    int windowHeight     = 720;
    bool fullscreen      = false;
    bool vsync           = true;
    GraphicsQuality quality = GraphicsQuality::Medium;
    float masterVolume   = 0.8f;
    float mouseSensitivity = 0.15f;
    bool showReflections = true;
    int shadowResolution = 1024; // scales with quality
};

struct Survivor
{
    std::string name;
    int suspicion   = 0;   // how "guilty" they seem, 0-100
    int usefulness  = 50;  // how valuable they seem to the group, 0-100
    bool alive      = true;
    bool isPlayer   = false;
};

struct ShelterStatus
{
    int oxygenLevel   = 100;
    int foodLevel     = 100;
    int powerLevel    = 100;
    int daysSurvived  = 0;
    int survivorsRequired = 3; // game ends when shelter reaches this count
};
