# FTC robot code: instructions for Claude

FIRST Tech Challenge team, 2026-27 season. The robot is the REV DUO Starter Bot: a REV Control Hub on the robot, plus a REV Driver Hub and a REV USB gamepad. Read [docs/NOTES.md](docs/NOTES.md) for the hardware, config names and controls, and [docs/SETUP.md](docs/SETUP.md) for build and deploy.

This repo is FIRST's FtcRobotController (remote `upstream`) plus our code. Only edit `TeamCode/` and `docs/`; never modify `FtcRobotController/` or the Gradle files. Check that code compiles with `./deploy.sh build` (macOS).

- Code is Java OpModes for the FTC SDK. There's one class per OpMode, in package `org.firstinspires.ftc.teamcode`, extending `LinearOpMode` and annotated `@TeleOp` or `@Autonomous`.
- Hardware names in `hardwareMap.get(...)` must match the robot configuration exactly: `leftDrive`, `rightDrive`, `intakeMotor`, `flywheel`, `crServo`, `pollenServo`.
- Our OpModes start from REV's working samples (`StarterBotTeleOp.java`, `StarterBotAuto.java`). They use newer SDK APIs (`gamepad1.crossWasPressed()`, `right_trigger_pressed`), so the SDK must be a current release.
- Keep code compatible with OnBot Java too: plain Java, no external libraries unless we decide to switch to Gradle-only.
- Students read this code. Keep it simple, comment the "why", and keep TeleOp controls documented in docs/NOTES.md when they change.
- Every loop must check `opModeIsActive()`, and motors must be stopped when an action ends. The robot is physical, so a stuck loop means a runaway robot.
- Code repo: https://github.com/v0ink/Laj-Robotics (public). Never push without the user asking, and never commit personal info.
