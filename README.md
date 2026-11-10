# Inkmode

An Android app that gives the whole phone an e-paper look. It turns everything grayscale except the apps you choose to keep in colour, and adds a minimal text-only home screen and optional notification batching.

Built for Android 15+ (tested target: Motorola edge 50 neo, Android 16). No ADB or computer needed.

## Install

1. On the phone, open **https://github.com/sandip-a11y/super-duper-waddle/releases/download/inkmode-latest/Inkmode.apk**
2. Open the downloaded file. Allow your browser to install apps when asked.
3. Open **Inkmode** and follow the setup steps on its first screen.

Every push rebuilds the APK through GitHub Actions. New builds install over the old one, with no uninstall needed.

## What it does

| Feature | How |
|---|---|
| Grayscale everywhere except colour apps (YouTube, Instagram, WhatsApp, Gmail by default) | An Android 15+ "Mode" whose only effect is grayscale, switched on and off by an accessibility service that sees which app is open. It never silences anything. |
| Minimal home screen | Clock, date, battery, a short text list of apps. All apps list turns pages instead of scrolling. |
| Paper by day, black ink by night | Follows the system dark theme schedule. Night uses true black, which saves power on P-OLED. |
| Notification batching (off by default) | Holds notifications and posts one digest every 30/60/120 minutes. Calls, alarms and "always let through" apps (WhatsApp, Messages, Phone by default) are never held. |
| Animations off, 60 Hz, Night Light, Extra dim | The app can't change these without ADB, so it opens the right Settings screen and shows whether each one is done. |
| Undo | "Turn everything off" in settings. |

## Setup steps (all on the phone)

1. App info → ⋮ → **Allow restricted settings** (Android requires this for apps not from the Play Store)
2. Accessibility → **Inkmode colour switch** → on
3. **Do Not Disturb access** → Inkmode → allow
4. **Notification access** → Inkmode → allow (only for batching)
5. Allow Inkmode's notifications
6. Set Inkmode as the **Home app**

## Known limits

- Per-app grayscale uses Android's Modes API. On some phones the switch may lag by a moment or show a Mode icon. Use "Test grayscale" in settings to check it works.
- YouTube picture-in-picture over another app will be grayscale.
- Tapping a held notification opens the original target only if Inkmode hasn't been restarted since it was held. Otherwise it opens the app.
- The signing key (`app/inkmode.keystore`) is committed so builds can update each other. It is a convenience key, not a secret: only install Inkmode from this repo's releases.
