# SHELTER PROTOCOL

A solo prototype inspired by bunker-survival / social-deduction games
(vote someone out to save resources). Built in C++ with OpenGL 3.3,
real 3D lighting, a reflective floor, and a settings menu that
actually changes render cost.

This is an original game — not a copy of any commercial title.

## What's in this repo

- `src/main.cpp` — game loop, menus, bunker scene, vote mechanic
- `src/game_types.h` — shared structs (settings, survivors, shelter state)
- `src/shader.h` — shader loading/compiling helper
- `src/mini_math.h` — small self-contained vec3/mat4 math (no GLM needed)
- `src/mesh.h` — cube/floor geometry generation
- `src/vector_text.h` — built-in text/menu renderer (no font files needed)
- `shaders/` — GLSL shaders (lighting + reflections)
- `CMakeLists.txt` — build configuration
- `.github/workflows/build-windows.yml` — auto-builds a Windows .exe

## How to build it from your phone (no PC needed)

1. Create a new GitHub repository (or reuse an existing one).
2. Push all these files to it, keeping the folder structure exactly
   as-is (`src/`, `shaders/`, `.github/workflows/`, `CMakeLists.txt`,
   this `README.md`).
3. GitHub Actions will automatically start a build (check the
   "Actions" tab in your repo).
4. Wait for the build to finish (a few minutes) — green checkmark
   means success.
5. Click into the finished run, scroll to "Artifacts", and download
   `ShelterProtocol-Windows.zip`.
6. Unzip it on any Windows PC and run `ShelterProtocol.exe` —
   everything it needs (DLLs, shaders) is bundled in the same folder.

You never need to install a compiler yourself — GitHub's cloud
machine does the compiling. You're just pushing source code.

## Controls

- **Main Menu**: Up/Down to navigate, Enter to select
- **Settings**: Up/Down to navigate, Left/Right to change a value,
  Esc/Backspace to go back
- **In-game**: Space or Enter to advance a day
- **Vote screen**: Up/Down to pick a survivor, Enter to vote them out
- **Esc** during play returns to the main menu

## Settings menu (as requested)

- **Graphics Quality**: Low / Medium / High — actually toggles
  reflections on/off and changes internal shadow resolution, so a
  weaker PC can turn it down for real performance gains
- **Resolution**: toggles between 1280x720 and 1920x1080
- **Fullscreen**: on/off
- **VSync**: on/off
- **Volume**: 0–100% (hook point for audio once sound is added)

## Extending it later

- Add more survivor traits / dialogue
- Add real audio (currently just a volume setting placeholder)
- Add more room shapes instead of the ring-of-walls placeholder
- Swap the vector-font UI for a proper texture-based font if you
  want nicer-looking text later
