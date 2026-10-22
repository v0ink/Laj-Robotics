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
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.ServoImpl;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller test: left side drives the motors, right side drives the servos.
 *
 *   Left stick up/down  -> motor speed: further = faster, up = forward, down = backward
 *   Left trigger        -> motor forward: further = faster
 *   Right stick up/down -> servo position: up = one end, down = the other, centered = middle
 *   Right trigger       -> servo from the middle toward the "up" end
 *
 * Every motor port (0-3) and every servo port (0-5) on the Control Hub is driven, so it
 * doesn't matter which port things are plugged into or what the robot configuration says.
 * A servo set to continuous mode spins instead: middle = stopped, ends = full speed.
 * If the robot is built, lift the wheels off the ground first.
 */
@TeleOp(name = "Controller Test", group = "Test")
public class ControllerTest extends LinearOpMode {

  private final List<DcMotorEx> motors = new ArrayList<>();
  private final List<Servo> servos = new ArrayList<>();

  @Override
  public void runOpMode() {
    // Telemetry goes to both the Driver Hub and FTC Dashboard (http://192.168.43.1:8080/dash)
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    setUpMotors();
    setUpServos();

    telemetry.addData("Motor ports found", motors.size());
    telemetry.addData("Servo ports found", servos.size());
    addBatteryLine();
    telemetry.addLine("Ready. Press ▶, then Start + A on the gamepad.");
    telemetry.update();

    waitForStart();

    while (opModeIsActive()) {
      // Left side: motor power from -1 (full backward) to 1 (full forward)
      double stick = -gamepad1.left_stick_y; // stick up gives a negative value, so flip it
      double motorPower = gamepad1.left_trigger > Math.abs(stick) ? gamepad1.left_trigger : stick;
      for (DcMotorEx motor : motors) {
        motor.setPower(motorPower);
      }

      // Right side: servo position from 0 to 1, 0.5 is the middle
      double servoStick = -gamepad1.right_stick_y;
      double servoInput = gamepad1.right_trigger > Math.abs(servoStick) ? gamepad1.right_trigger : servoStick;
      double servoPosition = 0.5 + 0.5 * servoInput;
      for (Servo servo : servos) {
        servo.setPosition(servoPosition);
      }

      telemetry.addData("Motor power", "%.2f", motorPower);
      telemetry.addData("Servo position", "%.2f", servoPosition);
      for (int port = 0; port < motors.size(); port++) {
        DcMotorEx motor = motors.get(port);
        // More than ~0.2 A on a port means a motor there is really being driven
        telemetry.addData("Motor port " + port, "%.2f A  enc %d",
            motor.getCurrent(CurrentUnit.AMPS), motor.getCurrentPosition());
      }
      addBatteryLine();
      telemetry.update();
    }

    for (DcMotorEx motor : motors) {
      motor.setPower(0);
    }
  }

  // Motor ports 0-3 of the Control Hub, using the configuration's controller when it has one
  private void setUpMotors() {
    DcMotorController controller = null;
    List<DcMotorController> configured = hardwareMap.getAll(DcMotorController.class);
    if (!configured.isEmpty()) {
      controller = configured.get(0);
    } else if (controlHub() != null) {
      try {
        controller = new LynxDcMotorController(hardwareMap.appContext, controlHub());
      } catch (Exception e) {
        telemetry.addData("Motors unavailable", e.getMessage());
      }
    }
    if (controller == null) {
      return;
    }
    for (int port = 0; port < 4; port++) {
      DcMotorEx motor = new DcMotorImplEx(controller, port);
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
      motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
      motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
      motors.add(motor);
    }
  }

  // Servo ports 0-5 of the Control Hub, using the configuration's controller when it has one
  private void setUpServos() {
    ServoController controller = null;
    List<ServoController> configured = hardwareMap.getAll(ServoController.class);
    if (!configured.isEmpty()) {
      controller = configured.get(0);
    } else if (controlHub() != null) {
      try {
        controller = new LynxServoController(hardwareMap.appContext, controlHub());
      } catch (Exception e) {
        telemetry.addData("Servos unavailable", e.getMessage());
      }
    }
    if (controller == null) {
      return;
    }
    for (int port = 0; port < 6; port++) {
      servos.add(new ServoImpl(controller, port));
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

  private void addBatteryLine() {
    LynxModule hub = controlHub();
    if (hub != null) {
      telemetry.addData("Battery", "%.1f V", hub.getInputVoltage(VoltageUnit.VOLTS));
    }
  }
}
