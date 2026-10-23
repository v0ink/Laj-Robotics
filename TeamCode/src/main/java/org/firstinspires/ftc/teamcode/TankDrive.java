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
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.ServoImpl;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.util.List;

/**
 * Tank drive + servo arm (gamepad 1).
 *
 *   Left stick up/down  -> left wheel,  Right stick up/down -> right wheel.
 *     All the way = full speed. Small pushes are extra gentle (like a video game
 *     throttle curve), so it's easy to creep slowly and still reach full speed.
 *     Both up = forward, both down = backward, one up + one down = spin in place.
 *
 *   Left trigger  -> retract the arm (counter-clockwise, looking at the front of the servo)
 *   Right trigger -> extend the arm  (clockwise)
 *     Pull further = moves faster. Let go = the arm stays where it is.
 *
 * Wiring: left drive motor in MOTOR port 0, right drive motor in MOTOR port 1,
 * arm servo in SERVO port 0. Everything is found by port number, so the robot
 * configuration on the Driver Hub doesn't need special names.
 */
@TeleOp(name = "Tank Drive", group = "Drive")
public class TankDrive extends LinearOpMode {

  private static final int LEFT_PORT = 0;
  private static final int RIGHT_PORT = 1;
  private static final int ARM_SERVO_PORT = 0;

  // Throttle curve: power = stick^CURVE. 1 = straight line, higher = gentler at the start.
  // At 2.5, a half push gives ~15% power, 3/4 push ~45%, and a full push 100%.
  private static final double CURVE = 2.5;
  // Ignore tiny stick wobble so the robot doesn't creep when the sticks are released
  private static final double DEADZONE = 0.05;

  // How fast the arm moves with a trigger fully pulled, in servo range per second
  // (servo range is 0 to 1, so 0.5 = half the servo's travel in one second)
  private static final double ARM_SPEED = 0.5;
  // Limits of the arm's travel (0 to 1). Narrow these if the arm hits the robot.
  private static final double ARM_MIN = 0.0;
  private static final double ARM_MAX = 1.0;
  // Position the arm jumps to the first time a trigger is pulled (a servo can't report
  // where it is, so we have to start somewhere)
  private static final double ARM_START = 0.5;
  // Which way "retract" turns the servo. If the left trigger turns the arm clockwise
  // (looking at the front of the servo), change this to -1.
  private static final int RETRACT_DIRECTION = 1;

  @Override
  public void runOpMode() {
    // Telemetry goes to both the Driver Hub and FTC Dashboard (http://192.168.43.1:8080/dash)
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    DcMotorController motorController = motorController();
    if (motorController == null) {
      telemetry.addLine("Can't reach the Control Hub's motor ports. Check the robot configuration.");
      telemetry.update();
      waitForStart();
      return;
    }

    DcMotorEx leftDrive = new DcMotorImplEx(motorController, LEFT_PORT);
    DcMotorEx rightDrive = new DcMotorImplEx(motorController, RIGHT_PORT);

    // The two motors face opposite directions on the robot, so one is reversed to make
    // "stick up" drive both wheels forward. If the robot drives backward or spins when
    // both sticks are up, swap which motor is reversed.
    leftDrive.setDirection(DcMotorSimple.Direction.FORWARD);
    rightDrive.setDirection(DcMotorSimple.Direction.REVERSE);

    for (DcMotorEx motor : new DcMotorEx[] {leftDrive, rightDrive}) {
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); // stop quickly when sticks are released
      motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    ServoController servoController = servoController();
    Servo arm = servoController != null ? new ServoImpl(servoController, ARM_SERVO_PORT) : null;
    double armPosition = Double.NaN; // unknown until a trigger is first pulled

    telemetry.addLine("Ready. Press ▶, then Start + A on the gamepad.");
    if (arm == null) {
      telemetry.addLine("Servo port not available: the arm won't move.");
    }
    telemetry.update();

    waitForStart();
    ElapsedTime loopTimer = new ElapsedTime();

    while (opModeIsActive()) {
      double dt = loopTimer.seconds();
      loopTimer.reset();

      // Sticks: up gives a negative value, so flip it (up = forward)
      double leftPower = throttleCurve(-gamepad1.left_stick_y);
      double rightPower = throttleCurve(-gamepad1.right_stick_y);
      leftDrive.setPower(leftPower);
      rightDrive.setPower(rightPower);

      // Triggers: right extends, left retracts; pulling both cancels out
      double armInput = gamepad1.right_trigger - gamepad1.left_trigger;
      if (arm != null && Math.abs(armInput) > DEADZONE) {
        if (Double.isNaN(armPosition)) {
          armPosition = ARM_START;
        }
        armPosition -= RETRACT_DIRECTION * armInput * ARM_SPEED * dt;
        armPosition = Range.clip(armPosition, ARM_MIN, ARM_MAX);
        arm.setPosition(armPosition);
      }

      telemetry.addData("Left  (port " + LEFT_PORT + ")", "stick %.2f -> power %.2f",
          -gamepad1.left_stick_y, leftPower);
      telemetry.addData("Right (port " + RIGHT_PORT + ")", "stick %.2f -> power %.2f",
          -gamepad1.right_stick_y, rightPower);
      telemetry.addData("Arm servo (port " + ARM_SERVO_PORT + ")",
          Double.isNaN(armPosition) ? "not moved yet" : String.format("%.2f", armPosition));
      LynxModule hub = controlHub();
      if (hub != null) {
        telemetry.addData("Battery", "%.1f V", hub.getInputVoltage(VoltageUnit.VOLTS));
      }
      telemetry.update();
    }

    leftDrive.setPower(0);
    rightDrive.setPower(0);
  }

  // Video-game style throttle: small pushes give very little power, a full push gives 1.0.
  // The sign is kept, so backward stays backward.
  private static double throttleCurve(double stick) {
    double magnitude = Math.abs(stick);
    if (magnitude < DEADZONE) {
      return 0;
    }
    // Rescale so the curve starts at 0 right at the edge of the deadzone
    double scaled = (magnitude - DEADZONE) / (1 - DEADZONE);
    return Math.signum(stick) * Math.pow(scaled, CURVE);
  }

  // The Control Hub's motor controller: from the robot configuration if it has one,
  // otherwise created directly so driving works even with an empty configuration
  private DcMotorController motorController() {
    List<DcMotorController> configured = hardwareMap.getAll(DcMotorController.class);
    if (!configured.isEmpty()) {
      return configured.get(0);
    }
    LynxModule hub = controlHub();
    if (hub == null) {
      return null;
    }
    try {
      return new LynxDcMotorController(hardwareMap.appContext, hub);
    } catch (Exception e) {
      telemetry.addData("Motor controller error", e.getMessage());
      return null;
    }
  }

  // Same idea for the servo ports
  private ServoController servoController() {
    List<ServoController> configured = hardwareMap.getAll(ServoController.class);
    if (!configured.isEmpty()) {
      return configured.get(0);
    }
    LynxModule hub = controlHub();
    if (hub == null) {
      return null;
    }
    try {
      return new LynxServoController(hardwareMap.appContext, hub);
    } catch (Exception e) {
      telemetry.addData("Servo controller error", e.getMessage());
      return null;
    }
  }

  private LynxModule controlHub() {
    for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
      if (hub.isParent()) {
        return hub;
      }
    }
    return null;
  }
}
