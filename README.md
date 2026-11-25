# LocalLLM

A local-first Android chat client for language models hosted by LM Studio on your local network.

## Current status

The first Android foundation has landed on `main` and is under active development. The initial vertical slice includes:

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

## Install a prebuilt APK

Tagged releases publish a ready-to-sideload debug APK. Download the latest from
the [Releases page](https://github.com/Bobs-Dev-Attic/LocalLLM/releases) onto an
Android device and open it to install (you will be prompted to allow installs
from the source app). This is a debug build for personal use, not a Play Store
release. See the install steps in the release notes.

All releases from v0.1.2 onward are signed with a shared, committed debug key
(`app/debug.keystore` — not a secret, not for production), so new versions
install in place over older ones and the in-app update check can install them.
Upgrading from v0.1.0 or v0.1.1, which were each signed with a different
throwaway key, requires uninstalling the app once and installing the new APK
fresh (an "App not installed" error on update is this signature change).

## Build

The project includes a committed Gradle wrapper pinned to Gradle 8.13, so no
system Gradle install is required. From the repository root, open the project in
Android Studio, or build from the command line with:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Connect to LM Studio

1. Start LM Studio on the host computer and load a model.
2. Enable its local server.
3. Find the host computer's LAN IP address and the server port (commonly `1234`).
4. In LocalLLM, open Connection settings and enter an API base URL such as `http://192.168.1.10:1234/v1`.
5. Tap **Test & load models**, then select a discovered model or enter its ID.
6. Send a message.

Use the host computer's LAN address, not `localhost` or `127.0.0.1` on the Android device.

## Recommended models

Any OpenAI-compatible model served by LM Studio works for chat. For coding —
and especially for the **GitHub tool-calling** feature (Settings → GitHub
accounts → "Let the model call GitHub tools") — pick a model whose LM Studio
card shows the **Tool Use** capability, or tool calls will be ignored.

Sizing is driven by GPU VRAM. Leave headroom beyond the weights file for the
context window and KV cache; a GGUF that fits exactly will not, in practice,
with a useful context length.

| VRAM | Suggested coding models (GGUF, via LM Studio) |
| --- | --- |
| ~16 GB | Qwen2.5-Coder 14B (Q4_K_M), Qwen3-Coder 30B-A3B (Q4, with some CPU offload) |
| ~24 GB | Qwen3-Coder 30B-A3B (Q4_K_M), Devstral Small 24B (Q4/Q5), Qwen2.5-Coder 32B (Q4, tight) |
| ~32 GB | **Qwen3-Coder 30B-A3B (Q4_K_M, ~18.6 GB)** — recommended; Qwen2.5-Coder 32B (Q4_K_M ~20 GB / Q6_K ~27 GB); Devstral Small 24B (Q5/Q6) |
| 48 GB+ | Qwen2.5-Coder 32B (Q8), larger MoE coders |

Notes:

- **Qwen3-Coder 30B-A3B** is a Mixture-of-Experts model (~3B active of ~30B
  total), so it is fast while staying strong at code, and advertises Tool Use.
  It is the default recommendation on a 32 GB card.
- Dense alternatives like **Qwen2.5-Coder 32B** are also solid; choose the quant
  (Q4/Q6) that leaves room for your context length.
- Model names and releases change often — confirm the current build in the LM
  Studio catalog or on Hugging Face before downloading.

## Network and privacy notes

- The app sends prompts to the LM Studio endpoint that the user configures. The model host therefore receives conversation content.
- No analytics or developer telemetry is implemented.
- Default diagnostics show request metadata and do not intentionally persist prompts or completions.
- HTTPS is preferred when available. LM Studio commonly uses HTTP on a private LAN. Android's cleartext traffic policy is app-wide for this first version because the endpoint is user-configurable; application validation limits HTTP URLs to literal private, loopback, or link-local IP addresses (plus localhost), avoiding hostname-based DNS rebinding for cleartext requests. Redirects are disabled. This is defense in depth, not a substitute for using a trusted network.
- Do not expose the LM Studio server directly to the public internet. A hostile device on the same LAN may still observe or attack unencrypted HTTP traffic.
- Backup is disabled for the initial app foundation. Endpoint settings are stored in app-private preferences.

## Architecture direction

The UI and chat state are separate from the LM Studio HTTP adapter. Keep provider-specific protocol details behind a backend interface as the app grows, so Ollama, llama.cpp server, or other compatible backends can be added without coupling them to Compose.

See [TODO.md](TODO.md) for the prioritized roadmap and [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md) for the processing-details and diagnostics specification.
