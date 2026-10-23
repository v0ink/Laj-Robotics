# FTC robot code: instructions for Claude

FIRST Tech Challenge team, 2026-27 season. The robot is the REV DUO Starter Bot: a REV Control Hub on the robot, plus a REV Driver Hub and a REV USB gamepad. Read [docs/NOTES.md](docs/NOTES.md) for the hardware, config names and controls, and [docs/SETUP.md](docs/SETUP.md) for build and deploy.

This repo is FIRST's FtcRobotController (remote `upstream`) plus our code. Only edit `TeamCode/` and `docs/`; never modify `FtcRobotController/` or FIRST's Gradle files (our extra dependencies go in `TeamCode/build.gradle`). Check that code compiles with `./deploy.sh build` (macOS).

- Code is Java OpModes for the FTC SDK. There's one class per OpMode, in package `org.firstinspires.ftc.teamcode`, extending `LinearOpMode` and annotated `@TeleOp` or `@Autonomous`.
- Hardware names in `hardwareMap.get(...)` must match the robot configuration exactly: `leftDrive`, `rightDrive`, `intakeMotor`, `flywheel`, `crServo`, `pollenServo`.
- The only OpMode right now is `TankDrive.java`: tank drive, left stick = left motor (MOTOR port 0), right stick = right motor (MOTOR port 1, reversed), throttle curve stick^2.5; left trigger retracts / right trigger extends the arm servo on SERVO port 0 (position stepped by trigger, holds when released). Motors are found by port number, not by configuration name, because the Driver Hub could not edit the configuration (version mismatch). Starter Bot samples and the earlier test OpModes are in git history (REV samples: commit 9df2d73).
- We deploy with Android Studio/Gradle and use **FTC Dashboard** (`com.acmerobotics.dashboard`). Wrap telemetry with `new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry())` so it shows on both the Driver Hub and the Dashboard. Code using Dashboard won't build in OnBot Java.
- Students read this code. Keep it simple, comment the "why", and keep TeleOp controls documented in docs/NOTES.md when they change.
- Every loop must check `opModeIsActive()`, and motors must be stopped when an action ends. The robot is physical, so a stuck loop means a runaway robot.
- Code repo: https://github.com/v0ink/Laj-Robotics (public). After a change compiles (`./deploy.sh build`), commit and push it so the team laptop can pull it. Never commit personal info.
