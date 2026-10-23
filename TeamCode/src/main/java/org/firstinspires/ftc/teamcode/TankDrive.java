package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.lynx.LynxDcMotorController;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorImplEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.util.List;

/**
 * Tank drive (gamepad 1).
 *
 *   Left stick up/down  -> left wheel:  all the way up = full speed forward,
 *                                       all the way down = full speed backward
 *   Right stick up/down -> right wheel: same
 *
 *   Both sticks up = drive forward, both down = backward,
 *   one up and one down = spin in place.
 *
 * Wiring: left drive motor in Control Hub MOTOR port 0, right drive motor in MOTOR port 1
 * (the REV Starter Bot layout). The motors are found by port number, so the robot
 * configuration on the Driver Hub doesn't need special names.
 */
@TeleOp(name = "Tank Drive", group = "Drive")
public class TankDrive extends LinearOpMode {

  private static final int LEFT_PORT = 0;
  private static final int RIGHT_PORT = 1;

  @Override
  public void runOpMode() {
    // Telemetry goes to both the Driver Hub and FTC Dashboard (http://192.168.43.1:8080/dash)
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    DcMotorController controller = motorController();
    if (controller == null) {
      telemetry.addLine("Can't reach the Control Hub's motor ports. Check the robot configuration.");
      telemetry.update();
      waitForStart();
      return;
    }

    DcMotorEx leftDrive = new DcMotorImplEx(controller, LEFT_PORT);
    DcMotorEx rightDrive = new DcMotorImplEx(controller, RIGHT_PORT);

    // The two motors face opposite directions on the robot, so one is reversed to make
    // "stick up" drive both wheels forward. If the robot drives backward or spins when
    // both sticks are up, swap which motor is reversed.
    leftDrive.setDirection(DcMotorSimple.Direction.FORWARD);
    rightDrive.setDirection(DcMotorSimple.Direction.REVERSE);

    for (DcMotorEx motor : new DcMotorEx[] {leftDrive, rightDrive}) {
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); // stop quickly when sticks are released
      motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    telemetry.addLine("Ready. Press ▶, then Start + A on the gamepad.");
    telemetry.update();

    waitForStart();

    while (opModeIsActive()) {
      // Stick up gives a negative value, so flip it: up = +1 (forward), down = -1 (backward)
      double leftPower = -gamepad1.left_stick_y;
      double rightPower = -gamepad1.right_stick_y;

      leftDrive.setPower(leftPower);
      rightDrive.setPower(rightPower);

      telemetry.addData("Left  (port " + LEFT_PORT + ")", "%.2f", leftPower);
      telemetry.addData("Right (port " + RIGHT_PORT + ")", "%.2f", rightPower);
      LynxModule hub = controlHub();
      if (hub != null) {
        telemetry.addData("Battery", "%.1f V", hub.getInputVoltage(VoltageUnit.VOLTS));
      }
      telemetry.update();
    }

    leftDrive.setPower(0);
    rightDrive.setPower(0);
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

  private LynxModule controlHub() {
    for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
      if (hub.isParent()) {
        return hub;
      }
    }
    return null;
  }
}
