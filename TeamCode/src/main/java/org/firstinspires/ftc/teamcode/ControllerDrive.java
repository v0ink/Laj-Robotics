package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.lynx.LynxDcMotorController;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxServoController;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorImplEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.ServoControllerEx;

import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.util.List;

/**
 * Drive with the REV gamepad on the Driver Hub (tank style).
 *
 *   Left stick up/down  -> left wheel,  Right stick up/down -> right wheel.
 *     A light push starts slow right away; full speed only at full push.
 *     Both up = forward, both down = backward, one up + one down = spin in place.
 *
 *   Left trigger  -> retract the arm (counter-clockwise, looking at the front of the servo)
 *   Right trigger -> extend the arm  (clockwise)
 *     The arm servo is in continuous-rotation mode, so the trigger sets its SPEED:
 *     light pull = slow, full pull = full speed. Let go and the servo gets no signal at
 *     all, so it stays where it is (it can never run off on its own).
 *
 * Wiring: left drive motor in MOTOR port 0, right drive motor in MOTOR port 1,
 * arm servo in SERVO port 0. Everything is found by port number, so the robot
 * configuration on the Driver Hub doesn't need special names.
 *
 * For the iPhone app use Phone Drive instead.
 */
@TeleOp(name = "Controller Drive", group = "Laj")
public class ControllerDrive extends LinearOpMode {

  private static final int LEFT_PORT = 0;
  private static final int RIGHT_PORT = 1;
  private static final int ARM_SERVO_PORT = 0;

  // Drive sticks. Past the deadzone the motor starts at DRIVE_MIN_POWER (just enough to
  // get the wheels turning), then the power grows along a curve to 1.0 at full push.
  //   stick: 0.1 -> 0.08   0.25 -> 0.12   0.5 -> 0.29   0.75 -> 0.58   1.0 -> 1.00
  private static final double DRIVE_DEADZONE = 0.05;   // ignore stick wobble when released
  private static final double DRIVE_MIN_POWER = 0.08;  // raise if light pushes don't move the robot
  private static final double DRIVE_CURVE = 2.0;       // 1 = straight line, higher = gentler start

  // Arm triggers, same idea: slow from the first bit of pull, full speed only when fully pulled
  private static final double ARM_DEADZONE = 0.03;
  private static final double ARM_MIN_POWER = 0.08;
  private static final double ARM_CURVE = 1.5;
  // Which way "retract" spins. If the left trigger turns the arm clockwise
  // (looking at the front of the servo), change this to 1.
  private static final int RETRACT_DIRECTION = -1;

  private DcMotorEx leftDrive;
  private DcMotorEx rightDrive;
  private ServoControllerEx servos;
  private boolean armSignalOn;
  private long armLastActiveNanos;
  // Switch the arm servo's signal off after this long without input
  private static final double ARM_IDLE_OFF_SECONDS = 1.0;

  @Override
  public void runOpMode() {
    // Telemetry goes to both the Driver Hub and FTC Dashboard (http://192.168.43.1:8080/dash)
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    if (!setUpHardware()) {
      telemetry.update();
      waitForStart();
      return;
    }
    armOff(); // no servo signal until a trigger is pulled, so the arm can't move by itself

    telemetry.addLine("Ready. Press ▶, then Start + A on the gamepad.");
    telemetry.update();
    waitForStart();

    try {
      while (opModeIsActive()) {
        // Sticks: up gives a negative value, so flip it (up = forward)
        double leftPower = shape(-gamepad1.left_stick_y, DRIVE_DEADZONE, DRIVE_MIN_POWER, DRIVE_CURVE);
        double rightPower = shape(-gamepad1.right_stick_y, DRIVE_DEADZONE, DRIVE_MIN_POWER, DRIVE_CURVE);
        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);

        // Triggers: left retracts, right extends; pulling both cancels out
        double armInput = gamepad1.left_trigger - gamepad1.right_trigger;
        double armPower = RETRACT_DIRECTION * shape(armInput, ARM_DEADZONE, ARM_MIN_POWER, ARM_CURVE);
        setArm(armPower);

        if (gamepad1.getGamepadId() == Gamepad.ID_UNASSOCIATED) {
          telemetry.addLine("No gamepad! Hold Start + A on the gamepad.");
        }
        telemetry.addData("Left  (port " + LEFT_PORT + ")", "stick %.2f -> power %.2f",
            -gamepad1.left_stick_y, leftPower);
        telemetry.addData("Right (port " + RIGHT_PORT + ")", "stick %.2f -> power %.2f",
            -gamepad1.right_stick_y, rightPower);
        telemetry.addData("Arm (servo port " + ARM_SERVO_PORT + ")", "LT %.2f RT %.2f -> power %.2f",
            gamepad1.left_trigger, gamepad1.right_trigger, armPower);
        addBatteryLine();
        telemetry.update();
      }
    } finally {
      // Always leave everything stopped, even if something above went wrong
      leftDrive.setPower(0);
      rightDrive.setPower(0);
      armOff();
    }
  }

  // Video-game style input shaping. Inside the deadzone: 0. Just past it: minPower, so
  // things start moving slowly right away. Then a curve up to exactly 1.0 at full input.
  // The sign is kept, so backward stays backward.
  private static double shape(double input, double deadzone, double minPower, double curve) {
    double magnitude = Math.min(1, Math.abs(input));
    if (magnitude < deadzone) {
      return 0;
    }
    double scaled = (magnitude - deadzone) / (1 - deadzone); // 0..1 past the deadzone
    return Math.signum(input) * (minPower + (1 - minPower) * Math.pow(scaled, curve));
  }

  // Continuous servo: power -1..1 becomes a signal around the middle (0.5 = stop).
  // The signal stays on while the arm is in use (switching it on and off every loop made the
  // servo laggy and jerky). After ARM_IDLE_OFF_SECONDS of no input it's switched off, so the
  // servo can never creep or run off by itself.
  private void setArm(double power) {
    if (servos == null) {
      return;
    }
    if (power != 0) {
      armLastActiveNanos = System.nanoTime();
      if (!armSignalOn) {
        servos.setServoPwmEnable(ARM_SERVO_PORT);
        armSignalOn = true;
      }
      servos.setServoPosition(ARM_SERVO_PORT, 0.5 + 0.5 * power);
    } else if (armSignalOn) {
      if ((System.nanoTime() - armLastActiveNanos) / 1e9 > ARM_IDLE_OFF_SECONDS) {
        armOff();
      } else {
        servos.setServoPosition(ARM_SERVO_PORT, 0.5); // stop, signal still on
      }
    }
  }

  private void armOff() {
    if (servos != null) {
      servos.setServoPwmDisable(ARM_SERVO_PORT);
    }
    armSignalOn = false;
  }

  // ---- Hardware set-up (by port number, works with any robot configuration) ----

  private boolean setUpHardware() {
    LynxModule hub = controlHub();
    DcMotorController motorController = null;
    List<DcMotorController> configuredMotors = hardwareMap.getAll(DcMotorController.class);
    if (!configuredMotors.isEmpty()) {
      motorController = configuredMotors.get(0);
    } else if (hub != null) {
      try {
        motorController = new LynxDcMotorController(hardwareMap.appContext, hub);
      } catch (Exception e) {
        telemetry.addData("Motor controller error", e.getMessage());
      }
    }
    if (motorController == null) {
      telemetry.addLine("Can't reach the Control Hub's motor ports. Check the robot configuration.");
      return false;
    }

    leftDrive = new DcMotorImplEx(motorController, LEFT_PORT);
    rightDrive = new DcMotorImplEx(motorController, RIGHT_PORT);
    // The motors face opposite ways on the robot, so one is reversed to make "stick up"
    // drive both wheels forward. If the robot spins when both sticks are up, swap these.
    leftDrive.setDirection(DcMotorSimple.Direction.FORWARD);
    rightDrive.setDirection(DcMotorSimple.Direction.REVERSE);
    for (DcMotorEx motor : new DcMotorEx[] {leftDrive, rightDrive}) {
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); // stop quickly when released
      motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
      motor.setPower(0);
    }

    List<ServoController> configuredServos = hardwareMap.getAll(ServoController.class);
    if (!configuredServos.isEmpty() && configuredServos.get(0) instanceof ServoControllerEx) {
      servos = (ServoControllerEx) configuredServos.get(0);
    } else if (hub != null) {
      try {
        servos = new LynxServoController(hardwareMap.appContext, hub);
      } catch (Exception e) {
        telemetry.addData("Servo controller error", e.getMessage());
      }
    }
    if (servos == null) {
      telemetry.addLine("Servo port not available: the arm won't move.");
    }
    return true;
  }

  private LynxModule controlHub() {
    for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
      if (hub.isParent()) {
        return hub;
      }
    }
    return null;
  }

  private void addBatteryLine() {
    LynxModule hub = controlHub();
    if (hub != null) {
      telemetry.addData("Battery", "%.1f V", hub.getInputVoltage(VoltageUnit.VOLTS));
    }
  }
}
