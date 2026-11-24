# Changelog

All notable changes to LocalLLM. This file is the online changelog; each release
also appears on the [Releases page](https://github.com/Bobs-Dev-Attic/LocalLLM/releases)
with its downloadable APK.

## 0.1.19

- The resubmit (replay) icon now sits inline, just left of each sent message,
  instead of on its own line below.
- When not connected to LM Studio, the composer's Send button shows a
  cloud-off icon (dimmed) instead of the paper-plane, as a reminder to connect.

## 0.1.18

- Resubmit a sent message: each of your messages now has a replay icon that
  re-sends the same text (and any attached images) as a new turn.

## 0.1.17

- Color themes now tint the whole chat window background and surfaces (not just
  the accent and message bubbles).
- Stronger, more visible field glow while the model is processing.

## 0.1.16

- The "More detail" response action is now an icon.
- The thinking gradient now animates inside the "Message your model" input
  field while generating, instead of across the whole chat background.

## 0.1.15

- Smaller corner radius on the message composer field.
- Moved the Copy and More detail actions to the right of the timing/tokens row,
  on the same line.

## 0.1.14

- New "Node Mind" app launcher icon (adaptive, with a themed-icon monochrome
  layer).
- The processing background gradient is now subtle and gently breathes, fading
  in when generation starts and out when it ends.

## 0.1.13

- Color themes now set matching container colors, so message bubbles and chips
  line up with each theme's accent (Blue, Rainbow, Monochrome).
- Made the animated generating-background gradient clearly visible (stronger and
  with distinct bands).

## 0.1.12

- Conversation history titles now summarize all questions in the conversation,
  not just the first.
- Connection status icon (connected/disconnected) next to the status label in
  the title.
- Response actions: copy the reply, a "More detail" button that asks the model
  to expand, and a Sources list of any links in the reply.

## 0.1.11

- Image input: attach photos to a message (Android photo picker) and send them to
  vision-capable LM Studio models as OpenAI image parts. Images are downscaled,
  shown as thumbnails in the composer and in the chat, and saved with history.

## 0.1.10

- Left-aligned the conversation history list items.

## 0.1.9

- Conversation history: save multiple conversations and reopen old ones from a
  history icon in the top bar; clear all history from there.
- Font size is now a slider (Smallest → Largest).
- New color themes: Blue, Rainbow, High Contrast, Monochrome (in addition to the
  default), each with light and dark variants.
- Animated diagonal gradient background while the model is generating, tinted to
  the current theme.
- Updates section: "Check for updates" is now a refresh icon beside the Updates
  header, with a link to this changelog.
- Rounded the message composer and reduced text-field padding for a tighter form.

## 0.1.8

- Processing details (timings, token counts, finish reason) persist with the
  conversation and are restored on reload.
- Tighter padding and margins at the Small and Smallest font sizes.

## 0.1.7

- Markdown rendering in finished assistant replies (headings, bold/italic, inline
  code, code blocks, lists, quotes, rules, links).
- The current conversation is saved and restored on next launch.
- Font sizes: default Small, with Smallest and Largest added.
- Model selection is an editable dropdown of discovered models with a Refresh icon.
- API base URL history dropdown.
- Condensed Settings dialog.

## 0.1.6

- Processing details moved to an info icon beside the duration/tokens summary.
- Live elapsed timer while the model is generating.
- Auto-scroll to the newest content as replies stream in.
- Token summary omitted when the server reports no counts.

## 0.1.5

- Default LM Studio base URL preset to `http://192.168.11.225:1234/v1`.

## 0.1.4

- App version shown next to the title in the top bar.

## 0.1.3

- Per-reply duration and token summary shown under each answer.
- Fixed the Settings dialog not opening at the Small font size.

## 0.1.2

- All builds signed with a shared debug key so updates install in place.

## 0.1.1

- Navigation-bar inset fix for the composer.
- Persistent theme and font-size settings.
- Loading indicator for Test & load models.
- In-app update check against GitHub releases.

## 0.1.0

- First Android foundation: LM Studio connection, model discovery, streaming
  chat, stop generation, per-response processing details, Gradle wrapper, and an
  automated release workflow.
