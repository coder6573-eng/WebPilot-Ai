# WebPilot AI

WebPilot is an Android-first autonomous browser agent. The Android app contains its own browser and agent interface: switch between **Agent** and **Browser** without leaving the app.

## Standalone Android app

The core mobile app does **not** require a separate computer, FastAPI server, Docker container, or remote browser worker. It uses the Android WebView as its browser and calls Google's Gemini API directly.

### Features
- Separate Agent and Browser tabs sharing one browser session
- Built-in web browser with address bar, JavaScript and page storage
- Gemini-powered task planning and browser actions
- Encrypted local storage for the Gemini API key
- Configurable Gemini model (default: `gemini-2.5-flash`)
- Activity feed, task status and stop control
- Bounded agent steps and a restricted browser action set

### Setup
1. Install the APK.
2. Open **Settings** and enter a Google Gemini API key from Google AI Studio.
3. Keep the default model or enter a Gemini model available to your API key.
4. Save the key. It is stored using AndroidX Security Crypto encrypted preferences.
5. Return to Agent, enter a task, and tap **Run agent**.
6. Switch to Browser to watch the same in-app browser while the agent works.

The API key is sent directly from the device to Google's Gemini API over HTTPS. It is not sent to this repository or a WebPilot server. API usage may be subject to Google's current quotas and billing terms.

## Build the Android APK

Open `android/` in Android Studio, or build from a machine with JDK 17 and Gradle:

```bash
cd android
gradle assembleDebug
```

APK output:
`android/app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions also builds a debug APK on pushes affecting `android/` and on manual workflow dispatch. Download it from the workflow run's Artifacts section.

## Browser-agent safety and limitations

- Webpage content is untrusted input and must not override the agent's task rules.
- The agent is limited to navigation, inspection, clicking, filling, scrolling, back, waiting and finishing.
- The agent is instructed not to perform purchases, send messages, submit forms, delete data, publish, transfer money, or change account/security settings. Complete those actions manually.
- This is an early prototype. Websites can change their layouts, and some sites may block WebView or automated interaction.
- The API key is encrypted at rest on the device, but anyone with access to an unlocked device may be able to use the configured key. Remove it in Settings before sharing the device.

## Optional backend

The `backend/` directory contains the earlier remote-worker prototype. It is not required by the standalone Android app and is not part of its normal setup.
