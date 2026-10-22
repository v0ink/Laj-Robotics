package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

import java.util.List;

/**
 * Test: spin the motor(s) from ANY gamepad input, on gamepad 1 or 2.
 *
 *   Thumbsticks -> variable speed: the further you push, the faster.
 *                  Up/right = forward, down/left = backward.
 *   Triggers    -> variable speed forward: the further you pull, the faster.
 *   Any button  -> half speed forward (A, B, X, Y, bumpers, D-pad, stick clicks, ...).
 *   Nothing     -> motor stops.
 *
 * Spins EVERY motor in the active robot configuration, so it works no matter which
 * port the test motor is plugged into. If the robot is built, lift the wheels off the
 * ground first. The screen shows the raw gamepad values so we can tell whether the
 * gamepad, the motor or power is the problem.
 */
@TeleOp(name = "Any Input Motor", group = "Test")
public class AnyInputMotor extends LinearOpMode {

  private static final double BUTTON_POWER = 0.5;

  @Override
  public void runOpMode() {
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    List<DcMotor> motors = hardwareMap.getAll(DcMotor.class);
    StringBuilder names = new StringBuilder();
    for (DcMotor motor : motors) {
      motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
      motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
      motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
      names.append(hardwareMap.getNamesOf(motor)).append(' ');
    }

    if (motors.isEmpty()) {
      telemetry.addLine("No motors in the active robot configuration.");
      telemetry.addLine("Add a motor to the configuration, then try again.");
    } else {
      telemetry.addLine("Ready. Press ▶, then press Start + A on the gamepad.");
    }
    addHardwareReport();
    telemetry.update();

    waitForStart();

    while (opModeIsActive()) {
      String source = "nothing";
      double power = 0;

      // Pick the input pushed the furthest, from either gamepad
      for (Gamepad pad : new Gamepad[] {gamepad1, gamepad2}) {
        double[] analog = {
            -pad.left_stick_y, pad.left_stick_x,   // stick up gives negative y, so flip it
            -pad.right_stick_y, pad.right_stick_x,
            pad.left_trigger, pad.right_trigger
        };
        String[] analogNames = {"left stick", "left stick", "right stick", "right stick",
            "left trigger", "right trigger"};
        for (int i = 0; i < analog.length; i++) {
          if (Math.abs(analog[i]) > Math.abs(power)) {
            power = analog[i];
            source = analogNames[i];
          }
        }
        if (anyButton(pad) && Math.abs(power) < BUTTON_POWER) {
          power = BUTTON_POWER;
          source = "button";
        }
      }

      for (DcMotor motor : motors) {
        motor.setPower(power);
      }

      telemetry.addData("Input", source);
      telemetry.addData("Power", "%.2f", power);
      telemetry.addData("Battery", "%.1f V", batteryVoltage());
      telemetry.addData("Raw gamepad1", "LS %.2f,%.2f RS %.2f,%.2f LT %.2f RT %.2f",
          gamepad1.left_stick_x, gamepad1.left_stick_y,
          gamepad1.right_stick_x, gamepad1.right_stick_y,
          gamepad1.left_trigger, gamepad1.right_trigger);
      for (DcMotor motor : motors) {
        // Current > ~0.1 A means the hub is really driving the motor
        telemetry.addData("Motor " + hardwareMap.getNamesOf(motor), "port %d  power %.2f  %.2f A  enc %d",
            motor.getPortNumber(), motor.getPower(),
            ((DcMotorEx) motor).getCurrent(CurrentUnit.AMPS), motor.getCurrentPosition());
      }
      addHubReport();
      telemetry.update();
    }

    for (DcMotor motor : motors) {
      motor.setPower(0);
    }
  }

  // Which hub and port every configured motor is on, plus each hub's power
  private void addHardwareReport() {
    for (DcMotor motor : hardwareMap.getAll(DcMotor.class)) {
      telemetry.addData("Motor " + hardwareMap.getNamesOf(motor), "port %d on %s",
          motor.getPortNumber(), motor.getConnectionInfo());
    }
    addHubReport();
  }

  // Input voltage below ~11 V (or ~0 V) means no battery power for the motors
  private void addHubReport() {
    for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
      telemetry.addData("Hub " + hub.getModuleAddress() + (hub.isParent() ? " (Control Hub)" : " (Expansion Hub)"),
          "%.1f V in, %.2f A total",
          hub.getInputVoltage(VoltageUnit.VOLTS), hub.getCurrent(CurrentUnit.AMPS));
    }
  }

  private boolean anyButton(Gamepad pad) {
    return pad.a || pad.b || pad.x || pad.y
        || pad.dpad_up || pad.dpad_down || pad.dpad_left || pad.dpad_right
        || pad.left_bumper || pad.right_bumper
        || pad.left_stick_button || pad.right_stick_button
        || pad.start || pad.back || pad.guide || pad.touchpad;
  }

  // Lowest reading across the hub's voltage sensors; 0 if none
  private double batteryVoltage() {
    double lowest = Double.POSITIVE_INFINITY;
    for (VoltageSensor sensor : hardwareMap.voltageSensor) {
      double v = sensor.getVoltage();
      if (v > 0) {
        lowest = Math.min(lowest, v);
      }
    }
    return lowest == Double.POSITIVE_INFINITY ? 0 : lowest;
  }
}
