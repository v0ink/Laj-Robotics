# Robotics team notes (FTC 2026-27)

Everything we've figured out so far about programming the robot.
Diagram: [images/how-code-gets-to-the-robot.svg](images/how-code-gets-to-the-robot.svg)

## Hardware (REV Robotics)

| Device | Where | What it does |
|---|---|---|
| **REV Control Hub** (label says "Controller") | On the robot | Android computer with no screen. Runs the **Robot Controller app** plus **our code**. Wired to the motors, servos and battery. Broadcasts its own Wi-Fi. |
| **REV Driver Hub** | Handheld | Runs the **Driver Station app**. Pick the OpMode, press INIT / ▶ / ■, see telemetry. **No code goes here.** |
| **REV USB gamepad** (PS4-style) | Plugs into the Driver Hub | Driver input. |

- Kit: **REV FTC Starter Kit V3.1** (REV-45-3529), built as the **2026-27 REV DUO FTC Starter Bot**.
- The Control Hub and the Driver Hub talk over Wi-Fi.
- The Robot Controller app version **must match** the Driver Station app version. Update both with the **REV Hardware Client** (a desktop app).
- App versions: **TODO**. Check Driver Hub ⋮ → About.

## Robot configuration (REV Starter Bot)

The names must match **exactly** what's in Driver Hub ⋮ → Configure Robot, because the code finds devices by name.
REV's ready-made config file is [robot-config/REVStarterBot2026.xml](robot-config/REVStarterBot2026.xml). Copy it to the Control Hub's `FIRST` folder, then activate it on the Driver Hub.

| Port | Device | Config name | Job |
|---|---|---|---|
| Motor 0 | UltraPlanetary HD Hex | `leftDrive` | Left drive wheel |
| Motor 1 | UltraPlanetary HD Hex | `rightDrive` | Right drive wheel (reversed in code) |
| Motor 2 | Core Hex | `intakeMotor` | Intake in/out |
| Motor 3 | UltraPlanetary HD Hex | `flywheel` | Launcher (velocity control, target 1160) |
| Servo 0 | Smart Servo, continuous mode | `crServo` | Feeder into the flywheel |
| Servo 1 | Smart Servo, standard | `pollenServo` | Pollen poker (pos 0 = down, 1 = up) |

Drivetrain: 2-motor tank drive (traction and omni wheels), driven **arcade style** on the left stick.
Wiring diagram: [images/wiring.png](images/wiring.png)

## REV starter TeleOp controls (gamepad1)

| Input | Action |
|---|---|
| Left stick | Drive (arcade) |
| Right bumper | Intake in |
| Right trigger | Intake out |
| Left bumper | Spin up flywheel, auto-feed when at speed |
| Triangle / Y | Flywheel to target velocity only |
| Square / X | Reverse flywheel (55%) |
| D-pad up / down | Feeder forward / reverse |
| Cross / A | Pollen poker down |
| Circle / B | Pollen poker up |

Picture: [images/gamepad-layout.png](images/gamepad-layout.png)

## The match

- **Autonomous, 30 s:** nobody touches the controllers; the Auto OpMode runs on its own.
- **Transition, ~8 s:** drivers pick up the gamepads.
- **TeleOp, 2 min:** drivers control the robot through the TeleOp OpMode.

REV's sample Auto: back up for 0.7 s, then spin the flywheel for 15 s, feeding pollen whenever it's at speed. There's an optional 10 s delay for starting on the far side.

## How the code works

- Language: **Java**. It's the same Java whether we use OnBot Java or Android Studio/Gradle.
- **OpMode** = one program you pick on the Driver Hub. It's a Java class that `extends LinearOpMode` with `@TeleOp` or `@Autonomous` on it. There's one file per OpMode.
  - Code before `waitForStart()` runs on **INIT**.
  - After ▶, the code below runs, usually a `while (opModeIsActive())` loop.
- Our OpModes run **inside the Robot Controller app on the Control Hub**.
- The FTC SDK ([FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController)) is the full source of that app. Our code goes in its `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/` folder.

## Two ways to get code onto the robot

**A. Gradle build + USB (main plan)**
- Write the code with Claude Code (on the Mac or the Windows PC).
- Build the whole Robot Controller app.
- Plug into the **Control Hub's USB-C port** and install. You can also use Wi-Fi adb: `adb connect 192.168.43.1:5555`.
- Android Studio is optional. On the Mac, run `./deploy.sh`. See [SETUP.md](SETUP.md).
- Needs **JDK 17** and the **Android SDK**. Windows may need Google's USB driver.

**B. OnBot Java (backup, e.g. at competitions)**
- Join the laptop to the **Control Hub's Wi-Fi**, open `http://192.168.43.1:8080`, and choose OnBot Java.
- Upload or paste the `.java` files, then click Build. The **Control Hub compiles them itself**; no install or cable needed.
- Blocks lives on the same page.

## FTC Dashboard

A browser control panel served by the robot at **http://192.168.43.1:8080/dash** (laptop on the robot's Wi-Fi). It does live telemetry and graphs, picks, INITs and starts OpModes, and takes a gamepad plugged into the laptop. Use it when the Driver Hub is dead or for tuning; it's not allowed in matches. See [SETUP.md](SETUP.md).

## Code sharing

- GitHub repo: https://github.com/v0ink/Laj-Robotics (**public**, so no personal info, forms or passwords go in it)
- Both the Mac and the Windows PC work from this repo: `git pull` before working, commit and `git push` after.
- `upstream` points to FIRST's FtcRobotController. Merge it to pick up new SDK versions.
- Don't sync the Gradle project through Google Drive.

## Corrections to the old programming notebook

- The ⋮ → "Program & Manage" menu is in the Driver Station app on the **Driver Hub**. The Control Hub has no screen.
- "Deploy to device" means the Control Hub, not the Driver Hub.

## Troubleshooting we've hit

- **"Application installation failed… different signature"** (first install from Android Studio): click OK. It replaces REV's preinstalled Robot Controller app, which only happens once per computer.
- **"Unknown host repo.maven.apache.org"**: the PC has no internet (it's on the robot's Wi-Fi). Switch to normal internet and sync.
- **Driver Hub says connected but Configure Robot says "not connected", plus a "does not match" warning**: the Driver Station app version is older than our Robot Controller (12.0). Update the Driver Hub with the REV Hardware Client.
- **Code says power 1.00 but the motor doesn't spin (hub ~0.18 A)**: the active configuration has the motor on a different port. The test OpMode **Any Input Motor** drives all four ports to get around this; the real fix is a correct configuration.
- USB can't power motors: the 12 V battery and switch must be on.

## Open TODOs

- [ ] Check the app versions on the Driver Hub (⋮ → About).
- [ ] Check whether REV's robot config is loaded and active on the Driver Hub.
- [x] Set up the repo with the FTC SDK (v12.0) and REV's starter TeleOp and Auto.
- [x] Mac toolchain: openjdk@17 and Android SDK via Homebrew. Build verified.
- [ ] Windows PC toolchain: see [SETUP.md](SETUP.md).
- [x] First deploy to the Control Hub from the Windows PC. A motor spins with Any Input Motor (2026-10-22).
- [ ] Update the Driver Hub's Driver Station app to 12.0 (REV Hardware Client), then make the real robot configuration.
- [ ] Test the starter TeleOp and Auto on the real robot.
- [ ] Customize: tune `targetVelocity`, drive speed and Auto timing to our robot.

## Links

- REV Starter Bot programming docs: https://docs.revrobotics.com/ftc-kickoff-concepts/home/programming-teleop-and-auto
- FTC docs (official): https://ftc-docs.firstinspires.org
- FTC SDK: https://github.com/FIRST-Tech-Challenge/FtcRobotController
- FTC SIM (practice in the browser, Blocks/OnBot Java): https://ftcsim.org
- Kit: https://www.revrobotics.com/rev-45-3529/
