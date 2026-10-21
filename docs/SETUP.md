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
