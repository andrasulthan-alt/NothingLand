# OmniLand

A Dynamic Island–style overlay for Android. OmniLand puts a small pill around your front camera that expands to show music controls, notifications and charging status.

OmniLand is a fork of [NothingLand](https://github.com/TheSerphh/NothingLand), originally made for Nothing phones. This fork focuses on making it work well on **any Android phone**, with signed releases you can update without reinstalling.

## Features

- **Calls**: incoming calls with Answer / Decline buttons, and a live timer during a call
- **Timers and stopwatches**: a live countdown or stopwatch from your Clock app
- **Downloads and uploads**: live progress from any app that shows a progress notification
- **Media**: current song, playback controls and an audio visualizer
- **Notifications**: incoming notifications in the island; choose which apps are allowed
- **Charging**: battery level while the phone is charging
- **Gestures**: tap, long-press and swipe to expand, collapse or dismiss
- **Looks**: custom background color, accent color or your own background image; adjustable size and position
- **Automatic positioning**: the island centers itself on your front camera (Android 11+)
- **Two activities at once**: when two things are active (for example music and a timer), the second one appears as a small bubble next to the island; tap it to swap
- **Album colors**: the island takes on a color from the album art of the song that's playing
- **Smooth animations and haptic feedback**
- **Maps navigation**: next turn, distance and ETA from Google Maps and other navigation apps
- **Hotspot status**: connected devices while your hotspot is on
- **Gestures on the idle island**: tap, double tap, long press and swipes can toggle the flashlight, open the camera or an app, take a screenshot, open notifications or quick settings, and more
- **Quick cards**: a sliders card (brightness, media volume, flashlight brightness) and a favorite apps card
- **Mini system events**: Bluetooth connected, ringer mode, airplane mode, hotspot, headphones, low battery, battery saver
- **Charging estimate**: time until full while charging
- **Demo mode**: fake call, timer, download and navigation events to try everything out

Most of these can be switched off individually in OmniLand's settings.

## Requirements

- Android 8.0 or newer
- Background blur needs Android 12 or newer (the island works without it on older versions)

Tested on: Samsung Galaxy S20 Ultra (Android 13). Reports from other phones are welcome in [Issues](https://github.com/andrasulthan-alt/OmniLand/issues).

## Download

- **GitHub Releases**: [latest APK](https://github.com/andrasulthan-alt/OmniLand/releases/latest)
- **Obtainium** or **Komi Store**: add `https://github.com/andrasulthan-alt/OmniLand`

Updates install over the previous version and keep your settings.

> **Upgrading from 0.4 or earlier?** Version 0.5 has a new app ID, so uninstall the old OmniLand once before installing 0.5. This is only needed once.

## Setup

1. **Install the APK.** Google Play Protect may block it, because the app uses accessibility and notification access. If so, open Play Store > profile > Play Protect > settings, turn off "Scan apps with Play Protect", install OmniLand, then turn scanning back on.
2. **Open OmniLand** and grant the permissions it asks for.
3. **Turn on the accessibility service**: Settings > Accessibility > Installed apps > OmniLand.
   On Android 13 and newer you'll first see **"Restricted setting"**. Tap OK, then go to Settings > Apps > OmniLand, tap **Allow restricted settings** (in the ⋮ menu or at the top right), and try again.
4. Optional: in OmniLand, tap **Disable battery optimization** so the system doesn't stop the island in the background.

> **After updating OmniLand**, if calls, timers or notifications stop appearing in the island, restart your phone once. Android sometimes doesn't reconnect notification access after an app update.

## Permissions

| Permission | Why |
|---|---|
| Accessibility service | Draws the island over other apps and the status bar |
| Notification access | Shows notifications and detects media playback |
| Microphone (record audio) | Only for the music visualizer; audio is analyzed live and never recorded or saved |
| Photos / storage | Only if you choose a custom background image |
| Internet | Only to check GitHub for a newer OmniLand version |
| Query installed apps | To list apps in the notification filter, gestures and the favorite apps card |
| Modify system settings | Only if you use the brightness slider; asked for when you first move it |
| Nearby devices (Bluetooth) | Only to show "headphones connected" events |

OmniLand has no ads, no analytics and no tracking.

## Building

Every push to the `NL-beta` branch builds an APK with GitHub Actions (Actions tab > Build APK > Artifacts). Release builds are signed with the project's release key, stored as repository secrets; forks without those secrets get a debug APK instead.

## Credits and license

- Original app: [NothingLand](https://github.com/TheSerphh/NothingLand) by theglitchh, released under the MIT License. The original notice is kept in [LICENSE-NothingLand-MIT.txt](LICENSE-NothingLand-MIT.txt).
- Navigation parsing, hotspot detection and system-event handling follow the approach of [Smart Island](https://github.com/agupta07505/SmartIsland) by Animesh Gupta (GPL-3.0), re-implemented in Java for OmniLand.
- OmniLand, including all changes in this fork, is licensed under the [GNU General Public License v3.0](LICENSE).
