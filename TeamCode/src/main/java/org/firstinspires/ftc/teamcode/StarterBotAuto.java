package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name = "Starter Bot Auto", group = "Starter")
public class StarterBotAuto extends LinearOpMode {

  private DcMotor rightDrive;
  private DcMotor flywheel;
  private Servo pollenServo;
  private DcMotor leftDrive;
  private CRServo crServo;
  private DcMotor intakeMotor;

  /**
   * This autonomous code will have the robot drive away from the wall then 
   * launch preloaded pollen. An optional delay is available if your team chooses 
   * to start on the far side!
   */
  @Override
  public void runOpMode() {
    int targetVelocity;
    ElapsedTime autoTimer;

    rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");
    flywheel = hardwareMap.get(DcMotor.class, "flywheel");
    pollenServo = hardwareMap.get(Servo.class, "pollenServo");
    leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");
    crServo = hardwareMap.get(CRServo.class, "crServo");
    intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");

    // Put initialization blocks here.
    rightDrive.setDirection(DcMotor.Direction.REVERSE);
    flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    targetVelocity = 1160;
    autoTimer = new ElapsedTime();
    waitForStart();
    if (opModeIsActive()) {
      // Ensures the servo for the pollen poker is in the right position pointing up
      pollenServo.setPosition(1);
      
      // Enable the sleep call below (Line 51) to add a delay to the program starting
      // The default delay is 10 seconds so be sure to adjust based on your strategy!
      //sleep(10000);
      
      // Robot drives forward 700 milliseconds to no longer touch the wall
      autoTimer.reset();
      while (autoTimer.milliseconds() <= 700 && opModeIsActive()) {
        leftDrive.setPower(-0.25);
        rightDrive.setPower(-0.25);
      }
      
      // Turn off drivetrain motors to prevent extra movement
      leftDrive.setPower(0);
      rightDrive.setPower(0);
      
      // Flywheel begins launch sequence for the preloaded pollen
      autoTimer.reset();
      
      // The flywheel is set to spin for 15 seconds.
      while (autoTimer.seconds() <= 15 && opModeIsActive()) {
        ((DcMotorEx) flywheel).setVelocity(targetVelocity);
       
        // The flywheel must be within a specified range of the target velocity for the intake and feeder servo to activate!
        if (((DcMotorEx) flywheel).getVelocity() >= targetVelocity - 50 && ((DcMotorEx) flywheel).getVelocity() <= targetVelocity + 50) {
          crServo.setPower(1);
          intakeMotor.setPower(1);
        } else {
        
          // If the velocity is not within range, the intake and feeder servo turn off to prevent early feeding
          crServo.setPower(0);
          intakeMotor.setPower(0);
        }
      }
    }
  }
}
