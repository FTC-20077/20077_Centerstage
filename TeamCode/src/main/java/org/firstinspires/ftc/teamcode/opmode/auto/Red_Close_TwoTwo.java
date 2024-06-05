package org.firstinspires.ftc.teamcode.opmode.auto;
import com.acmerobotics.roadrunner.Action;
import com.acmerobotics.roadrunner.ParallelAction;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.SequentialAction;
import com.acmerobotics.roadrunner.SleepAction;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.acmerobotics.roadrunner.Vector2d;
import com.acmerobotics.roadrunner.ftc.Actions;
import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.internal.system.Deadline;
import org.firstinspires.ftc.teamcode.config.roadrunner.MecanumDrive;
import org.firstinspires.ftc.teamcode.config.subsystem.*;

import java.util.concurrent.TimeUnit;

@Autonomous(name="Red Close 2+2", group = "Red")
public class Red_Close_TwoTwo extends LinearOpMode {

    private HuskyLens huskyLens;

    @Override
    public void runOpMode() {
        ClawSubsystem claw = new ClawSubsystem(hardwareMap);
        LiftSubsystem lift = new LiftSubsystem(hardwareMap);
        GearRotationSubsystem gear = new GearRotationSubsystem(hardwareMap);
        Pose2d beginPose = new Pose2d(62, 12, Math.toRadians(180));
        MecanumDrive drive = new MecanumDrive(hardwareMap, beginPose);
        PresetSubsystem presets = new PresetSubsystem(claw, lift, gear);
        CameraSubsystem camera = new CameraSubsystem(hardwareMap);
        huskyLens = hardwareMap.get(HuskyLens.class, "huskyLens");

        camera.SPEED_GAIN = -0.02;   // Drive = Error * Gain
        camera.STRAFE_GAIN = -0.01;
        camera.TURN_GAIN = 0;

        // Actions that run when Init
        claw.closeClaws();
        gear.wheelServo_Deactivated();
        camera.initAprilTag();
        camera.setManualExposure(2, 50);

        //---------------------------------------- Red Close 2+2 ----------------------------------------------\\
        Pose2d RedCloseTwoTwo_startPose = new Pose2d(62, 12, Math.toRadians(180));
        Pose2d RedCloseTwoTwo_yellowScoringPose3 = new Pose2d(36, 34, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_yellowScoringPose2 = new Pose2d(26, 26, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_yellowScoringPose1 = new Pose2d(36, 10, Math.toRadians(270));
        Pose2d RedCloseTwoZero_yellowScoringOverridePose3 = new Pose2d(42, 45, Math.toRadians(270));
        Pose2d RedCloseTwoZero_yellowScoringOverridePose2 = new Pose2d(36, 45, Math.toRadians(270));
        Pose2d RedCloseTwoZero_yellowScoringOverridePose1 = new Pose2d(30, 45, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_driveToWhitePose3 = new Pose2d(42, 51.5, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_driveToWhitePose2 = new Pose2d(36, 51.5, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_driveToWhitePose1 = new Pose2d(30, 51.5, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_whiteTrussPose = new Pose2d(35.5,-36.5, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_whiteScoringPose = new Pose2d(60, 36, Math.toRadians(270));
        Pose2d RedCloseTwoTwo_parkingPose = new Pose2d(40.5, 52.75, Math.toRadians(90));

        //This action drives to the first tape line
        TrajectoryActionBuilder RedCloseTwoTwo_purpleTAction3 = drive.actionBuilder(RedCloseTwoTwo_startPose)
                .lineToX(55)
                .splineTo(new Vector2d(36, 34), Math.toRadians(270));
        Action RedCloseTwoTwo_purpleAction3 = RedCloseTwoTwo_purpleTAction3.build();

        //This action drives to the second tape line
        TrajectoryActionBuilder RedCloseTwoTwo_purpleTAction2 = drive.actionBuilder(RedCloseTwoTwo_startPose)
                .lineToX(55)
                .splineTo(new Vector2d(26, 26), Math.toRadians(270));
        Action RedCloseTwoTwo_purpleAction2 = RedCloseTwoTwo_purpleTAction2.build();

        //This action drives to the third tape line
        TrajectoryActionBuilder RedCloseTwoTwo_purpleTAction1 = drive.actionBuilder(RedCloseTwoTwo_startPose)
                .lineToX(55)
                .splineTo(new Vector2d(36, 10), Math.toRadians(270));
        Action RedCloseTwoTwo_purpleAction1 = RedCloseTwoTwo_purpleTAction1.build();

        //This action drives to the first backdrop section
        TrajectoryActionBuilder RedCloseTwoTwo_yellowScoringTAction3 = drive.actionBuilder(RedCloseTwoTwo_yellowScoringPose3)
                .strafeTo(new Vector2d(42, 45));
        Action RedCloseTwoTwo_yellowScoringAction3 = RedCloseTwoTwo_yellowScoringTAction3.build();

        //This action drives to the second backdrop section
        TrajectoryActionBuilder RedCloseTwoTwo_yellowScoringTAction2 = drive.actionBuilder(RedCloseTwoTwo_yellowScoringPose2)
                .strafeTo(new Vector2d(36, 45));
        Action RedCloseTwoTwo_yellowScoringAction2 = RedCloseTwoTwo_yellowScoringTAction2.build();

        //This action drives to the third backdrop section
        TrajectoryActionBuilder RedCloseTwoTwo_yellowScoringTAction1 = drive.actionBuilder(RedCloseTwoTwo_yellowScoringPose1)
                .strafeTo(new Vector2d(36,15))
                .strafeTo(new Vector2d(30, 45));
        Action RedCloseTwoTwo_yellowScoringAction1 = RedCloseTwoTwo_yellowScoringTAction1.build();

        //This action OVERRIDES camera and drives to the first backdrop section
        TrajectoryActionBuilder RedCloseTwoZero_yellowScoringOverrideTAction3 = drive.actionBuilder(RedCloseTwoZero_yellowScoringOverridePose3)
                .strafeTo(new Vector2d(42, 51.5));
        Action RedCloseTwoZero_yellowScoringOverrideAction3 = RedCloseTwoZero_yellowScoringOverrideTAction3.build();

        //This action OVERRIDES camera and drives to the second backdrop section
        TrajectoryActionBuilder RedCloseTwoZero_yellowScoringOverrideTAction2 = drive.actionBuilder(RedCloseTwoZero_yellowScoringOverridePose2)
                .strafeTo(new Vector2d(36, 51.5));
        Action RedCloseTwoZero_yellowScoringOverrideAction2 = RedCloseTwoZero_yellowScoringOverrideTAction2.build();

        //This action OVERRIDES camera and drives to the third backdrop section
        TrajectoryActionBuilder RedCloseTwoZero_yellowScoringOverrideTAction1 = drive.actionBuilder(RedCloseTwoZero_yellowScoringOverridePose1)
                .strafeTo(new Vector2d(30, 51.5));
        Action RedCloseTwoZero_yellowScoringOverrideAction1 = RedCloseTwoZero_yellowScoringOverrideTAction1.build();

        //This action drives to robot to the white pixel stack
        TrajectoryActionBuilder RedCloseTwoTwo_driveToWhiteTAction3 = drive.actionBuilder(RedCloseTwoTwo_driveToWhitePose3)
                .strafeTo(new Vector2d(42,47))
                .strafeToConstantHeading(new Vector2d(60, 36))
                .strafeTo(new Vector2d(60,-36))
                .strafeToConstantHeading(new Vector2d(35.5,-36.5));
        Action RedCloseTwoTwo_driveToWhiteAction3 = RedCloseTwoTwo_driveToWhiteTAction3.build();

        //This action drives to robot to the white pixel stack
        TrajectoryActionBuilder RedCloseTwoTwo_driveToWhiteTAction2 = drive.actionBuilder(RedCloseTwoTwo_driveToWhitePose2)
                .strafeTo(new Vector2d(36,47))
                .strafeToConstantHeading(new Vector2d(60, 36))
                .strafeTo(new Vector2d(60,-36))
                .strafeToConstantHeading(new Vector2d(35.5,-36.5));
                /*.strafeTo(new Vector2d(-36,47))
                .strafeToLinearHeading(new Vector2d(-63, 36), Math.toRadians(267))//267
                .strafeTo(new Vector2d(-63,-36))
                .strafeToLinearHeading((new Vector2d(-35,-36.5)), Math.toRadians(270));*/
        Action RedCloseTwoTwo_driveToWhiteAction2 = RedCloseTwoTwo_driveToWhiteTAction2.build();

        //This action drives to robot to the white pixel stack
        TrajectoryActionBuilder RedCloseTwoTwo_driveToWhiteTAction1 = drive.actionBuilder(RedCloseTwoTwo_driveToWhitePose1)
                .strafeTo(new Vector2d(42,47))
                .strafeToConstantHeading(new Vector2d(60, 36))
                .strafeTo(new Vector2d(60,-36))
                .strafeToConstantHeading(new Vector2d(35.5,-36.5));
                /*.strafeTo(new Vector2d(-30,47))
                .strafeToLinearHeading(new Vector2d(-63, 36), Math.toRadians(267))
                .strafeTo(new Vector2d(-63,-36))
                .strafeToLinearHeading(new Vector2d(-35,-36.5), Math.toRadians(270));*/
        Action RedCloseTwoTwo_driveToWhiteAction1 = RedCloseTwoTwo_driveToWhiteTAction1.build();

        //This action drives to robot to the white pixel stack
        TrajectoryActionBuilder RedCloseTwoTwo_whiteTrussTAction = drive.actionBuilder(RedCloseTwoTwo_whiteTrussPose)
                .strafeToConstantHeading(new Vector2d(60,-36))
                .strafeToConstantHeading(new Vector2d(60,-12))
                .strafeToConstantHeading(new Vector2d(60, 36));
        Action RedCloseTwoTwo_whiteTrussAction = RedCloseTwoTwo_whiteTrussTAction.build();

        TrajectoryActionBuilder RedCloseTwoTwo_whiteScoringTAction = drive.actionBuilder(RedCloseTwoTwo_whiteScoringPose)
                .splineTo(new Vector2d(40.5, 52.75), Math.toRadians(90));
        Action RedCloseTwoTwo_whiteScoringAction = RedCloseTwoTwo_whiteScoringTAction.build();

        //This action drives to robot to the parking zone
        TrajectoryActionBuilder RedCloseTwoTwo_parkingTAction = drive.actionBuilder(RedCloseTwoTwo_parkingPose)
                .strafeTo(new Vector2d(38,40))
                .strafeTo((new Vector2d(67, 50)));
        Action RedCloseTwoTwo_parkingAction = RedCloseTwoTwo_parkingTAction.build();


        //Huskylens Setup
        Deadline rateLimit = new Deadline(1, TimeUnit.SECONDS);
        rateLimit.expire();

        if (!huskyLens.knock()) {
            telemetry.addData(">>", "Problem communicating with " + huskyLens.getDeviceName());
        } else {
            telemetry.addData(">>", "Press start to continue");
        }
        huskyLens.selectAlgorithm(HuskyLens.Algorithm.COLOR_RECOGNITION);


        // Wait for driver to press start
        telemetry.addData("Camera preview on/off", "3 dots, Camera Stream");
        telemetry.addData(">", "Touch Play to start OpMode");
        telemetry.update();
        waitForStart();


        while (opModeIsActive()) {

            if (!rateLimit.hasExpired()) {
                continue;
            }
            rateLimit.reset();
            HuskyLens.Block[] blocks = huskyLens.blocks();
            telemetry.addData("Block count", blocks.length);
            for (int i = 0; i < blocks.length; i++) {
                telemetry.addData("Block", blocks[i].toString());// this gives you the data
                telemetry.addData("location?", blocks[i].x);// this gives you just x

                //----------------------------1----------------------------\\
                if (blocks[i].x < 100 && blocks[i].id == 1 && blocks[i].y < 200) {

                    Actions.runBlocking(
                            new SequentialAction(
                                    new ParallelAction(
                                            presets.StartPos(),
                                            RedCloseTwoTwo_purpleAction1
                                    ),
                                    new SleepAction(0.2),
                                    claw.openLClaw(),
                                    new SleepAction(0.35),
                                    new ParallelAction(
                                            new ParallelAction(
                                                    presets.ScoringPos(),
                                                    RedCloseTwoTwo_yellowScoringAction1
                                            ),
                                            claw.closeLClaw()
                                    )
                            )
                    );

                    camera.alignToTag(4);

                    if(!camera.targetFound){
                        Actions.runBlocking(
                                RedCloseTwoZero_yellowScoringOverrideAction1
                        );
                    }

                    Actions.runBlocking(
                            new SequentialAction(
                                    new SleepAction(.1),
                                    claw.openRClaw(),
                                    new SleepAction(.25),
                                    new ParallelAction(
                                            new SequentialAction(
                                                    presets.GroundPos(),
                                                    claw.openClaws(),
                                                    claw.whiteGroundClaw()
                                            ),
                                            RedCloseTwoTwo_driveToWhiteAction1
                                    ),
                                    new SequentialAction(
                                            presets.WhiteStack(),
                                            new SleepAction(0.25),
                                            RedCloseTwoTwo_whiteTrussAction,
                                            new ParallelAction(
                                                    RedCloseTwoTwo_whiteScoringAction,
                                                    presets.WhiteScoringPos()
                                            )
                                    ),

                                    new SequentialAction(
                                            new SleepAction(.1),
                                            claw.openClaws(),
                                            new SleepAction(.25),
                                            RedCloseTwoTwo_parkingAction,
                                            presets.WhiteGroundPos()
                                    )
                            )
                    );
                    sleep(400000);
                }


                //----------------------------2----------------------------\\
                if (blocks[i].x > 100 && blocks[i].x < 200 && blocks[i].id == 1 && blocks[i].y < 200) {
                    Actions.runBlocking(
                            new SequentialAction(
                                    new ParallelAction(
                                            presets.StartPos(),
                                            RedCloseTwoTwo_purpleAction2
                                    ),
                                    new SleepAction(0.1),
                                    claw.openLClaw(),
                                    new SleepAction(0.25),
                                    new ParallelAction(
                                            new SequentialAction(
                                                    presets.ScoringPos(),
                                                    RedCloseTwoTwo_yellowScoringAction2
                                            ),
                                            claw.closeLClaw()
                                    )
                            )
                    );

                    camera.alignToTag(5);

                    if(!camera.targetFound){
                        Actions.runBlocking(
                                RedCloseTwoZero_yellowScoringOverrideAction2
                        );
                    }

                    Actions.runBlocking(
                            new SequentialAction(
                                    new SleepAction(.1),
                                    claw.openRClaw(),
                                    new SleepAction(.25),
                                    new ParallelAction(
                                            new SequentialAction(
                                                    presets.GroundPos(),
                                                    claw.openClaws(),
                                                    claw.whiteGroundClaw()
                                            ),
                                            RedCloseTwoTwo_driveToWhiteAction2
                                    ),
                                    new SequentialAction(
                                            presets.WhiteStack(),
                                            new SleepAction(0.25),
                                            RedCloseTwoTwo_whiteTrussAction,
                                            new ParallelAction(
                                                    RedCloseTwoTwo_whiteScoringAction,
                                                    presets.WhiteScoringPos()
                                            )
                                    ),

                                    new SequentialAction(
                                            new SleepAction(.25),
                                            claw.openClaws(),
                                            new SleepAction(.25),
                                            RedCloseTwoTwo_parkingAction,
                                            presets.WhiteGroundPos()
                                    )
                            )
                    );
                    sleep(400000);
                }


                //----------------------------3---------------------------\\
                if (blocks[i].x > 210 && blocks[i].id == 1 && blocks[i].y < 200) {
                    Actions.runBlocking(
                            new SequentialAction(
                                    new ParallelAction(
                                            presets.StartPos(),
                                            RedCloseTwoTwo_purpleAction3
                                    ),
                                    new SleepAction(0.1),
                                    claw.openLClaw(),
                                    new SleepAction(0.25),
                                    new ParallelAction(
                                            new SequentialAction(
                                                    presets.ScoringPos(),
                                                    RedCloseTwoTwo_yellowScoringAction3
                                            ),
                                            claw.closeLClaw()
                                    )
                            )
                    );

                    camera.alignToTag(6);

                    if(!camera.targetFound){
                        Actions.runBlocking(
                                RedCloseTwoZero_yellowScoringOverrideAction3
                        );
                    }

                    Actions.runBlocking(
                            new SequentialAction(
                                    new SleepAction(.1),
                                    claw.openRClaw(),
                                    new SleepAction(.25),
                                    new ParallelAction(
                                            new SequentialAction(
                                                    presets.GroundPos(),
                                                    claw.openClaws(),
                                                    claw.whiteGroundClaw()
                                            ),
                                            RedCloseTwoTwo_driveToWhiteAction3
                                    ),
                                    new SequentialAction(
                                            presets.WhiteStack(),
                                            new SleepAction(0.25),
                                            RedCloseTwoTwo_whiteTrussAction,
                                            new ParallelAction(
                                                    RedCloseTwoTwo_whiteScoringAction,
                                                    presets.WhiteScoringPos()
                                            )
                                    ),

                                    new SequentialAction(
                                            new SleepAction(.1),
                                            claw.openClaws(),
                                            new SleepAction(.25),
                                            RedCloseTwoTwo_parkingAction,
                                            presets.WhiteGroundPos()
                                    )
                            )
                    );
                    sleep(400000);
                }
            }
        }

    }


}

