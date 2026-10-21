package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name = "Starter Bot TeleOp", group = "Starter")
public class StarterBotTeleOp extends LinearOpMode {

  private DcMotor intakeMotor;
  private DcMotor flywheel;
  private CRServo crServo;
  private DcMotor rightDrive;
  private Servo pollenServo;
  private DcMotor leftDrive;

  int targetVelocity;
  
    /**
   * The section below contains the iterative OpMode for the robot to operate so
   * long as "Stop" has not been pressed OR communication to the Driver Station
   * has not been lost.
   */
  @Override
  public void runOpMode() {
    intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");
    flywheel = hardwareMap.get(DcMotor.class, "flywheel");
    crServo = hardwareMap.get(CRServo.class, "crServo");
    rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");
    pollenServo = hardwareMap.get(Servo.class, "pollenServo");
    leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");

    // Setting up motors and variables
    rightDrive.setDirection(DcMotor.Direction.REVERSE);
    flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    targetVelocity = 1160;
    waitForStart();
    if (opModeIsActive()) {
      // Ensures the servo is up. Runs once after "Play" is pressed.
      pollenServo.setPosition(1);
      // Loop for the OpMode containing the functions
      while (opModeIsActive()) {
        intakeControls();
        pollenPoker();
        arcadeDrive();
        flywheelControls();
      // Allows for velocity to be seen on the Driver Station
        telemetry.addData("Flywheel Velocity", ((DcMotorEx) flywheel).getVelocity());
        telemetry.update();
      }
    }
  }

//Below are the individual functions, also called methods, for each system.

  /**
   * Intake Controls
   * This function contains the controls for the intake system using the Core Hex.
   */
  private void intakeControls() {
    // Intake in and out
    if (gamepad1.right_bumper) {
      intakeMotor.setPower(1);
    } else if (gamepad1.right_trigger_pressed) {
      intakeMotor.setPower(-1);
    } else {
      intakeMotor.setPower(0);
    }
  }
  
  /**
   * Controls for the Pollen poking servo
   * This function contains the code for the servo powered pollen poker on the front of the robot.
   */
  private void pollenPoker() {
    // Servo control for poking out pollen
    if (gamepad1.crossWasPressed()) {
      pollenServo.setPosition(0);
    } else if (gamepad1.circleWasPressed()) {
      pollenServo.setPosition(1);
    }
  }

  /**
   * Controls for Arcade Drive
   * This function contains the code for the arcade drive controls.
   */
  private void arcadeDrive() {
    float x;
    float y;

    // Arcade Drive on left stick
    x = gamepad1.left_stick_x;
    y = -gamepad1.left_stick_y;
    leftDrive.setPower(y + x);
    rightDrive.setPower(y - x);
  }
  /**
   * Controls for the flywheel - manual and auto sequence
   * This function contains the controls for the flywheel system.
   */
  private void flywheelControls() {
    if (gamepad1.triangle) {
      // Manual control for flywheel motor
      ((DcMotorEx) flywheel).setVelocity(targetVelocity);
    } else if (gamepad1.square) {
      // Reverse flywheel
      flywheel.setPower(-0.55);
    } else if (gamepad1.left_bumper) {
      ((DcMotorEx) flywheel).setVelocity(targetVelocity);
      // Auto feed into flywheel if within a set velocity range
      if (((DcMotorEx) flywheel).getVelocity() >= targetVelocity - 20 && ((DcMotorEx) flywheel).getVelocity() <= targetVelocity + 20) {
        crServo.setPower(1);
      } else {
        // Stop feeding while the flywheel is off speed (e.g. it slowed after a launch);
        // without this the feeder stays on and pushes pollen into a slow flywheel
        crServo.setPower(0);
      }
    } else if (gamepad1.dpad_up) {
      // Manual control for feeder servo
      crServo.setPower(1);
    } else if (gamepad1.dpad_down) {
      // Reverse feeder
      crServo.setPower(-1);
    } else {
      // Turn off servo and motor when gamepad is not active
      flywheel.setPower(0);
      crServo.setPower(0);
    }
  }
}
