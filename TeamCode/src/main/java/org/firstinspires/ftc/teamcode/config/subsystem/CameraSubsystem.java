package org.firstinspires.ftc.teamcode.config.subsystem;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.telemetry;
import androidx.annotation.NonNull;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.hardware.DcMotor;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.vision.VisionPortal;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class CameraSubsystem {

    public static class Camera {

        /* Motor Intialization */
        private DcMotor leftFrontDrive = null;
        private DcMotor leftBackDrive = null;
        private DcMotor rightFrontDrive = null;
        private DcMotor rightBackDrive = null;

        /* April Tag Movement Values */
        double DESIRED_DISTANCE = 1.5; // In Inches
        final double SPEED_GAIN = -0.02;   // Drive = Error * Gain
        final double STRAFE_GAIN = -0.01;
        double TURN_GAIN = 0.01;
        double MAX_AUTO_SPEED = 0.9;
        double MAX_AUTO_STRAFE = 0.9;
        double MAX_AUTO_TURN = 0.65;
        double forward;
        double strafe;
        double turn;
        boolean targetFound = false;


        /* Camera Initialization */
        private ElapsedTime aprilTagTime = new ElapsedTime();
        private static final boolean USE_WEBCAM = true;
        private VisionPortal visionPortal;
        private AprilTagProcessor aprilTag;
        private AprilTagDetection desiredTag = null;
        private int aprilTagDecimation = 3;


        public Camera(HardwareMap hardwareMap) {
            leftFrontDrive = hardwareMap.get(DcMotor.class, "lF");
            rightFrontDrive = hardwareMap.get(DcMotor.class, "rF");
            leftBackDrive = hardwareMap.get(DcMotor.class, "lB");
            rightBackDrive = hardwareMap.get(DcMotor.class, "rB");
            leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
            leftBackDrive.setDirection(DcMotor.Direction.REVERSE);
            rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
            rightBackDrive.setDirection(DcMotor.Direction.FORWARD);
        }


        public void moveRobot(double x, double y, double yaw) {
            double leftFrontPower    =  x -y -yaw;
            double rightFrontPower   =  x +y +yaw;
            double leftBackPower     =  x +y -yaw;
            double rightBackPower    =  x -y +yaw;

            // Normalize wheel powers to be less than 1.0
            double max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
            max = Math.max(max, Math.abs(leftBackPower));
            max = Math.max(max, Math.abs(rightBackPower));

            if (max > 1.0) {
                leftFrontPower /= max;
                rightFrontPower /= max;
                leftBackPower /= max;
                rightBackPower /= max;
            }

            // Send powers to the wheels.
            leftFrontDrive.setPower(leftFrontPower);
            rightFrontDrive.setPower(rightFrontPower);
            leftBackDrive.setPower(leftBackPower);
            rightBackDrive.setPower(rightBackPower);
        }


        void alignToTag(int DESIRED_TAG_ID) {
            List<AprilTagDetection> currentDetections = aprilTag.getDetections();

            for (AprilTagDetection detection : currentDetections) {
                if (detection.metadata != null) {
                    if ((DESIRED_TAG_ID < 0) || (detection.id == DESIRED_TAG_ID)) {
                        targetFound = true;
                        desiredTag = detection;
                        break;
                    } else {
                        telemetry.addData("Skipping", "Tag ID %d is not desired", detection.id);
                    }
                } else {
                    telemetry.addData("Unknown", "Tag ID %d is not in TagLibrary", detection.id);
                }
            }

            if (targetFound) {
                // Determine heading, range and Yaw (tag image rotation) error
                double rangeError = (desiredTag.ftcPose.range - DESIRED_DISTANCE);
                double headingError = desiredTag.ftcPose.bearing;
                double yawError = desiredTag.ftcPose.yaw;

                // Use the speed and turn "gains" to calculate how we want the robot to move.
                forward = Range.clip(rangeError * SPEED_GAIN, -MAX_AUTO_SPEED, MAX_AUTO_SPEED);
                turn = Range.clip(headingError * TURN_GAIN, -MAX_AUTO_TURN, MAX_AUTO_TURN);
                strafe = Range.clip(-yawError * STRAFE_GAIN, -MAX_AUTO_STRAFE, MAX_AUTO_STRAFE);
            }

            aprilTagTime.reset();

            while (aprilTagTime.seconds() <= 1) {
                moveRobot(forward,strafe,turn);
            }

            telemetry.addData("time",aprilTagTime);


        }


    }
}







