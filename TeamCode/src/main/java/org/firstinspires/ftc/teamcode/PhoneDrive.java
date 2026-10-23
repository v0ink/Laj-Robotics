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
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.ServoControllerEx;

import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Drive with the Robot Remote iPhone app (phone joined to the Control Hub's Wi-Fi).
 *
 *   Left slider  -> left wheel,  Right slider -> right wheel (tank style).
 *     Up = forward, down = backward, middle = stop. The sliders are exact, so there's
 *     only a tiny deadzone: the robot starts moving slowly as soon as you move a slider.
 *   Arm slider   -> left = retract, right = extend. Further = faster, let go = stops
 *     (the servo gets no signal at all, so it stays where it is).
 *
 * The app sends its sliders straight to this OpMode over UDP port 8765, about 30 times a
 * second, as text: "LAJ1,left,right,arm" (each -1..1). This does NOT go through gamepad1,
 * so it works even when a gamepad is plugged into the Driver Hub. The app starts and stops
 * this OpMode through FTC Dashboard.
 *
 * Safety: if no packet arrives for 0.3 s (phone locked, Wi-Fi dropped, app closed),
 * everything stops until the phone is back.
 *
 * Wiring is the same as Controller Drive: left motor MOTOR port 0, right motor
 * MOTOR port 1, arm servo SERVO port 0.
 */
@TeleOp(name = "Phone Drive", group = "Laj")
public class PhoneDrive extends LinearOpMode {

  private static final int UDP_PORT = 8765;
  private static final double TIMEOUT_SECONDS = 0.3;

  private static final int LEFT_PORT = 0;
  private static final int RIGHT_PORT = 1;
  private static final int ARM_SERVO_PORT = 0;

  // Phone sliders: tiny deadzone, then a small starting power and a gentle curve to 1.0.
  //   slider: 0.05 -> 0.06   0.25 -> 0.13   0.5 -> 0.32   0.75 -> 0.61   1.0 -> 1.00
  private static final double DRIVE_DEADZONE = 0.02;
  private static final double DRIVE_MIN_POWER = 0.06;  // raise if small slider moves don't move the robot
  private static final double DRIVE_CURVE = 1.8;       // 1 = straight line, higher = gentler start

  private static final double ARM_DEADZONE = 0.03;
  private static final double ARM_MIN_POWER = 0.08;
  private static final double ARM_CURVE = 1.5;
  // Which way "retract" spins; must match Controller Drive. Flip to 1 if retract goes clockwise.
  private static final int RETRACT_DIRECTION = -1;

  private DcMotorEx leftDrive;
  private DcMotorEx rightDrive;
  private ServoControllerEx servos;

  // Latest values from the phone, written by the network thread
  private volatile double phoneLeft;
  private volatile double phoneRight;
  private volatile double phoneArm;
  private volatile long lastPacketNanos;
  private volatile long packetCount;
  private volatile boolean listening = true;

  @Override
  public void runOpMode() throws InterruptedException {
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    if (!setUpHardware()) {
      telemetry.update();
      waitForStart();
      return;
    }
    armOff();

    DatagramSocket socket;
    try {
      // Reuse the port even if a previous run didn't close it cleanly
      socket = new DatagramSocket(null);
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(UDP_PORT));
      socket.setSoTimeout(200);
    } catch (Exception e) {
      telemetry.addData("Can't open UDP port " + UDP_PORT, e.getMessage());
      telemetry.update();
      waitForStart();
      return;
    }
    Thread receiver = new Thread(() -> receive(socket), "phone receiver");
    receiver.start();

    try {
      // Show whether the phone is talking to us before the OpMode is started
      while (!isStarted() && !isStopRequested()) {
        telemetry.addLine("Ready. Tap Start in the Robot Remote app (or press ▶ here).");
        telemetry.addData("Phone", phoneConnected() ? "connected" : "not heard yet");
        telemetry.update();
        sleep(50);
      }

      while (opModeIsActive()) {
        boolean connected = phoneConnected();
        double leftPower = connected ? shape(phoneLeft, DRIVE_DEADZONE, DRIVE_MIN_POWER, DRIVE_CURVE) : 0;
        double rightPower = connected ? shape(phoneRight, DRIVE_DEADZONE, DRIVE_MIN_POWER, DRIVE_CURVE) : 0;
        // Phone arm slider: left (negative) = retract, right (positive) = extend
        double armPower = connected
            ? RETRACT_DIRECTION * shape(-phoneArm, ARM_DEADZONE, ARM_MIN_POWER, ARM_CURVE) : 0;

        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);
        setArm(armPower);

        telemetry.addData("Phone", connected ? "connected (" + packetCount + " packets)"
            : "NO SIGNAL: everything stopped");
        telemetry.addData("Left  (port " + LEFT_PORT + ")", "slider %.2f -> power %.2f", phoneLeft, leftPower);
        telemetry.addData("Right (port " + RIGHT_PORT + ")", "slider %.2f -> power %.2f", phoneRight, rightPower);
        telemetry.addData("Arm (servo port " + ARM_SERVO_PORT + ")", "slider %.2f -> power %.2f", phoneArm, armPower);
        addBatteryLine();
        telemetry.update();
      }
    } finally {
      leftDrive.setPower(0);
      rightDrive.setPower(0);
      armOff();
      listening = false;
      socket.close();
      receiver.join(500);
    }
  }

  // Runs on its own thread: keeps the newest values the phone sent
  private void receive(DatagramSocket socket) {
    byte[] buffer = new byte[128];
    while (listening) {
      try {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String[] parts = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8)
            .trim().split(",");
        if (parts.length == 4 && parts[0].equals("LAJ1")) {
          phoneLeft = clip(Double.parseDouble(parts[1]));
          phoneRight = clip(Double.parseDouble(parts[2]));
          phoneArm = clip(Double.parseDouble(parts[3]));
          lastPacketNanos = System.nanoTime();
          packetCount++;
        }
      } catch (SocketTimeoutException | NumberFormatException e) {
        // nothing arrived, or a garbled packet: keep listening
      } catch (Exception e) {
        if (!listening) {
          return; // socket closed on purpose
        }
      }
    }
  }

  private boolean phoneConnected() {
    return lastPacketNanos != 0 && (System.nanoTime() - lastPacketNanos) / 1e9 < TIMEOUT_SECONDS;
  }

  private static double clip(double value) {
    return Double.isNaN(value) ? 0 : Math.max(-1, Math.min(1, value));
  }

  // Inside the deadzone: 0. Just past it: minPower, so things start moving slowly right away.
  // Then a curve up to exactly 1.0 at full input. The sign is kept.
  private static double shape(double input, double deadzone, double minPower, double curve) {
    double magnitude = Math.min(1, Math.abs(input));
    if (magnitude < deadzone) {
      return 0;
    }
    double scaled = (magnitude - deadzone) / (1 - deadzone);
    return Math.signum(input) * (minPower + (1 - minPower) * Math.pow(scaled, curve));
  }

  // Continuous servo: power -1..1 becomes a signal around the middle (0.5 = stop).
  // At 0 the signal is switched off entirely, so the servo never creeps.
  private void setArm(double power) {
    if (servos == null) {
      return;
    }
    if (power == 0) {
      armOff();
    } else {
      servos.setServoPwmEnable(ARM_SERVO_PORT);
      servos.setServoPosition(ARM_SERVO_PORT, 0.5 + 0.5 * power);
    }
  }

  private void armOff() {
    if (servos != null) {
      servos.setServoPwmDisable(ARM_SERVO_PORT);
    }
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
    // Same as Controller Drive: the motors face opposite ways, so one is reversed
    leftDrive.setDirection(DcMotorSimple.Direction.FORWARD);
    rightDrive.setDirection(DcMotorSimple.Direction.REVERSE);
    for (DcMotorEx motor : new DcMotorEx[] {leftDrive, rightDrive}) {
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
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
