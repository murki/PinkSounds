# NoiseScape Architecture & Guidelines

Welcome to the NoiseScape project. This file provides critical context for AI agents working on this application to ensure consistency in architecture, design, and functionality.

## Project Overview
NoiseScape is a minimalist, immersive Android application for generating psychoacoustic sound profiles (White, Gray, and Pink noise). It leverages real-time digital signal processing (DSP) for infinite, unlooped procedural audio generation.

## Technical Architecture

### 1. Audio Engine (`com.example.audio.NoisePlayer`)
- **Procedural DSP**: Do NOT use pre-recorded MP3s or looping audio files. All sound is synthesized at runtime.
- **Algorithms**:
    - *Pink Noise*: Uses a 12-pole Voss-McCartney approximation algorithm.
    - *Gray Noise*: Combines real-time digital low-pass and high-pass filters to create a U-shaped equal-loudness contour over white noise.
- **Execution**: Runs in a dedicated high-priority background thread writing PCM 16-bit audio directly to an `AudioTrack`.
- **Constraint**: When modifying audio generation, carefully manage performance and ensure thread-safety. State is managed via `@Volatile` variables for real-time parameter tweaking (like volume fading).

### 2. Presentation Layer (`com.example.ui.NoiseViewModel`)
- **State Management**: Uses Kotlin Coroutines and `StateFlow`.
- **Responsibilities**: Controls active noise profile, volumes, play/pause states, sleep timer countdown, and preset configurations. State is persisted locally using `SharedPreferences`.

### 3. User Interface (`com.example.ui.NoiseUiScreen`)
- **Paradigm**: Built entirely in Jetpack Compose.
- **Immersive Custom Design**: The app deliberately avoids standard Material 3 components (like default sliders or cards) in favor of a highly custom "Immersive UI" theme.
    - *Sliders*: Uses a custom `ImmersiveSlider` implementation with horizontal drag gesture detection. Do NOT revert to `androidx.compose.material3.Slider`.
    - *Interactions*: Relies heavily on pointer inputs, vertical dragging to switch profiles, and custom animations like `corePulse` and `spring` bounces.
    - *Visualizer*: Uses a live, procedural `Canvas`-based `WaveformVisualizer` built with sine waves.

## Design Tokens & Theming
When altering or adding UI, strictly adhere to the following color palette and principles:

- **Background (Canvas)**: Deep charcoal (`#1A1C1E`)
- **Secondary / Card Backgrounds**: Dark gray (`#2A2D30`, `#303033`)
- **Text / Muted Icons**: Cool silver (`#C2C7CF`), Darker gray (`#45474A`)
- **Accent Colors (Dynamic by Profile)**:
    - *Pink Noise*: Warm comfort rose (`#FFB1C8`) / Light (`#FFD9E2`)
    - *White Noise*: Pure white (`#FFFFFF`) / Light (`#F5F5F5`)
    - *Gray Noise*: Stormy slate gray (`#78909C`) / Light (`#B0BEC5`)

## Guidelines for Modification
1. **Preserve Custom UI**: Do not replace custom interactive elements (`ImmersiveSlider`, pulse animations, gradient auras) with standard Material components unless specifically instructed to strip the design.
2. **Gesture Navigation**: The primary navigation is a vertical drag on the background to cycle through profiles. Ensure new overlay UI elements do not block this gesture unexpectedly.
3. **Audio Performance**: Any additions to `NoisePlayer.kt` inside the `while(isPlaying)` loop must be heavily optimized, avoiding complex object allocations to prevent audio dropouts (stuttering).
4. **Consistency**: Maintain the sleek, minimalist aesthetic. Avoid adding unnecessary borders, drop shadows, or high-contrast bright backgrounds. Keep Typography weights targeted (use `ExtraBold` for headers, `Light` for main titles, `Medium`/`Normal` for captions).
