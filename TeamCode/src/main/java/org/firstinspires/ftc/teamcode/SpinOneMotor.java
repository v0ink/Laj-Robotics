package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import java.util.List;

/**
 * First test: spin one motor with the gamepad.
 *
 * Uses the motor named "testMotor" if the robot configuration has one.
 * Otherwise it uses the first motor in the active configuration, so it works
 * with whatever configuration is already on the robot.
 *
 * Controls (gamepad1):
 *   Left stick up/down  -> motor speed, forward/backward
 *   Hold A (cross)      -> spin forward at half speed
 *   Let go              -> motor stops
 */
@TeleOp(name = "Spin One Motor", group = "Test")
public class SpinOneMotor extends LinearOpMode {

  // Must match the name in the robot configuration on the Driver Hub
  private static final String MOTOR_NAME = "testMotor";

  @Override
  public void runOpMode() {
    // Send telemetry to both the Driver Hub and FTC Dashboard (http://192.168.43.1:8080/dash)
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    // INIT: list every motor in the active configuration, so we can see the names
    List<DcMotor> allMotors = hardwareMap.getAll(DcMotor.class);
    StringBuilder names = new StringBuilder();
    for (DcMotor m : allMotors) {
      names.append(hardwareMap.getNamesOf(m)).append(' ');
    }

    // Prefer "testMotor"; otherwise fall back to the first configured motor
    DcMotor motor = hardwareMap.tryGet(DcMotor.class, MOTOR_NAME);
    if (motor == null && !allMotors.isEmpty()) {
      motor = allMotors.get(0);
    }
    if (motor == null) {
      telemetry.addLine("No motors in the active robot configuration.");
      telemetry.addLine("Add one (Configure Robot or FTC Dashboard) and try again.");
      telemetry.update();
      waitForStart();
      return;
    }
    telemetry.addData("Using motor", hardwareMap.getNamesOf(motor));
    telemetry.addData("All motors", names.length() > 0 ? names.toString() : "(none)");

    motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

    telemetry.addLine("Ready. Press ▶, then push the left stick or hold A.");
    telemetry.update();

    waitForStart();

    while (opModeIsActive()) {
      // Stick up gives a negative value, so flip it so "up" means forward
      double power = -gamepad1.left_stick_y;
      if (gamepad1.a) {
        power = 0.5;
      }
      motor.setPower(power);

      telemetry.addData("Power", "%.2f", power);
      telemetry.addData("Encoder position", motor.getCurrentPosition());
      telemetry.update();
    }

    motor.setPower(0);
  }
}
