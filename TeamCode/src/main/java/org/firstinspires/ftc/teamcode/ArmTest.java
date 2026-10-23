package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Arm Test: finds how much current the arm servo draws when it hits the end of its travel,
 * and saves a stall threshold that Controller Drive and Phone Drive use to stop the arm
 * before it grinds against the barrier.
 *
 * What it does after ▶ (about 20 seconds, hands clear of the arm):
 *   1. measures the current with the servo off
 *   2. runs the arm slowly one way until it stalls at the end (current jumps and stays up)
 *   3. does the same the other way
 *   4. saves a threshold halfway between "moving freely" and "stalled"
 *
 * Press ■ at any time to stop. Run it again whenever the arm is changed.
 */
@TeleOp(name = "Arm Test", group = "Laj")
public class ArmTest extends LinearOpMode {

  private static final double TEST_POWER = 0.3;        // slow, so hitting the end is gentle
  private static final double SPIN_UP_SECONDS = 0.4;   // ignore the start-up spike
  private static final double MAX_SECONDS_PER_SIDE = 8;
  private static final double STALL_FACTOR = 1.8;      // stalled = this much above moving freely
  private static final double STALL_MARGIN_MA = 250;   // ...and at least this many mA above

  private ArmServo arm;

  @Override
  public void runOpMode() {
    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
    arm = new ArmServo(hardwareMap);
    if (!arm.isAvailable()) {
      telemetry.addLine("Servo port not available.");
      telemetry.update();
      waitForStart();
      return;
    }

    telemetry.addLine("Arm Test: press ▶. The arm moves slowly to each end by itself.");
    telemetry.addLine("Keep hands clear. Press ■ to stop at any time.");
    telemetry.update();
    waitForStart();

    try {
      double idle = average(0.5);
      Side plus = runToEnd(+1, idle);
      Side minus = runToEnd(-1, idle);
      arm.off();
      if (!opModeIsActive()) {
        return;
      }

      double free = Math.max(plus.freeMilliamps, minus.freeMilliamps);
      double stall = Math.min(plus.stalled ? plus.stallMilliamps : Double.MAX_VALUE,
          minus.stalled ? minus.stallMilliamps : Double.MAX_VALUE);
      boolean found = stall != Double.MAX_VALUE;
      double threshold = found ? (free + stall) / 2 : -1;
      if (found) {
        ArmServo.saveThreshold(threshold);
      }

      while (opModeIsActive()) {
        telemetry.addData("Servo off", "%.0f mA", idle);
        telemetry.addData("+ direction", plus.describe());
        telemetry.addData("- direction", minus.describe());
        if (found) {
          telemetry.addData("Saved stall limit", "%.0f mA (used by Controller Drive and Phone Drive)", threshold);
        } else {
          telemetry.addLine("No end found either way: the arm may turn freely. Nothing saved.");
        }
        telemetry.addLine("Done. Press ■.");
        telemetry.update();
        sleep(200);
      }
    } finally {
      arm.off();
    }
  }

  private static class Side {
    double freeMilliamps;
    double stallMilliamps;
    double seconds;
    boolean stalled;

    String describe() {
      return stalled
          ? String.format("moving %.0f mA, end reached after %.1f s, stalled %.0f mA", freeMilliamps, seconds, stallMilliamps)
          : String.format("moving %.0f mA, no end found in %.0f s", freeMilliamps, MAX_SECONDS_PER_SIDE);
    }
  }

  // Run one way at TEST_POWER until the current jumps and stays up (the end of travel)
  private Side runToEnd(int direction, double idle) {
    Side side = new Side();
    if (!opModeIsActive()) {
      return side;
    }
    ElapsedTime timer = new ElapsedTime();
    ElapsedTime overFor = new ElapsedTime();
    boolean over = false;
    double freeSum = 0;
    int freeCount = 0;

    while (opModeIsActive() && timer.seconds() < MAX_SECONDS_PER_SIDE) {
      arm.runRaw(direction * TEST_POWER);
      double mA = arm.readMilliamps();
      double t = timer.seconds();

      if (t > SPIN_UP_SECONDS && t < SPIN_UP_SECONDS + 0.5) {
        freeSum += mA;
        freeCount++;
      } else if (t >= SPIN_UP_SECONDS + 0.5) {
        double free = freeSum / Math.max(1, freeCount);
        side.freeMilliamps = free;
        boolean high = mA > Math.max(free * STALL_FACTOR, free + STALL_MARGIN_MA);
        if (high && !over) {
          over = true;
          overFor.reset();
        } else if (!high) {
          over = false;
        }
        if (over && overFor.seconds() > ArmServo.STALL_SECONDS) {
          side.stalled = true;
          side.stallMilliamps = average(0.2);
          side.seconds = t;
          break;
        }
      }

      telemetry.addData("Testing", direction > 0 ? "+ direction" : "- direction");
      telemetry.addData("Current", "%.0f mA (servo off: %.0f mA)", mA, idle);
      telemetry.addData("Time", "%.1f s", t);
      telemetry.update();
    }
    arm.runRaw(0);
    sleep(400);
    return side;
  }

  // Average servo current over a short time
  private double average(double seconds) {
    ElapsedTime timer = new ElapsedTime();
    double sum = 0;
    int count = 0;
    while (opModeIsActive() && timer.seconds() < seconds) {
      double mA = arm.readMilliamps();
      if (mA >= 0) {
        sum += mA;
        count++;
      }
      sleep(30);
    }
    return count == 0 ? 0 : sum / count;
  }
}
