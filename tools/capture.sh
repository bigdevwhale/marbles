#!/usr/bin/env bash
# Screenshots of the demo build for the READMEs: tools/capture.sh <out-dir> en|ru
# Build it with `./gradlew assembleDebug -Pdemo -Pshots` (app.marbles.shots, never someone's demo).
# Reseeds it in that language, then walks the main screens with guarded taps.
set -e
cd "$(dirname "$0")/.."
source tools/phone.sh
OUT="$1"; LANG_="${2:-en}"
PKG=app.marbles.shots
mkdir -p "$OUT"
SHOTS="$OUT"
case "$PKG" in *.demo|*.shots) ;; *) echo "refusing to clear $PKG"; exit 1 ;; esac
"$ADB" shell pm clear $PKG >/dev/null
"$ADB" shell cmd locale set-app-locales $PKG --locales "$LANG_"
"$ADB" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 10
if [ "$LANG_" = ru ]; then T_TONIGHT="Вечер"; T_MIRROR="Зеркало"; T_KEPT="Память"; T_JAR="Банка"; T_LETTER="Письмо месяца"; T_PETYA="Петя"; T_DECK="Вся колода"
else T_TONIGHT="Tonight"; T_MIRROR="Mirror"; T_KEPT="Kept"; T_JAR="Jar"; T_LETTER="The month's letter"; T_PETYA="Petya"; T_DECK="The whole deck"; fi
up() { guard $PKG && "$ADB" shell input swipe 540 1800 540 700 400; sleep 1; }
back() { guard $PKG && "$ADB" shell input keyevent 4; sleep 2; }
shot 1-jar
up; up; shot 2-tray
"$ADB" shell input swipe 540 700 540 2000 300; sleep 1
tap_text $PKG "$T_TONIGHT"; sleep 3; shot 3-tonight
up; up; shot 4-tonight-more
tap_text $PKG "$T_MIRROR"; sleep 3; shot 5-mirror
up; up; shot 6-mirror-more
tap_text $PKG "$T_KEPT"; sleep 3; shot 7-kept
tap_text $PKG "$T_LETTER"; sleep 3; shot 8-letter
back
tap_text $PKG "$T_TONIGHT"; sleep 2
tap_text $PKG "$T_DECK"; sleep 3; shot 9-deck
back
tap_text $PKG "$T_JAR"; sleep 2
tap_at $PKG 540 2235; sleep 2; shot 10-start
back
echo done
