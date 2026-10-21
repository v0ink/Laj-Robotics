#!/usr/bin/env bash
# Build the Robot Controller app with our TeamCode and install it on the Control Hub (macOS).
#   ./deploy.sh          build + install over USB-C (or an already connected Wi-Fi adb)
#   ./deploy.sh wifi     connect to the Control Hub over its Wi-Fi first, then build + install
#   ./deploy.sh build    only build (checks that the code compiles; no robot needed)
set -euo pipefail
cd "$(dirname "$0")"

# The FTC build needs JDK 17; newer JDKs on this Mac are too new for it
if [ -d /opt/homebrew/opt/openjdk@17 ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$ANDROID_HOME/platform-tools/adb"

if [ ! -f local.properties ]; then
  echo "sdk.dir=$ANDROID_HOME" > local.properties
fi

case "${1:-usb}" in
  build)
    ./gradlew assembleDebug
    exit 0
    ;;
  wifi)
    # Laptop must be joined to the Control Hub's Wi-Fi network
    "$ADB" connect 192.168.43.1:5555
    ;;
esac

if ! "$ADB" devices | grep -qw device; then
  echo "No Control Hub found. Plug it in with USB-C (and power it on), or run: ./deploy.sh wifi"
  exit 1
fi

./gradlew installDebug
echo "Installed. On the Driver Hub, pick the OpMode and press INIT."
