# vLLM Remote — Android App

Native Android app for remotely monitoring and controlling a vLLM inference server. Provides a single-screen dashboard with real-time GPU metrics, model management, and service lifecycle controls.

## Screenshots

*TODO: Add screenshots*

## Features

- Real-time server status with color-coded state chips
- Animated GPU gauge rings (utilization, VRAM, temperature) with severity thresholds
- Model switching with usage-sorted list
- Service controls (start/stop/restart)
- Server shutdown with confirmation dialog
- Configurable server URL with DataStore persistence
- Auto-polling every 5 seconds with unreachable detection
- Haptic feedback on all actions
- Always-dark teal theme

## Requirements

- Android 13+ (API 33)
- Network access to the vLLM Manager backend (e.g. via Tailscale)
- JDK 17 for building

## Building

```bash
cd android
./gradlew assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

To install directly on a connected device:

```bash
./gradlew installDebug
```

## Architecture

Clean Architecture with three layers:

```
domain/          Pure Kotlin — models, repository interfaces, use cases
data/            Ktor HTTP client, DataStore persistence, repository implementations
presentation/    Jetpack Compose UI, ViewModel with StateFlow
di/              Koin dependency injection modules
```

### Tech Stack

| Concern           | Library                       |
|-------------------|-------------------------------|
| UI                | Jetpack Compose (Material 3)  |
| HTTP Client       | Ktor (CIO engine)             |
| DI                | Koin                          |
| State Management  | ViewModel + StateFlow         |
| Local Persistence | Preferences DataStore         |
| Serialization     | Kotlinx Serialization         |
| Build System      | Gradle Kotlin DSL + Version Catalog |

### Data Flow

The `DashboardViewModel` polls `/status` and `/models` every 5 seconds in parallel. UI state is exposed as a single `StateFlow<DashboardUiState>`. Actions (start, stop, switch, etc.) use a fire-and-forget pattern — the next poll cycle picks up the resulting state change.

## Configuration

On first launch, enter the full server URL including port (e.g. `http://100.x.x.x:9090`). The URL is persisted in DataStore and can be changed via the settings icon in the top bar.

## Server Compatibility

Communicates with the vLLM Manager backend on port 9090. No authentication — relies on Tailscale network security.

### API Endpoints Used

| Endpoint    | Method | Purpose                          |
|-------------|--------|----------------------------------|
| `/status`   | GET    | Server state, model, GPU stats   |
| `/models`   | GET    | List configured models           |
| `/start`    | POST   | Start vLLM service               |
| `/stop`     | POST   | Stop vLLM service                |
| `/restart`  | POST   | Restart vLLM service             |
| `/switch`   | POST   | Switch model (`{"model": "id"}`) |
| `/shutdown` | POST   | Shut down server machine         |
