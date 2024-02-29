// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.function.DoubleSupplier;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.subsystems.IndexSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.VisionSubsystem;
import frc.robot.subsystems.LEDSubsystem.TempState;

public class ShootCommand extends Command {
  private final ShooterSubsystem shooter;
  private final IndexSubsystem index;
  private boolean feeding = false;
  DoubleSupplier topSupplier = null;
  DoubleSupplier bottomSupplier = null;
  private final VisionSubsystem vision = VisionSubsystem.getInstance();
  private boolean cancelled = false;

  public ShootCommand(ShooterSubsystem shooter, IndexSubsystem index) {
    addRequirements(shooter, index);
    this.shooter = shooter;
    this.index = index;
  }

  public ShootCommand(ShooterSubsystem shooter, IndexSubsystem index, DoubleSupplier topSupplier, DoubleSupplier bottomSupplier) {
    this(shooter, index);
    this.topSupplier = topSupplier;
    this.bottomSupplier = bottomSupplier;
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    LEDSubsystem.setTempState(TempState.SHOOTING);
    cancelled = false;
    feeding = false;

    if (topSupplier != null && bottomSupplier != null) {
      shooter.shoot(topSupplier.getAsDouble(), bottomSupplier.getAsDouble());
    } else {
      if (!DriverStation.isAutonomous() && shooter.usingVision()) {
        cancelled = !vision.haveTarget();
      }
      if (cancelled) {
        // TODO Flash LEDs to indicate failure
        cancel();
      } else {
        shooter.shoot();
      }
    }
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    if (cancelled) {
      return;
    }
    if (!feeding && shooter.isReady() && (!shooter.usingVision() || (Math.abs(vision.angleError().getDegrees()) < Constants.Vision.maxAngleError))) {
      index.feed();
      feeding = true;
    }
    if (topSupplier == null || bottomSupplier == null) {
      shooter.shoot();
    }
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    // Stop feeding
    index.stop();

    // Restore idle speed
    shooter.idle();

    // Restore default shot
    shooter.setNextShot(null);

    LEDSubsystem.clearTempState();
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    // TODO Consider ending command when shooter is done (especially for Auto)
    return cancelled;
  }
}