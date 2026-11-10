# Inkmode

An Android app that gives the whole phone an e-paper look. It turns everything grayscale except the apps you choose to keep in colour, and adds a minimal text-only home screen and a paged app list.

Built for Android 15+ (tested target: Motorola edge 50 neo, Android 16). No ADB or computer needed.

## Install

1. On the phone, open **https://github.com/sandip-a11y/super-duper-waddle/releases/download/inkmode-latest/Inkmode.apk**
2. Open the downloaded file. Allow your browser to install apps when asked.
3. Open **Inkmode** and follow the setup steps on its first screen.

Every push rebuilds the APK through GitHub Actions. New builds install over the old one, with no uninstall needed.

## What it does

| Feature | How |
|---|---|
| Grayscale everywhere except colour apps (YouTube, Instagram, WhatsApp, Gmail by default) | An Android 15+ "Mode" whose only effect is grayscale. A small background service checks which app is open about once a second (via Usage access) while the screen is on, and switches the Mode. It never silences anything. |
| Minimal home screen | Clock, date, battery, a short text list of apps. The all-apps list turns pages instead of scrolling. |
| Paper by day, black ink by night | Follows the system dark theme schedule. Night uses true black, which saves power on P-OLED. |
| Animations off, 60 Hz, Night Light, Extra dim | The app can't change these without ADB, so it opens the right Settings screen and shows whether each one is done. |
| Undo | "Turn everything off" in settings. |

Inkmode deliberately uses **no accessibility service and no notification access**. Play Protect's fraud protection blocks sideloaded apps that ask for either, with no way to override it on some phones. That is also why there is no notification batching.

## Setup steps (all on the phone)

1. **Do Not Disturb access** → Inkmode → allow
2. **Usage access** → Inkmode → allow (if greyed out: App info → ⋮ → Allow restricted settings)
3. Let Inkmode run in the background (battery: Unrestricted)
4. Set Inkmode as the **Home app**

## Known limits

- Grayscale switches up to about a second after you open or leave a colour app.
- Per-app grayscale uses Android's Modes API. On some phones the switch may lag by a moment or show a Mode icon. Use "Test grayscale" in settings to check it works.
- YouTube picture-in-picture over another app will be grayscale.
- Inkmode shows a silent, minimised notification while the colour switch runs. Android requires it for background services.
- The signing key (`app/inkmode.keystore`) is committed so builds can update each other. It is a convenience key, not a secret: only install Inkmode from this repo's releases.
