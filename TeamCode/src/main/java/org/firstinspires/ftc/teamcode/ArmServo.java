package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxServoController;
import com.qualcomm.hardware.lynx.commands.core.LynxGetADCCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxGetADCResponse;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.ServoController;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import com.qualcomm.robotcore.util.ReadWriteFile;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.util.List;

/**
 * The arm's continuous-rotation servo (SERVO port 0), shared by Controller Drive,
 * Phone Drive and Arm Test. Not an OpMode itself.
 *
 * - setPower(-1..1) sets the speed; 0 stops. The signal is switched off after 1 s idle, so
 *   the servo can never creep or run off by itself.
 * - Stall guard: when the arm hits the end of its travel the servo current jumps. If it stays
 *   above the calibrated threshold, the arm stops pushing that way and only moves the other
 *   way until you reverse. Arm Test measures the threshold and saves it to the Control Hub.
 */
public class ArmServo {

  public static final int PORT = 0;

  // Where Arm Test saves the stall threshold (survives restarts and new code installs)
  static final File SETTINGS_FILE = new File(AppUtil.FIRST_FOLDER, "laj_arm_stall_milliamps.txt");
  // Used until Arm Test has been run once: a typical servo stall is well above this
  static final double DEFAULT_STALL_MILLIAMPS = 1200;
  // Current must stay above the threshold this long before it counts as a stall
  // (starting the servo causes a short spike that isn't a stall)
  static final double STALL_SECONDS = 0.2;
  private static final double IDLE_OFF_SECONDS = 1.0;
  private static final double CURRENT_READ_INTERVAL = 0.03;

  private final ServoControllerEx servos;
  private final LynxModule hub;
  private final double stallMilliamps;
  private final boolean calibrated;

  private boolean signalOn;
  private long lastActiveNanos;
  private double power;
  private int blockedDirection;     // +1 / -1 = stalled that way, 0 = free
  private long overSinceNanos;      // when the current first went over the threshold, 0 = not over
  private long lastReadNanos;
  private double milliamps;

  public ArmServo(HardwareMap hardwareMap) {
    hub = controlHub(hardwareMap);
    ServoControllerEx found = null;
    List<ServoController> configured = hardwareMap.getAll(ServoController.class);
    if (!configured.isEmpty() && configured.get(0) instanceof ServoControllerEx) {
      found = (ServoControllerEx) configured.get(0);
    } else if (hub != null) {
      try {
        found = new LynxServoController(hardwareMap.appContext, hub);
      } catch (Exception e) {
        found = null;
      }
    }
    servos = found;

    double saved = readSavedThreshold();
    calibrated = saved > 0;
    stallMilliamps = calibrated ? saved : DEFAULT_STALL_MILLIAMPS;
    off();
  }

  public boolean isAvailable() {
    return servos != null;
  }

  /** Speed -1..1 (0 = stop). Call every loop. */
  public void setPower(double requested) {
    if (servos == null) {
      return;
    }
    int direction = (int) Math.signum(requested);
    if (direction != 0 && direction == -blockedDirection) {
      blockedDirection = 0; // moving away from the barrier: free again
    }
    if (direction != 0 && direction == blockedDirection) {
      requested = 0; // still pushing into the barrier: refuse
    }
    power = requested;

    if (power != 0) {
      lastActiveNanos = System.nanoTime();
      if (!signalOn) {
        servos.setServoPwmEnable(PORT);
        signalOn = true;
      }
      servos.setServoPosition(PORT, 0.5 + 0.5 * power);
      checkStall(direction);
    } else {
      overSinceNanos = 0;
      if (signalOn) {
        if ((System.nanoTime() - lastActiveNanos) / 1e9 > IDLE_OFF_SECONDS) {
          off();
        } else {
          servos.setServoPosition(PORT, 0.5); // stop, signal still on
        }
      }
    }
  }

  /** Arm Test only: run without the stall guard. */
  void runRaw(double rawPower) {
    if (servos == null) {
      return;
    }
    if (!signalOn) {
      servos.setServoPwmEnable(PORT);
      signalOn = true;
    }
    power = rawPower;
    servos.setServoPosition(PORT, 0.5 + 0.5 * rawPower);
  }

  public void off() {
    if (servos != null) {
      servos.setServoPwmDisable(PORT);
    }
    signalOn = false;
    power = 0;
    overSinceNanos = 0;
  }

  private void checkStall(int direction) {
    double now = readMilliamps();
    if (now < 0) {
      return; // couldn't read the sensor: no guard this loop
    }
    if (now > stallMilliamps) {
      if (overSinceNanos == 0) {
        overSinceNanos = System.nanoTime();
      } else if ((System.nanoTime() - overSinceNanos) / 1e9 > STALL_SECONDS) {
        blockedDirection = direction;
        power = 0;
        servos.setServoPosition(PORT, 0.5);
        overSinceNanos = 0;
      }
    } else {
      overSinceNanos = 0;
    }
  }

  /** Total current of the servo ports in mA (only our arm servo is plugged in). -1 if unreadable. */
  public double readMilliamps() {
    if (hub == null) {
      return -1;
    }
    long now = System.nanoTime();
    if ((now - lastReadNanos) / 1e9 < CURRENT_READ_INTERVAL) {
      return milliamps; // don't flood the hub with requests
    }
    lastReadNanos = now;
    try {
      LynxGetADCCommand command = new LynxGetADCCommand(hub,
          LynxGetADCCommand.Channel.SERVO_CURRENT, LynxGetADCCommand.Mode.ENGINEERING);
      LynxGetADCResponse response = command.sendReceive();
      milliamps = response.getValue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return -1;
    } catch (Exception e) {
      return -1;
    }
    return milliamps;
  }

  /** One line for telemetry */
  public String status() {
    String guard = blockedDirection == 0 ? "free"
        : "stopped at the " + (blockedDirection > 0 ? "+" : "-") + " end, reverse to move";
    return String.format("power %.2f  %.0f mA  limit %.0f mA%s  %s", power, milliamps, stallMilliamps,
        calibrated ? "" : " (default, run Arm Test)", guard);
  }

  static void saveThreshold(double milliamps) {
    ReadWriteFile.writeFile(SETTINGS_FILE, String.valueOf(Math.round(milliamps)));
  }

  private static double readSavedThreshold() {
    try {
      if (SETTINGS_FILE.exists()) {
        return Double.parseDouble(ReadWriteFile.readFile(SETTINGS_FILE).trim());
      }
    } catch (Exception e) {
      // unreadable file: fall back to the default
    }
    return -1;
  }

  private static LynxModule controlHub(HardwareMap hardwareMap) {
    for (LynxModule hub : hardwareMap.getAll(LynxModule.class)) {
      if (hub.isParent()) {
        return hub;
      }
    }
    return null;
  }
}
