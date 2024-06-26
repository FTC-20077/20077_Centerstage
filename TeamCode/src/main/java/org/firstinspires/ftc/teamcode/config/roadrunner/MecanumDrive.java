package org.firstinspires.ftc.teamcode.config.roadrunner;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.*;
import com.acmerobotics.roadrunner.AngularVelConstraint;
import com.acmerobotics.roadrunner.DualNum;
import com.acmerobotics.roadrunner.HolonomicController;
import com.acmerobotics.roadrunner.MecanumKinematics;
import com.acmerobotics.roadrunner.MinVelConstraint;
import com.acmerobotics.roadrunner.MotorFeedforward;
import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Pose2dDual;
import com.acmerobotics.roadrunner.ProfileAccelConstraint;
import com.acmerobotics.roadrunner.Time;
import com.acmerobotics.roadrunner.TimeTrajectory;
import com.acmerobotics.roadrunner.TimeTurn;
import com.acmerobotics.roadrunner.TrajectoryActionBuilder;
import com.acmerobotics.roadrunner.TurnConstraints;
import com.acmerobotics.roadrunner.Twist2dDual;
import com.acmerobotics.roadrunner.VelConstraint;
import com.acmerobotics.roadrunner.ftc.DownsampledWriter;
import com.acmerobotics.roadrunner.ftc.Encoder;
import com.acmerobotics.roadrunner.ftc.FlightRecorder;
import com.acmerobotics.roadrunner.ftc.LazyImu;
import com.acmerobotics.roadrunner.ftc.LynxFirmware;
import com.acmerobotics.roadrunner.ftc.OverflowEncoder;
import com.acmerobotics.roadrunner.ftc.PositionVelocityPair;
import com.acmerobotics.roadrunner.ftc.RawEncoder;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.opmode.archived.ancient.Drawing;
import org.firstinspires.ftc.teamcode.config.roadrunner.messages.DriveCommandMessage;
import org.firstinspires.ftc.teamcode.config.roadrunner.messages.MecanumCommandMessage;
import org.firstinspires.ftc.teamcode.config.roadrunner.messages.MecanumLocalizerInputsMessage;
import org.firstinspires.ftc.teamcode.config.roadrunner.messages.PoseMessage;

import java.lang.Math;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Config
public class MecanumDrive {
    public static class Params {
        // IMU orientation
        // TODO: fill in these values based on
        //   see https://ftc-docs.firstinspires.org/en/latest/programming_resources/imu/imu.html?highlight=imu#physical-hub-mounting
        public RevHubOrientationOnRobot.LogoFacingDirection logoFacingDirection =
                RevHubOrientationOnRobot.LogoFacingDirection.RIGHT;
        public RevHubOrientationOnRobot.UsbFacingDirection usbFacingDirection =
                RevHubOrientationOnRobot.UsbFacingDirection.UP;

        // drive model parameters
        public double inPerTick = 0.0029343584; //0.0029429942048504
        public double lateralInPerTick = 0.002023526540982968;
        public double trackWidthTicks = 5084.903040001737; //5103.7103507255215

        // feedforward parameters (in tick units)
        public double kS = 1.1953050792827469;
        public double kV = 0.00022;
        public double kA = 0.0001775;

        // path profile parameters (in inches)
        public double maxWheelVel = 60;
        public double minProfileAccel = -30;
        public double maxProfileAccel = 60;

        // turn profile parameters (in radians)
        public double maxAngVel = Math.PI; // shared with path
        public double maxAngAccel = Math.PI;

        // path controller gains
        public double axialGain = 6.669;
        public double lateralGain = 8.3;
        public double headingGain = 6.5; // shared with turn

        public double axialVelGain = 0;
        public double lateralVelGain = 0.0;
        public double headingVelGain = 0.0; // shared with turn
    }

    public static Params PARAMS = new Params();

    public final MecanumKinematics kinematics = new MecanumKinematics(
            PARAMS.inPerTick * PARAMS.trackWidthTicks, PARAMS.inPerTick / PARAMS.lateralInPerTick);

    public final TurnConstraints defaultTurnConstraints = new TurnConstraints(
            PARAMS.maxAngVel, -PARAMS.maxAngAccel, PARAMS.maxAngAccel);
    public final VelConstraint defaultVelConstraint =
            new MinVelConstraint(Arrays.asList(
                    kinematics.new WheelVelConstraint(PARAMS.maxWheelVel),
                    new AngularVelConstraint(PARAMS.maxAngVel)
            ));
    public final AccelConstraint defaultAccelConstraint =
            new ProfileAccelConstraint(PARAMS.minProfileAccel, PARAMS.maxProfileAccel);

    public final DcMotorEx leftFront, leftBack, rightBack, rightFront, lift, gear;

    public final Servo clawL, clawR, pivot, droneServo;

    public final VoltageSensor voltageSensor;

    public final LazyImu lazyImu;

    public final Localizer localizer;
    public Pose2d pose;

    public final LinkedList<Pose2d> poseHistory = new LinkedList<>();

    private final DownsampledWriter estimatedPoseWriter = new DownsampledWriter("ESTIMATED_POSE", 50_000_000);
    private final DownsampledWriter targetPoseWriter = new DownsampledWriter("TARGET_POSE", 50_000_000);
    private final DownsampledWriter driveCommandWriter = new DownsampledWriter("DRIVE_COMMAND", 50_000_000);
    private final DownsampledWriter mecanumCommandWriter = new DownsampledWriter("MECANUM_COMMAND", 50_000_000);

    public class DriveLocalizer implements Localizer {
        public final Encoder leftFront, leftBack, rightBack, rightFront;
        public final IMU imu;

        private int lastLeftFrontPos, lastLeftBackPos, lastRightBackPos, lastRightFrontPos;
        private Rotation2d lastHeading;
        private boolean initialized;

        public DriveLocalizer() {
            leftFront = new OverflowEncoder(new RawEncoder(MecanumDrive.this.leftFront));
            leftBack = new OverflowEncoder(new RawEncoder(MecanumDrive.this.leftBack));
            rightBack = new OverflowEncoder(new RawEncoder(MecanumDrive.this.rightBack));
            rightFront = new OverflowEncoder(new RawEncoder(MecanumDrive.this.rightFront));

            imu = lazyImu.get();

            // TODO: reverse encoders if needed
            leftFront.setDirection(DcMotorSimple.Direction.REVERSE);
        }

        @Override
        public Twist2dDual<Time> update() {
            PositionVelocityPair leftFrontPosVel = leftFront.getPositionAndVelocity();
            PositionVelocityPair leftBackPosVel = leftBack.getPositionAndVelocity();
            PositionVelocityPair rightBackPosVel = rightBack.getPositionAndVelocity();
            PositionVelocityPair rightFrontPosVel = rightFront.getPositionAndVelocity();

            YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();

            FlightRecorder.write("MECANUM_LOCALIZER_INPUTS", new MecanumLocalizerInputsMessage(
                    leftFrontPosVel, leftBackPosVel, rightBackPosVel, rightFrontPosVel, angles));

            Rotation2d heading = Rotation2d.exp(angles.getYaw(AngleUnit.RADIANS));

            if (!initialized) {
                initialized = true;

                lastLeftFrontPos = leftFrontPosVel.position;
                lastLeftBackPos = leftBackPosVel.position;
                lastRightBackPos = rightBackPosVel.position;
                lastRightFrontPos = rightFrontPosVel.position;

                lastHeading = heading;

                return new Twist2dDual<>(
                        Vector2dDual.constant(new Vector2d(0.0, 0.0), 2),
                        DualNum.constant(0.0, 2)
                );
            }

            double headingDelta = heading.minus(lastHeading);
            Twist2dDual<Time> twist = kinematics.forward(new MecanumKinematics.WheelIncrements<>(
                    new DualNum<Time>(new double[]{
                            (leftFrontPosVel.position - lastLeftFrontPos),
                            leftFrontPosVel.velocity,
                    }).times(PARAMS.inPerTick),
                    new DualNum<Time>(new double[]{
                            (leftBackPosVel.position - lastLeftBackPos),
                            leftBackPosVel.velocity,
                    }).times(PARAMS.inPerTick),
                    new DualNum<Time>(new double[]{
                            (rightBackPosVel.position - lastRightBackPos),
                            rightBackPosVel.velocity,
                    }).times(PARAMS.inPerTick),
                    new DualNum<Time>(new double[]{
                            (rightFrontPosVel.position - lastRightFrontPos),
                            rightFrontPosVel.velocity,
                    }).times(PARAMS.inPerTick)
            ));

            lastLeftFrontPos = leftFrontPosVel.position;
            lastLeftBackPos = leftBackPosVel.position;
            lastRightBackPos = rightBackPosVel.position;
            lastRightFrontPos = rightFrontPosVel.position;

            lastHeading = heading;

            return new Twist2dDual<>(
                    twist.line,
                    DualNum.cons(headingDelta, twist.angle.drop(1))
            );
        }
    }

    public MecanumDrive(HardwareMap hardwareMap, Pose2d pose) {
        this.pose = pose;

        LynxFirmware.throwIfModulesAreOutdated(hardwareMap);

        for (LynxModule module : hardwareMap.getAll(LynxModule.class)) {
            module.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        // TODO: make sure your config has motors with these names (or change them)
        //   see https://ftc-docs.firstinspires.org/en/latest/hardware_and_software_configuration/configuring/index.html
        leftFront = hardwareMap.get(DcMotorEx.class, "lF");
        leftBack = hardwareMap.get(DcMotorEx.class, "lB");
        rightBack = hardwareMap.get(DcMotorEx.class, "rB");
        rightFront = hardwareMap.get(DcMotorEx.class, "rF");
        lift = hardwareMap.get(DcMotorEx.class, "lift");//0
        gear = hardwareMap.get(DcMotorEx.class, "gear");//1
        pivot = hardwareMap.get(Servo.class, "pivot");
        //claw = hardwareMap.get(Servo.class, "claw");
        droneServo = hardwareMap.get(Servo.class, "droneServo");
        clawL = hardwareMap.get(Servo.class, "clawL");
        clawR = hardwareMap.get(Servo.class, "clawR");

        leftFront.setDirection(DcMotor.Direction.REVERSE);
        leftBack.setDirection(DcMotor.Direction.REVERSE);
        rightFront.setDirection(DcMotor.Direction.FORWARD);
        rightBack.setDirection(DcMotor.Direction.FORWARD);
        gear.setDirection(DcMotor.Direction.REVERSE);
        gear.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        lift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        gear.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftBack.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightBack.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // TODO: reverse motor directions if needed
        //   leftFront.setDirection(DcMotorSimple.Direction.REVERSE);

        // TODO: make sure your config has an IMU with this name (can be BNO or BHI)
        //   see https://ftc-docs.firstinspires.org/en/latest/hardware_and_software_configuration/configuring/index.html
        lazyImu = new LazyImu(hardwareMap, "imu", new RevHubOrientationOnRobot(
                PARAMS.logoFacingDirection, PARAMS.usbFacingDirection));

        voltageSensor = hardwareMap.voltageSensor.iterator().next();

        localizer = new ThreeDeadWheelLocalizer(hardwareMap, PARAMS.inPerTick);

        FlightRecorder.write("MECANUM_PARAMS", PARAMS);
    }

    public void setDrivePowers(PoseVelocity2d powers) {
        MecanumKinematics.WheelVelocities<Time> wheelVels = new MecanumKinematics(1).inverse(
                PoseVelocity2dDual.constant(powers, 1));

        double maxPowerMag = 1;
        for (DualNum<Time> power : wheelVels.all()) {
            maxPowerMag = Math.max(maxPowerMag, power.value());
        }

        leftFront.setPower(wheelVels.leftFront.get(0) / maxPowerMag);
        leftBack.setPower(wheelVels.leftBack.get(0) / maxPowerMag);
        rightBack.setPower(wheelVels.rightBack.get(0) / maxPowerMag);
        rightFront.setPower(wheelVels.rightFront.get(0) / maxPowerMag);
    }

    public final class FollowTrajectoryAction implements Action {
        public final TimeTrajectory timeTrajectory;
        private double beginTs = -1;

        private final double[] xPoints, yPoints;

        public FollowTrajectoryAction(TimeTrajectory t) {
            timeTrajectory = t;

            List<Double> disps = com.acmerobotics.roadrunner.Math.range(
                    0, t.path.length(),
                    Math.max(2, (int) Math.ceil(t.path.length() / 2)));
            xPoints = new double[disps.size()];
            yPoints = new double[disps.size()];
            for (int i = 0; i < disps.size(); i++) {
                Pose2d p = t.path.get(disps.get(i), 1).value();
                xPoints[i] = p.position.x;
                yPoints[i] = p.position.y;
            }
        }

        @Override
        public boolean run(@NonNull TelemetryPacket p) {
            double t;
            if (beginTs < 0) {
                beginTs = Actions.now();
                t = 0;
            } else {
                t = Actions.now() - beginTs;
            }

            if (t >= timeTrajectory.duration) {
                leftFront.setPower(0);
                leftBack.setPower(0);
                rightBack.setPower(0);
                rightFront.setPower(0);

                return false;
            }

            Pose2dDual<Time> txWorldTarget = timeTrajectory.get(t);
            targetPoseWriter.write(new PoseMessage(txWorldTarget.value()));

            PoseVelocity2d robotVelRobot = updatePoseEstimate();

            PoseVelocity2dDual<Time> command = new HolonomicController(
                    PARAMS.axialGain, PARAMS.lateralGain, PARAMS.headingGain,
                    PARAMS.axialVelGain, PARAMS.lateralVelGain, PARAMS.headingVelGain
            )
                    .compute(txWorldTarget, pose, robotVelRobot);
            driveCommandWriter.write(new DriveCommandMessage(command));

            MecanumKinematics.WheelVelocities<Time> wheelVels = kinematics.inverse(command);
            double voltage = voltageSensor.getVoltage();

            final MotorFeedforward feedforward = new MotorFeedforward(PARAMS.kS,
                    PARAMS.kV / PARAMS.inPerTick, PARAMS.kA / PARAMS.inPerTick);
            double leftFrontPower = feedforward.compute(wheelVels.leftFront) / voltage;
            double leftBackPower = feedforward.compute(wheelVels.leftBack) / voltage;
            double rightBackPower = feedforward.compute(wheelVels.rightBack) / voltage;
            double rightFrontPower = feedforward.compute(wheelVels.rightFront) / voltage;
            mecanumCommandWriter.write(new MecanumCommandMessage(
                    voltage, leftFrontPower, leftBackPower, rightBackPower, rightFrontPower
            ));

            leftFront.setPower(leftFrontPower);
            leftBack.setPower(leftBackPower);
            rightBack.setPower(rightBackPower);
            rightFront.setPower(rightFrontPower);

            p.put("x", pose.position.x);
            p.put("y", pose.position.y);
            p.put("heading (deg)", Math.toDegrees(pose.heading.toDouble()));

            Pose2d error = txWorldTarget.value().minusExp(pose);
            p.put("xError", error.position.x);
            p.put("yError", error.position.y);
            p.put("headingError (deg)", Math.toDegrees(error.heading.toDouble()));

            // only draw when active; only one drive action should be active at a time
            Canvas c = p.fieldOverlay();
            drawPoseHistory(c);

            c.setStroke("#4CAF50");
            Drawing.drawRobot(c, txWorldTarget.value());

            c.setStroke("#3F51B5");
            Drawing.drawRobot(c, pose);

            c.setStroke("#4CAF50FF");
            c.setStrokeWidth(1);
            c.strokePolyline(xPoints, yPoints);

            return true;
        }

        @Override
        public void preview(Canvas c) {
            c.setStroke("#4CAF507A");
            c.setStrokeWidth(1);
            c.strokePolyline(xPoints, yPoints);
        }
    }

    public final class TurnAction implements Action {
        private final TimeTurn turn;

        private double beginTs = -1;

        public TurnAction(TimeTurn turn) {
            this.turn = turn;
        }

        @Override
        public boolean run(@NonNull TelemetryPacket p) {
            double t;
            if (beginTs < 0) {
                beginTs = Actions.now();
                t = 0;
            } else {
                t = Actions.now() - beginTs;
            }

            if (t >= turn.duration) {
                leftFront.setPower(0);
                leftBack.setPower(0);
                rightBack.setPower(0);
                rightFront.setPower(0);

                return false;
            }

            Pose2dDual<Time> txWorldTarget = turn.get(t);
            targetPoseWriter.write(new PoseMessage(txWorldTarget.value()));

            PoseVelocity2d robotVelRobot = updatePoseEstimate();

            PoseVelocity2dDual<Time> command = new HolonomicController(
                    PARAMS.axialGain, PARAMS.lateralGain, PARAMS.headingGain,
                    PARAMS.axialVelGain, PARAMS.lateralVelGain, PARAMS.headingVelGain
            )
                    .compute(txWorldTarget, pose, robotVelRobot);
            driveCommandWriter.write(new DriveCommandMessage(command));

            MecanumKinematics.WheelVelocities<Time> wheelVels = kinematics.inverse(command);
            double voltage = voltageSensor.getVoltage();
            final MotorFeedforward feedforward = new MotorFeedforward(PARAMS.kS,
                    PARAMS.kV / PARAMS.inPerTick, PARAMS.kA / PARAMS.inPerTick);
            double leftFrontPower = feedforward.compute(wheelVels.leftFront) / voltage;
            double leftBackPower = feedforward.compute(wheelVels.leftBack) / voltage;
            double rightBackPower = feedforward.compute(wheelVels.rightBack) / voltage;
            double rightFrontPower = feedforward.compute(wheelVels.rightFront) / voltage;
            mecanumCommandWriter.write(new MecanumCommandMessage(
                    voltage, leftFrontPower, leftBackPower, rightBackPower, rightFrontPower
            ));

            leftFront.setPower(feedforward.compute(wheelVels.leftFront) / voltage);
            leftBack.setPower(feedforward.compute(wheelVels.leftBack) / voltage);
            rightBack.setPower(feedforward.compute(wheelVels.rightBack) / voltage);
            rightFront.setPower(feedforward.compute(wheelVels.rightFront) / voltage);

            Canvas c = p.fieldOverlay();
            drawPoseHistory(c);

            c.setStroke("#4CAF50");
            Drawing.drawRobot(c, txWorldTarget.value());

            c.setStroke("#3F51B5");
            Drawing.drawRobot(c, pose);

            c.setStroke("#7C4DFFFF");
            c.fillCircle(turn.beginPose.position.x, turn.beginPose.position.y, 2);

            return true;
        }

        @Override
        public void preview(Canvas c) {
            c.setStroke("#7C4DFF7A");
            c.fillCircle(turn.beginPose.position.x, turn.beginPose.position.y, 2);
        }
    }

    public PoseVelocity2d updatePoseEstimate() {
        Twist2dDual<Time> twist = localizer.update();
        pose = pose.plus(twist.value());

        poseHistory.add(pose);
        while (poseHistory.size() > 100) {
            poseHistory.removeFirst();
        }

        estimatedPoseWriter.write(new PoseMessage(pose));

        return twist.velocity().value();
    }

    private void drawPoseHistory(Canvas c) {
        double[] xPoints = new double[poseHistory.size()];
        double[] yPoints = new double[poseHistory.size()];

        int i = 0;
        for (Pose2d t : poseHistory) {
            xPoints[i] = t.position.x;
            yPoints[i] = t.position.y;

            i++;
        }

        c.setStrokeWidth(1);
        c.setStroke("#3F51B5");
        c.strokePolyline(xPoints, yPoints);
    }

    public TrajectoryActionBuilder actionBuilder(Pose2d beginPose) {
        return new TrajectoryActionBuilder(
                TurnAction::new,
                FollowTrajectoryAction::new,
                new TrajectoryBuilderParams(
                        1e-6,
                        new ProfileParams(
                                0.25, 0.1, 1e-2
                        )
                ),
                beginPose, 0.0,
                defaultTurnConstraints,
                defaultVelConstraint, defaultAccelConstraint
        );
    }
    /*
    //---------------------------------------- Blue Close 2+0 ----------------------------------------------\\
    Pose2d BlueCloseTwoZero_startPose = new Pose2d(-62, 12, 0);
    Pose2d BlueCloseTwoZero_yellowScoringPose1 = new Pose2d(-40, 29, Math.toRadians(270));
    Pose2d BlueCloseTwoZero_yellowScoringPose2 = new Pose2d(-28.5, 24, Math.toRadians(270));
    Pose2d BlueCloseTwoZero_yellowScoringPose3 = new Pose2d(-33.5, 10.5, Math.toRadians(270));
    Pose2d BlueCloseTwoZero_parkingPose1 = new Pose2d(-42, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoZero_parkingPose2 = new Pose2d(-36, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoZero_parkingPose3 = new Pose2d(-30, 55, Math.toRadians(270));


    //This action drives to the first tape line
    TrajectoryActionBuilder BlueCloseTwoZero_purpleTAction1 = actionBuilder(BlueCloseTwoZero_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-40, 29), Math.toRadians(270));

    public Action BlueCloseTwoZero_purpleAction1 = BlueCloseTwoZero_purpleTAction1.build();

    //This action drives to the second tape line
    TrajectoryActionBuilder BlueCloseTwoZero_purpleTAction2 = actionBuilder(BlueCloseTwoZero_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-31, 24), Math.toRadians(270));

    public Action BlueCloseTwoZero_purpleAction2 = BlueCloseTwoZero_purpleTAction2.build();

    //This action drives to the third tape line
    TrajectoryActionBuilder BlueCloseTwoZero_purpleTAction3 = actionBuilder(BlueCloseTwoZero_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-33.5, 10.5), Math.toRadians(270));

    public Action BlueCloseTwoZero_purpleAction3 = BlueCloseTwoZero_purpleTAction3.build();

    //This action drives to the first backdrop section


    TrajectoryActionBuilder BlueCloseTwoZero_yellowScoringTAction1 = actionBuilder(BlueCloseTwoZero_yellowScoringPose1)
            .strafeTo(new Vector2d(-38, 45));

    public Action BlueCloseTwoZero_yellowScoringAction1 = BlueCloseTwoZero_yellowScoringTAction1.build();


    //This action drives to the second backdrop section
    TrajectoryActionBuilder BlueCloseTwoZero_yellowScoringTAction2 = actionBuilder(BlueCloseTwoZero_yellowScoringPose2)
            .strafeTo(new Vector2d(-32, 45));


    public Action BlueCloseTwoZero_yellowScoringAction2 = BlueCloseTwoZero_yellowScoringTAction2.build();

    //This action drives to the third backdrop section
    TrajectoryActionBuilder BlueCloseTwoZero_yellowScoringTAction3 = actionBuilder(BlueCloseTwoZero_yellowScoringPose3)
            .strafeTo(new Vector2d(-33.5,15))
            .strafeTo(new Vector2d(-22, 45));


    public Action BlueCloseTwoZero_yellowScoringAction3 = BlueCloseTwoZero_yellowScoringTAction3.build();


    //This action drives to robot to the first parking zone
    TrajectoryActionBuilder BlueCloseTwoZero_parkingTAction1 = actionBuilder(BlueCloseTwoZero_parkingPose1)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoZero_parkingAction1 = BlueCloseTwoZero_parkingTAction1.build();

    //This action drives to robot to the second parking zone
    TrajectoryActionBuilder BlueCloseTwoZero_parkingTAction2 = actionBuilder(BlueCloseTwoZero_parkingPose2)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoZero_parkingAction2 = BlueCloseTwoZero_parkingTAction2.build();

    //This action drives to robot to the third parking zone
    TrajectoryActionBuilder BlueCloseTwoZero_parkingTAction3 = actionBuilder(BlueCloseTwoZero_parkingPose3)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoZero_parkingAction3 = BlueCloseTwoZero_parkingTAction3.build();

    //---------------------------------------- Blue Close 2+2 ----------------------------------------------\\
    Pose2d BlueCloseTwoTwo_startPose = new Pose2d(-62, 12, 0);
    Pose2d BlueCloseTwoTwo_yellowScoringPose1 = new Pose2d(-40, 29, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_yellowScoringPose2 = new Pose2d(-28.5, 24, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_yellowScoringPose3 = new Pose2d(-33.5, 10.5, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_driveToWhitePose1 = new Pose2d(-42, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_driveToWhitePose2 = new Pose2d(-36, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_driveToWhitePose3 = new Pose2d(-30, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_whiteScoringPose = new Pose2d(-40, 29, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_parkingPose1 = new Pose2d(-42, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_parkingPose2 = new Pose2d(-36, 55, Math.toRadians(270));
    Pose2d BlueCloseTwoTwo_parkingPose3 = new Pose2d(-30, 55, Math.toRadians(270));


    //This action drives to the first tape line
    TrajectoryActionBuilder BlueCloseTwoTwo_purpleTAction1 = actionBuilder(BlueCloseTwoTwo_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-40, 29), Math.toRadians(270));

    public Action BlueCloseTwoTwo_purpleAction1 = BlueCloseTwoTwo_purpleTAction1.build();

    //This action drives to the second tape line
    TrajectoryActionBuilder BlueCloseTwoTwo_purpleTAction2 = actionBuilder(BlueCloseTwoTwo_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-31, 24), Math.toRadians(270));

    public Action BlueCloseTwoTwo_purpleAction2 = BlueCloseTwoTwo_purpleTAction2.build();

    //This action drives to the third tape line
    TrajectoryActionBuilder BlueCloseTwoTwo_purpleTAction3 = actionBuilder(BlueCloseTwoTwo_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-33.5, 10.5), Math.toRadians(270));

    public Action BlueCloseTwoTwo_purpleAction3 = BlueCloseTwoTwo_purpleTAction3.build();

    //This action drives to the first backdrop section
    TrajectoryActionBuilder BlueCloseTwoTwo_yellowScoringTAction1 = actionBuilder(BlueCloseTwoTwo_yellowScoringPose1)
            .strafeTo(new Vector2d(-38, 45));

    public Action BlueCloseTwoTwo_yellowScoringAction1 = BlueCloseTwoTwo_yellowScoringTAction1.build();


    //This action drives to the second backdrop section
    TrajectoryActionBuilder BlueCloseTwoTwo_yellowScoringTAction2 = actionBuilder(BlueCloseTwoTwo_yellowScoringPose2)
            .strafeTo(new Vector2d(-32, 45));

    public Action BlueCloseTwoTwo_yellowScoringAction2 = BlueCloseTwoTwo_yellowScoringTAction2.build();

    //This action drives to the third backdrop section
    TrajectoryActionBuilder BlueCloseTwoTwo_yellowScoringTAction3 = actionBuilder(BlueCloseTwoTwo_yellowScoringPose3)
            .strafeTo(new Vector2d(-33.5,15))
            .strafeTo(new Vector2d(-22, 45));
    public Action BlueCloseTwoTwo_yellowScoringAction3 = BlueCloseTwoTwo_yellowScoringTAction3.build();

    //This action drives to robot to the white pixel stack
    TrajectoryActionBuilder BlueCloseTwoTwo_driveToWhiteTAction1 = actionBuilder(BlueCloseTwoTwo_driveToWhitePose1)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoTwo_driveToWhiteAction1 = BlueCloseTwoTwo_driveToWhiteTAction1.build();

    //This action drives to robot to the white pixel stack
    TrajectoryActionBuilder BlueCloseTwoTwo_driveToWhiteTAction2 = actionBuilder(BlueCloseTwoTwo_driveToWhitePose2)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoTwo_driveToWhiteAction2 = BlueCloseTwoTwo_driveToWhiteTAction2.build();

    //This action drives to robot to the white pixel stack
    TrajectoryActionBuilder BlueCloseTwoTwo_driveToWhiteTAction3 = actionBuilder(BlueCloseTwoTwo_driveToWhitePose3)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoTwo_driveToWhiteAction3 = BlueCloseTwoTwo_driveToWhiteTAction3.build();

    //This action drives to robot to the parking zone
    TrajectoryActionBuilder BlueCloseTwoTwo_parkingTAction1 = actionBuilder(BlueCloseTwoTwo_parkingPose1)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoTwo_parkingAction1 = BlueCloseTwoTwo_parkingTAction1.build();

    //This action drives to robot to the parking zone
    TrajectoryActionBuilder BlueCloseTwoTwo_parkingTAction2 = actionBuilder(BlueCloseTwoTwo_parkingPose2)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoTwo_parkingAction2 = BlueCloseTwoTwo_parkingTAction2.build();

    //This action drives to robot to the parking zone
    TrajectoryActionBuilder BlueCloseTwoTwo_parkingTAction3 = actionBuilder(BlueCloseTwoTwo_parkingPose3)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueCloseTwoTwo_parkingAction3 = BlueCloseTwoTwo_parkingTAction3.build();


    //------------------------------------------- Red Close -------------------------------------------------\\
    Pose2d RedCloseTwoZero_startPose = new Pose2d(62, 12, Math.toRadians(180));
    Pose2d RedCloseTwoZero_yellowScoringPose1 = new Pose2d(40, 29, Math.toRadians(270));
    Pose2d RedCloseTwoZero_yellowScoringPose2 = new Pose2d(28.5, 24, Math.toRadians(270));
    Pose2d RedCloseTwoZero_yellowScoringPose3 = new Pose2d(33.5, 10.5, Math.toRadians(270));
    Pose2d RedCloseTwoZero_parkingPose3 = new Pose2d(44, 55, Math.toRadians(270));
    Pose2d RedCloseTwoZero_parkingPose2 = new Pose2d(38, 55, Math.toRadians(270));
    Pose2d RedCloseTwoZero_parkingPose1 = new Pose2d(32, 55, Math.toRadians(270));


    //This action drives to the third tape line
    TrajectoryActionBuilder RedCloseTwoZero_purpleTAction3 = actionBuilder(RedCloseTwoZero_startPose)
            .lineToX(55)
            .splineTo(new Vector2d(34, 31), Math.toRadians(270));

    public Action RedCloseTwoZero_purpleAction3 = RedCloseTwoZero_purpleTAction3.build();

    //This action drives to the second tape line
    TrajectoryActionBuilder RedCloseTwoZero_purpleTAction2 = actionBuilder(RedCloseTwoZero_startPose)
            .lineToX(55)
            .splineTo(new Vector2d(26, 24), Math.toRadians(270));

    public Action RedCloseTwoZero_purpleAction2 = RedCloseTwoZero_purpleTAction2.build();

    //This action drives to the first tape line
    TrajectoryActionBuilder RedCloseTwoZero_purpleTAction1 = actionBuilder(RedCloseTwoZero_startPose)
            .lineToX(55)
            .splineTo(new Vector2d(33, 12.5), Math.toRadians(270));

    public Action RedCloseTwoZero_purpleAction1 = RedCloseTwoZero_purpleTAction1.build();

    //This action drives to the first backdrop section

    TrajectoryActionBuilder RedCloseTwoZero_yellowScoringTAction1 = actionBuilder(RedCloseTwoZero_yellowScoringPose1)
            .strafeTo(new Vector2d(20.5, 46));

    public Action RedCloseTwoZero_yellowScoringAction1 = RedCloseTwoZero_yellowScoringTAction1.build();


    //This action drives to the second backdrop section
    TrajectoryActionBuilder RedCloseTwoZero_yellowScoringTAction2 = actionBuilder(RedCloseTwoZero_yellowScoringPose2)
            .lineToY(30)
            .strafeTo(new Vector2d(38, 45));


    public Action RedCloseTwoZero_yellowScoringAction2 = RedCloseTwoZero_yellowScoringTAction2.build();

    //This action drives to the third backdrop section
    TrajectoryActionBuilder RedCloseTwoZero_yellowScoringTAction3 = actionBuilder(RedCloseTwoZero_yellowScoringPose3)
            .lineToY(38)
            .strafeTo(new Vector2d(43.5, 45));


    public Action RedCloseTwoZero_yellowScoringAction3 = RedCloseTwoZero_yellowScoringTAction3.build();


    //This action drives to robot to the first parking zone
    TrajectoryActionBuilder RedCloseTwoZero_parkingTAction1 = actionBuilder(RedCloseTwoZero_parkingPose1)
            .strafeTo(new Vector2d(38,43))
            .strafeTo((new Vector2d(69, 50)));

    public Action RedCloseTwoZero_parkingAction1 = RedCloseTwoZero_parkingTAction1.build();

    //This action drives to robot to the second parking zone
    TrajectoryActionBuilder RedCloseTwoZero_parkingTAction2 = actionBuilder(RedCloseTwoZero_parkingPose2)
            .strafeTo(new Vector2d(38,43))
            .strafeTo((new Vector2d(69, 50)));

    public Action RedCloseTwoZero_parkingAction2 = RedCloseTwoZero_parkingTAction2.build();

    //This action drives to robot to the third parking zone
    TrajectoryActionBuilder RedCloseTwoZero_parkingTAction3 = actionBuilder(RedCloseTwoZero_parkingPose3)
            .strafeTo(new Vector2d(38,43))
            .strafeTo((new Vector2d(69, 50)));

    public Action RedCloseTwoZero_parkingAction3 = RedCloseTwoZero_parkingTAction3.build();

    //---------------------------------------- Blue Far ----------------------------------------------\\
    Pose2d BlueFarTwoOne_startPose = new Pose2d(-62, -36, 0);
    Pose2d BlueFarTwoOne_whitePose1 = new Pose2d(-40, 29, Math.toRadians(270));
    Pose2d BlueFarTwoOne_whitePose2 = new Pose2d(-28.5, 24, Math.toRadians(270));
    Pose2d BlueFarTwoOne_whitePose3 = new Pose2d(-33.5, 10.5, Math.toRadians(270));
    Pose2d BlueFarTwoOne_yellowScoringPose1 = new Pose2d(-40, 29, Math.toRadians(270));
    Pose2d BlueFarTwoOne_yellowScoringPose2 = new Pose2d(-28.5, 24, Math.toRadians(270));
    Pose2d BlueFarTwoOne_yellowScoringPose3 = new Pose2d(-33.5, 10.5, Math.toRadians(270));
    Pose2d BlueFarTwoOne_parkingPose1 = new Pose2d(-42, 55, Math.toRadians(270));
    Pose2d BlueFarTwoOne_parkingPose2 = new Pose2d(-36, 55, Math.toRadians(270));
    Pose2d BlueFarTwoOne_parkingPose3 = new Pose2d(-30, 55, Math.toRadians(270));


    //This action drives to the first tape line
    TrajectoryActionBuilder BlueFarTwoOne_purpleTAction1 = actionBuilder(BlueFarTwoOne_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-27, -36), Math.toRadians(90));

    public Action BlueFarTwoOne_purpleAction1 = BlueFarTwoOne_purpleTAction1.build();

    //This action drives to the second tape line
    TrajectoryActionBuilder BlueFarTwoOne_purpleTAction2 = actionBuilder(BlueFarTwoOne_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-36, 36), Math.toRadians(0));

    public Action BlueFarTwoOne_purpleAction2 = BlueFarTwoOne_purpleTAction2.build();

    //This action drives to the third tape line
    TrajectoryActionBuilder BlueFarTwoOne_purpleTAction3 = actionBuilder(BlueFarTwoOne_startPose)
            .setTangent(0)
            .lineToX(-55)
            .splineTo(new Vector2d(-27, -36), Math.toRadians(270));

    public Action BlueFarTwoOne_purpleAction3 = BlueFarTwoOne_purpleTAction3.build();

    //This action drives to the first tape line
    TrajectoryActionBuilder BlueFarTwoOne_WhiteTAction1 = actionBuilder(BlueFarTwoOne_whitePose1)
            .lineToX(-42)
            .splineTo(new Vector2d(-36, -48), Math.toRadians(270));

    public Action BlueFarTwoOne_WhiteAction1 = BlueFarTwoOne_WhiteTAction1.build();

    //This action drives to the second tape line
    TrajectoryActionBuilder BlueFarTwoOne_WhiteTAction2 = actionBuilder(BlueFarTwoOne_whitePose2)
            .lineToX(-42)
            .splineTo(new Vector2d(-36, -48), Math.toRadians(270));

    public Action BlueFarTwoOne_WhiteAction2 = BlueFarTwoOne_WhiteTAction2.build();

    //This action drives to the third tape line
    TrajectoryActionBuilder BlueFarTwoOne_WhiteTAction3 = actionBuilder(BlueFarTwoOne_whitePose3)
            .lineToY(-44)
            .strafeTo(new Vector2d(-36, -48));

    public Action BlueFarTwoOne_WhiteAction3 = BlueFarTwoOne_WhiteTAction3.build();

    //This action drives to the first backdrop section


    TrajectoryActionBuilder BlueFarTwoOne_yellowScoringTAction1 = actionBuilder(BlueFarTwoOne_yellowScoringPose1)
            .strafeTo(new Vector2d(-38, 45));

    public Action BlueFarTwoOne_yellowScoringAction1 = BlueFarTwoOne_yellowScoringTAction1.build();


    //This action drives to the second backdrop section
    TrajectoryActionBuilder BlueFarTwoOne_yellowScoringTAction2 = actionBuilder(BlueFarTwoOne_yellowScoringPose2)
            .strafeTo(new Vector2d(-32, 45));


    public Action BlueFarTwoOne_yellowScoringAction2 = BlueFarTwoOne_yellowScoringTAction2.build();

    //This action drives to the third backdrop section
    TrajectoryActionBuilder BlueFarTwoOne_yellowScoringTAction3 = actionBuilder(BlueFarTwoOne_yellowScoringPose3)
            .strafeTo(new Vector2d(-33.5,15))
            .strafeTo(new Vector2d(-22, 45));


    public Action BlueFarTwoOne_yellowScoringAction3 = BlueFarTwoOne_yellowScoringTAction3.build();


    //This action drives to robot to the first parking zone
    TrajectoryActionBuilder BlueFarTwoOne_parkingTAction1 = actionBuilder(BlueFarTwoOne_parkingPose1)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueFarTwoOne_parkingAction1 = BlueFarTwoOne_parkingTAction1.build();

    //This action drives to robot to the second parking zone
    TrajectoryActionBuilder BlueFarTwoOne_parkingTAction2 = actionBuilder(BlueFarTwoOne_parkingPose2)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueFarTwoOne_parkingAction2 = BlueFarTwoOne_parkingTAction2.build();

    //This action drives to robot to the third parking zone
    TrajectoryActionBuilder BlueFarTwoOne_parkingTAction3 = actionBuilder(BlueFarTwoOne_parkingPose3)
            .strafeTo(new Vector2d(-36,43))
            .strafeTo((new Vector2d(-67, 50)));

    public Action BlueFarTwoOne_parkingAction3 = BlueFarTwoOne_parkingTAction3.build();
    */
}