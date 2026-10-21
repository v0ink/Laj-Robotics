package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * First test: spin one motor with the gamepad.
 *
 * Setup on the Driver Hub (⋮ → Configure Robot): add one motor on any motor port
 * and name it exactly "testMotor".
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
    // INIT: find the motor by its configured name
    DcMotor motor = hardwareMap.get(DcMotor.class, MOTOR_NAME);
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
