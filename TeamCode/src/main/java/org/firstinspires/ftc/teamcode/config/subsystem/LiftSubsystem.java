package org.firstinspires.ftc.teamcode.config.subsystem;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class LiftSubsystem {
    private DcMotorEx lift;

    public LiftSubsystem(HardwareMap hardwareMap) {
        lift = hardwareMap.get(DcMotorEx.class, "gear");
        lift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        lift.setDirection(DcMotorSimple.Direction.FORWARD);
    }

    //------------------------------ Lift Extend ------------------------------//
    public Action liftExtend_Scoring() {
        return new Action() {
            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                lift.setTargetPosition(-600);
                lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                lift.setPower(0.7);
                return false;
            }
        };
    }

    public Action liftExtend_Stack() {
        return new Action() {
            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                lift.setTargetPosition(-375); //425 --> 375
                lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                lift.setPower(0.5);
                return false;
            }
        };
    }

    //------------------------------ Lift Retract ------------------------------//
    public Action liftRetract_Scoring() {
        return new Action() {
            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                lift.setTargetPosition(600);
                lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                lift.setPower(0.7);
                return false;
            }
        };
    }

    public Action liftRetract_Stack() {
        return new Action() {
            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                lift.setTargetPosition(375); //425 --> 375
                lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                lift.setPower(0.6);
                return false;
            }
        };
    }

    //------------------------------ Wait for Lift -------------------------------//
    public Action waitForLift() {
        return new Action() {
            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                return lift.isBusy();
            }
        };
    }

    //------------------------------ Stop Lift -------------------------------//
    public Action stopLift() {
        return new Action() {

            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                lift.setPower(0);
                return false;
            }
        };
    }

    public Action resetLift() {
        return new Action() {

            @Override
            public boolean run(@NonNull TelemetryPacket telemetryPacket) {
                lift.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                return false;
            }
        };
    }


}

