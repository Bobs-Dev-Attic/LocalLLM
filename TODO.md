# TODO

This is a prioritized implementation backlog for LocalLLM, an Android chat client for locally hosted language models (initially LM Studio over a local network).

## Repository baseline

The repository was inspected while preparing this backlog. It currently has a minimal README and no Android source code, Gradle project, tests, or implementation documentation. The items below therefore begin with establishing and documenting the application foundation. Do not treat suggested package names or architecture as existing code.

## P0 — Establish a buildable, secure foundation

- [ ] **Create the Android application skeleton.** Add Gradle wrapper, Android Gradle Plugin/Kotlin configuration, app module, manifest, and a reproducible build. Choose and document minimum/target SDK based on required Android integrations and support policy.
- [ ] **Document product and supported environments.** Define Android versions/devices, LM Studio versions, network assumptions, supported API endpoints, and explicit non-goals.
- [ ] **Define architecture boundaries.** Separate UI, conversation/domain logic, backend abstraction, persistence, and diagnostics. Keep Compose/ViewModel types out of backend contracts.
- [ ] **Establish quality gates.** Add formatting/lint, static analysis, unit tests, Android tests, dependency vulnerability checks, and CI build/test workflows.
- [ ] **Define a secrets and privacy policy.** No prompts/completions or credentials in logs by default; no analytics/network telemetry without opt-in; document data storage, export, deletion, and retention.
- [ ] **Harden network configuration.** Validate user-entered endpoint scheme/host/port; prefer HTTPS; narrowly scope any cleartext LAN exception; retain normal certificate validation; set connect/read/write timeouts; handle redirects safely.
- [ ] **Add a threat model.** Cover hostile LAN peers, DNS/IP changes, malicious server responses, endpoint spoofing, log/export leakage, backup exposure, clipboard leakage, and compromised devices.

## P1 — Deliver a reliable LM Studio connection

- [ ] **Implement backend interface and LM Studio adapter.** Encapsulate OpenAI-compatible API paths, serialization, HTTP transport, SSE parsing, and normalized errors.
- [ ] **Implement server settings.** Persist endpoint and optional authentication securely; mask secrets in UI; provide a connection test with actionable feedback.
- [ ] **Implement model discovery and selection.** Query available models when supported; handle empty/unavailable model lists; allow manual model ID entry as a fallback.
- [ ] **Implement connection lifecycle.** Distinguish connecting, connected, unavailable, timed out, unauthorized, and incompatible server states.
- [ ] **Implement robust streaming.** Correctly parse SSE framing, partial UTF-8 frames, multi-line data, finish reasons, usage chunks where available, server error frames, EOF, and cancellation.
- [ ] **Implement cancellation and recovery.** Stop generation on user request; handle app backgrounding/network changes; reconnect without duplicate or silently merged generations.
- [ ] **Add backend capability negotiation.** Model listing, token usage, performance telemetry, and other optional features must be capability-driven, not assumed.
- [ ] **Add protocol contract tests.** Use deterministic fixtures for success, errors, malformed JSON/SSE, slow responses, premature disconnects, and server-version differences.

## P1 — Core chat experience

- [ ] **Build conversation UI.** Message list, composer, send/stop, streaming updates, loading/error states, and accessible controls.
- [ ] **Implement conversation persistence.** Define schema, migrations, ordering, search, rename, delete, and safe handling of large histories.
- [ ] **Support conversation context correctly.** Track system/user/assistant messages, context limits, truncation strategy, and model changes without silently losing user content.
- [ ] **Implement message actions.** Copy, regenerate, edit/resend user message, and delete with clear behavior for downstream messages.
- [ ] **Handle long conversations efficiently.** Lazy/virtualized list rendering, stable item keys, bounded in-memory state, and avoid copying full history on every token.
- [ ] **Add UX states and recovery.** Empty state, no server, model unavailable, request failure, offline, interrupted stream, and retry affordances.
- [ ] **Validate large and unusual content.** Long prompts, Unicode, markdown/code blocks, large responses, and malformed model output.

## P1 — Diagnostics and processing details

- [ ] **Implement the diagnostics design in [docs/DIAGNOSTICS.md](docs/DIAGNOSTICS.md).** Treat it as the source of truth for metric definitions, event schema, privacy, and acceptance criteria.
- [ ] **Add request correlation IDs.** Generate one ID per generation attempt and propagate it through all client-side lifecycle events.
- [ ] **Measure client-observable timings.** Request dispatch, connection lifecycle where exposed, time to first decoded content token, and terminal duration using a monotonic clock.
- [ ] **Capture optional backend telemetry.** Token counts, finish reason, prompt evaluation, generation speed, context usage, and model-load metrics only when actually reported or reliably measured.
- [ ] **Add per-message Processing Details.** Collapsed by default; show endpoint label, model, timing, available token metrics, outcome, and explicit unavailable states.
- [ ] **Add a bounded diagnostics event store.** Structured metadata only by default; retention limit, asynchronous writes, clear-all, and process-restart semantics.
- [ ] **Add diagnostics viewer and export.** Search/filter by request ID and outcome; sanitized preview; user-initiated export; no automatic content export.
- [ ] **Test privacy redaction.** Prove prompts, completions, tokens, authorization headers, URL secrets, and sensitive payloads are absent from default logs.
- [ ] **Test diagnostic non-interference.** Ensure event collection cannot block streaming, freeze UI, or leak memory on long generations.

## P2 — Performance, privacy, and UX hardening

- [ ] **Profile memory and CPU.** Measure long conversation rendering, large messages, streaming, markdown rendering, and repeated model switching on representative low/mid-range devices.
- [ ] **Bound retained state.** Avoid duplicate message buffers; release completed stream buffers; cap diagnostics and transient response state; use paging for persisted conversations.
- [ ] **Review persistence security.** App-private storage, backup policy, database encryption decision, migrations, deletion semantics, and optional biometric/app-lock design.
- [ ] **Review Android permissions.** Request only required permissions; document local network behavior for supported Android versions; avoid broad storage access.
- [ ] **Accessibility review.** TalkBack labels/order, scalable text, contrast, keyboard navigation, and status communication beyond color.
- [ ] **Add theme and layout polish.** Light/dark themes, responsive phone/tablet layouts, code block readability, markdown tables, and long-response navigation.
- [ ] **Add structured error guidance.** Explain common causes (wrong host/port, firewall, server stopped, model not loaded, timeout) without leaking internal details.
- [ ] **Add privacy controls.** Retention configuration, diagnostics opt-out, clear conversation/diagnostic data, and explicit warning for any future content logging.
- [ ] **Run a security review.** Inspect dependencies, exported components, intent handling, WebView use (if any), backup rules, TLS configuration, input validation, and logcat output.

## P3 — Extensibility and advanced features

- [ ] **Add additional backend adapters.** Consider Ollama and llama.cpp server after the backend interface is stable; avoid LM Studio assumptions in shared UI.
- [ ] **Add attachments and multimodal input.** Define size/type limits, permissions, temporary-file cleanup, supported model capability checks, and local-only processing expectations.
- [ ] **Add Android integrations selectively.** Share intents, document picker, clipboard, and notification behavior with explicit privacy boundaries.
- [ ] **Add optional prompt templates/system presets.** Store locally, make active template visible, and define behavior when switching models/conversations.
- [ ] **Evaluate background generation.** Only if supported by Android lifecycle constraints; use an appropriate foreground service and transparent notification if long-running background work is essential.
- [ ] **Evaluate model/server discovery.** Keep discovery opt-in and LAN-scoped; do not scan broad address ranges by default.
- [ ] **Publish architecture and support docs.** Setup guide, troubleshooting, privacy/data handling, backend compatibility matrix, and contribution guide.

## Definition of done

- Builds reproducibly from a clean checkout using documented commands.
- Automated tests and static checks pass in CI.
- Network and storage behavior have documented privacy/security boundaries.
- Streaming and cancellation are reliable under slow, malformed, and interrupted network conditions.
- Long conversations and long generations remain responsive with bounded memory.
- Diagnostics are accurate, bounded, opt-in where sensitive, and content-free by default.
- User-facing errors are actionable and accessible.
