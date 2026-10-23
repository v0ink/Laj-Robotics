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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Drive the robot from the iPhone app (phone joined to the Control Hub's Wi-Fi).
 *
 * The phone sends a small text packet about 30 times a second over UDP to port 8765:
 *     "left,right,arm"   e.g. "0.80,-0.80,0.35"
 *   left / right = tank sticks, -1 (full back) to 1 (full forward)
 *   arm          = servo slider, 0 to 1 (an exact position, not a speed)
 *
 * Safety: if no packet arrives for 0.5 s (phone locked, Wi-Fi dropped, app closed)
 * the wheels stop. The phone app can INIT and start this OpMode itself (via FTC Dashboard
 * on port 8000), or you can press INIT then ▶ on the Driver Hub.
 *
 * Wiring is the same as TankDrive: left motor in MOTOR port 0, right motor in
 * MOTOR port 1, arm servo in SERVO port 0.
 */
@TeleOp(name = "Phone Drive", group = "Drive")
public class PhoneDrive extends LinearOpMode {

  private static final int PORT = 8765;
  private static final int LEFT_PORT = 0;
  private static final int RIGHT_PORT = 1;
  private static final int ARM_SERVO_PORT = 0;

  // Stop the wheels if the phone goes quiet for this long
  private static final double TIMEOUT_SECONDS = 0.5;

  // Same throttle curve as TankDrive: gentle near the middle, full power at the end
  private static final double CURVE = 2.5;
  private static final double DEADZONE = 0.05;

  // Limits of the arm's travel (0 to 1). Narrow these if the arm hits the robot.
  private static final double ARM_MIN = 0.0;
  private static final double ARM_MAX = 1.0;

  @Override
  public void runOpMode() {
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
    // Motors face opposite ways on the robot, so one is reversed (same as TankDrive)
    leftDrive.setDirection(DcMotorSimple.Direction.FORWARD);
    rightDrive.setDirection(DcMotorSimple.Direction.REVERSE);
    for (DcMotorEx motor : new DcMotorEx[] {leftDrive, rightDrive}) {
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
      motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    ServoController servoController = servoController();
    Servo arm = servoController != null ? new ServoImpl(servoController, ARM_SERVO_PORT) : null;

    DatagramSocket socket;
    try {
      socket = new DatagramSocket(PORT);
      socket.setSoTimeout(50); // wake up every 50 ms so we can check opModeIsActive() and the timeout
    } catch (Exception e) {
      telemetry.addData("Can't open UDP port " + PORT, e.getMessage());
      telemetry.update();
      waitForStart();
      return;
    }

    telemetry.addLine("Ready. Tap Start robot in the phone app (or press ▶ here).");
    telemetry.update();
    waitForStart();

    byte[] buffer = new byte[128];
    ElapsedTime sinceLastPacket = new ElapsedTime();
    boolean connected = false;
    double leftPower = 0;
    double rightPower = 0;
    double armPosition = Double.NaN;

    try {
      while (opModeIsActive()) {
        try {
          DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
          socket.receive(packet);
          String[] parts = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8)
              .trim().split(",");
          if (parts.length == 3) {
            leftPower = throttleCurve(Double.parseDouble(parts[0]));
            rightPower = throttleCurve(Double.parseDouble(parts[1]));
            armPosition = Range.clip(Double.parseDouble(parts[2]), ARM_MIN, ARM_MAX);
            sinceLastPacket.reset();
            connected = true;
          }
        } catch (java.net.SocketTimeoutException e) {
          // nothing arrived in 50 ms, fall through to the timeout check
        } catch (NumberFormatException e) {
          // garbled packet: ignore it
        } catch (java.io.IOException e) {
          telemetry.addData("Network error", e.getMessage());
        }

        // Failsafe: lost the phone, so stop the wheels (the arm just stays where it is)
        if (sinceLastPacket.seconds() > TIMEOUT_SECONDS) {
          leftPower = 0;
          rightPower = 0;
          connected = false;
        }

        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);
        if (arm != null && !Double.isNaN(armPosition)) {
          arm.setPosition(armPosition);
        }

        telemetry.addData("Phone", connected ? "connected" : "no signal (wheels stopped)");
        telemetry.addData("Left / Right", "%.2f / %.2f", leftPower, rightPower);
        telemetry.addData("Arm", Double.isNaN(armPosition) ? "waiting" : String.format("%.2f", armPosition));
        telemetry.update();
      }
    } finally {
      leftDrive.setPower(0);
      rightDrive.setPower(0);
      socket.close();
    }
  }

  private static double throttleCurve(double stick) {
    stick = Range.clip(stick, -1, 1);
    double magnitude = Math.abs(stick);
    if (magnitude < DEADZONE) {
      return 0;
    }
    double scaled = (magnitude - DEADZONE) / (1 - DEADZONE);
    return Math.signum(stick) * Math.pow(scaled, CURVE);
  }

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
