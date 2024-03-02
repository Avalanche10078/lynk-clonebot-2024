// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.IndexSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.ShooterSubsystem;
import frc.robot.subsystems.LEDSubsystem.TempState;

public class ShooterIntakeCommand extends Command {
  private final ShooterSubsystem shooter;
  private final IndexSubsystem index;
  private final Joystick controller;
  private boolean pulling = true;
  private boolean seenIt = false;

  public ShooterIntakeCommand(ShooterSubsystem shooter, IndexSubsystem index, Joystick controller) {
    addRequirements(shooter, index);
    this.shooter = shooter;
    this.index = index;
    this.controller = controller;
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
    pulling = true;
    seenIt = false;
    shooter.intake();
    index.eject();
    LEDSubsystem.setTempState(TempState.INTAKING);
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    if (pulling) {
      if (!seenIt) {
        if (!index.getIndexSensor().getAsBoolean()) {
          return;
        } else {
          seenIt = true;
        }
      } else {
        // Seen it
        if (index.getIndexSensor().getAsBoolean()) {
          return;
        } else {
          pulling = false;
          index.feed();
        }
      }
    }
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    shooter.stop();
    index.stop();

    LEDSubsystem.clearTempState();

    if (!interrupted) {
      CommandScheduler.getInstance().schedule(
        Commands.startEnd(
          () -> { controller.setRumble(RumbleType.kLeftRumble, 1.0); controller.setRumble(RumbleType.kRightRumble, 1.0); },
          () -> { controller.setRumble(RumbleType.kLeftRumble, 0.0); controller.setRumble(RumbleType.kRightRumble, 0.0); })
        .raceWith(Commands.waitSeconds(0.5)));
    }
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return (!pulling && index.getIndexSensor().getAsBoolean());
  }
}