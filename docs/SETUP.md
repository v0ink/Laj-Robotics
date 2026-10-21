# Setup: build and deploy

Our code lives in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`. Deploying builds the whole Robot Controller app with our code inside and installs it on the **Control Hub** (the box on the robot). The Driver Hub never gets code.

## First time on a new computer

```bash
git clone https://github.com/v0ink/Laj-Robotics.git
```

### macOS (Homebrew)

```bash
brew install openjdk@17 android-commandlinetools
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
yes | sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools --licenses
sdkmanager --sdk_root=/opt/homebrew/share/android-commandlinetools "platform-tools" "platforms;android-30" "build-tools;35.0.0"
```

After that, `./deploy.sh` handles the paths for you.

### Windows

The easiest path is **Android Studio**. It installs the JDK and Android SDK itself.

1. Install Android Studio and open the `Laj-Robotics` folder. Let it sync; it downloads what it needs.
2. Install **Git for Windows** to pull and push code. Claude Code works too.
3. Plug in the Control Hub with USB-C. If Android Studio doesn't list it, install the **Google USB Driver** (Android Studio → SDK Manager → SDK Tools).
4. Select the **TeamCode** run configuration and the Control Hub device, then press **Run ▶**.

If you'd rather use the command line: with Android Studio's JDK set as `JAVA_HOME`, run `gradlew.bat installDebug`.

## Getting the latest code in Android Studio (Windows)

1. Connect the PC to **normal internet**, not the robot's Wi-Fi.
2. Pull: **Git → Pull…** in the top menu (or the blue ↙ arrow next to the branch name `master` at the top left), then click **Pull**.
   You can also open the **Terminal** tab at the bottom and run `git pull`.
3. If the build files changed, Android Studio shows a **"Sync Now"** bar. Click it, or use File → Sync Project with Gradle Files.
4. Pick the **Control Hub** in the device dropdown, keep **TeamCode** selected, and press **Run ▶**.

If the pull complains about "local changes", someone edited files on this PC. Ask Claude before overwriting them.

## FTC Dashboard (control the robot from a browser)

1. Connect the laptop to the Control Hub's Wi-Fi (`FTC-…` network).
2. Open **http://192.168.43.1:8080/dash** in Chrome or Edge.
3. At the top, pick an OpMode, then press **Init** and **Start**. Stop works the same way.
4. Gamepad: plug it into the **laptop**, open the Dashboard's gamepad view, and press a button so the browser detects it. Browser gamepad support can be flaky, so if the Driver Hub is available, use that for driving.
5. Telemetry (power, encoder, velocity) appears live, and numbers can be graphed.

The Dashboard is only for practice and tuning. At competitions you must use the Driver Hub.

## Every time

```bash
git pull                 # get the latest code first
./deploy.sh build        # (Mac) check that it compiles; no robot needed
./deploy.sh              # (Mac) build + install over USB-C
./deploy.sh wifi         # (Mac) same, over the Control Hub's Wi-Fi (join its network first)
git add -A && git commit -m "what changed" && git push
```

Then on the Driver Hub: pick the OpMode (left list = Auto, right list = TeleOp), press **INIT**, then **▶**.

## Robot configuration (one time per Control Hub)

The code expects these device names: `leftDrive`, `rightDrive`, `intakeMotor`, `flywheel`, `crServo`, `pollenServo`.
Either:

- create them by hand on the Driver Hub (⋮ → Configure Robot), using the port table in [NOTES.md](NOTES.md), or
- copy [robot-config/REVStarterBot2026.xml](robot-config/REVStarterBot2026.xml) into the Control Hub's `FIRST` folder with the hub plugged in (`adb push docs/robot-config/REVStarterBot2026.xml /sdcard/FIRST/`), then activate it on the Driver Hub (⋮ → Configure Robot).

## Version matching

The Robot Controller app we install is **SDK v12.0**. The Driver Station app on the Driver Hub should be the same version. Update it with the **REV Hardware Client**, or from Program & Manage → Manage.

## Getting new SDK versions from FIRST

```bash
git fetch upstream
git merge upstream/master
```
