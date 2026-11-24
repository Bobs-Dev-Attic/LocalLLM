# LocalLLM

A local-first Android chat client for language models hosted by LM Studio on your local network.

## Current status

The first Android foundation is under active development on the `build/android-foundation` branch. The initial vertical slice includes:

- Native Android UI built with Kotlin and Jetpack Compose.
- Configurable LM Studio OpenAI-compatible API base URL.
- Model discovery through `GET /v1/models`, with manual model ID entry.
- Streaming chat through `POST /v1/chat/completions` using server-sent events.
- Stop-generation support and per-response processing details (request ID, model, endpoint, time to first token, total duration, token counts and finish reason when supplied by the server).
- Basic endpoint validation and bounded connection/read/write timeouts.
- A GitHub Actions build, unit-test and lint workflow.

This is an early foundation, not a feature-complete release. Conversation persistence, markdown rendering, attachments, richer diagnostics history/export, credential management and additional backends remain planned.

## Requirements

- Android Studio with Android SDK 36 and JDK 17.
- Gradle 8.13 for command-line builds.
- LM Studio running on a computer reachable from the Android device over the same trusted network.
- A model loaded in LM Studio and its local server enabled.

## Build

From the repository root, use Android Studio to open the project, or install Gradle 8.13 and run:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

To generate a standard Gradle wrapper for local use:

```bash
gradle wrapper --gradle-version 8.13
```

Then use `./gradlew` (or `gradlew.bat` on Windows) for subsequent builds.

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Connect to LM Studio

1. Start LM Studio on the host computer and load a model.
2. Enable its local server.
3. Find the host computer's LAN IP address and the server port (commonly `1234`).
4. In LocalLLM, open Connection settings and enter an API base URL such as `http://192.168.1.10:1234/v1`.
5. Tap **Test & load models**, then select a discovered model or enter its ID.
6. Send a message.

Use the host computer's LAN address, not `localhost` or `127.0.0.1` on the Android device.

## Network and privacy notes

- The app sends prompts to the LM Studio endpoint that the user configures. The model host therefore receives conversation content.
- No analytics or developer telemetry is implemented.
- Default diagnostics show request metadata and do not intentionally persist prompts or completions.
- HTTPS is preferred when available. LM Studio commonly uses HTTP on a private LAN. Android's cleartext traffic policy is app-wide for this first version because the endpoint is user-configurable; application validation limits HTTP URLs to addresses that resolve exclusively to private, loopback, or link-local IP ranges. Redirects are disabled. This is defense in depth, not a substitute for using a trusted network.
- Do not expose the LM Studio server directly to the public internet. A hostile device on the same LAN may still observe or attack unencrypted HTTP traffic.
- Backup is disabled for the initial app foundation. Endpoint settings are stored in app-private preferences.

## Architecture direction

The UI and chat state are separate from the LM Studio HTTP adapter. Keep provider-specific protocol details behind a backend interface as the app grows, so Ollama, llama.cpp server, or other compatible backends can be added without coupling them to Compose.

See [TODO.md](TODO.md) for the prioritized roadmap and [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md) for the processing-details and diagnostics specification.
