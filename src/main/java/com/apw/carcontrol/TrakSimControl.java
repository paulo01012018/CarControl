package com.apw.carcontrol;

import com.apw.apw3.DriverCons;
import com.apw.apw3.MyMath;
import com.apw.apw3.SimCamera;
import com.apw.fakefirm.Arduino;

import javax.swing.JFrame;

public class TrakSimControl implements CarControl {
    private SimCamera cam;
    private Arduino driveSys;

    private byte[] cameraImage = null;
    private byte[] processedImage;

    private int currentSteering = 0;
    private int currentVelocity = 0;
    private final int SteerPin, GasPin;
    private final double LefScaleSt, RitScaleSt;

    public TrakSimControl() {
        cam = new SimCamera();
        cam.Connect(4); // 30 FPS

        SteerPin = DriverCons.D_SteerServo;
        GasPin = DriverCons.D_GasServo;
        LefScaleSt = ((double) DriverCons.D_LeftSteer) / 90.0;
        RitScaleSt =  ((double) DriverCons.D_RiteSteer) / 90.0;

        driveSys = new Arduino();
        driveSys.pinMode(SteerPin, Arduino.SERVO);
        driveSys.pinMode(GasPin, Arduino.SERVO);
    }

    @Override
    public byte[] readCameraImage() {
        int nrows = cam.Dimz() >> 16;
        int ncols = cam.Dimz() << 16 >> 16;
        if(cameraImage == null || (nrows * ncols * 4) != cameraImage.length) {
            cameraImage = new byte[nrows * ncols * 4];
        }
        boolean b = cam.NextFrame(cameraImage);
        if (!b) {
            System.err.println("An error occurred in TrakSimControl while reading the camera image from SimCamera.");
        }

        processedImage = null;
        return cameraImage;
    }

    @Override
    public byte[] getRecentCameraImage() {
        return cameraImage;
    }

    @Override
    public byte[] getProcessedImage() {
        if(processedImage == null) {
            return cameraImage;
        }
        return processedImage;
    }

    @Override
    public void setProcessedImage(byte[] image) {
        this.processedImage = image;
    }


    @Override
    public void exit(int why) {
        //FlyCamera myVid = theVideo;
        try {
            accelerate(true, 0);
            steer(true, 0);
            if (cam != null) {
                cam.Finish();
            }
            if (driveSys != null) {
                driveSys.Close();
            }
        } catch (Exception ignored) {
        }
        System.out.println("-------- Clean Stop -------- " + why);
        System.exit(why);
    }

    @Override
    public void accelerate(boolean absolute, int velocity) {
        if (!absolute) {
            velocity = currentVelocity + velocity;
        }
        if (velocity != 0) {
            velocity = MyMath.iMax(MyMath.iMin(velocity, 90), -90);
            if (velocity == currentVelocity) {
                return;
            }
        }
        if (velocity == 0 && !absolute && currentVelocity == 0) {
            return;
        }

        currentVelocity = velocity;
        if (driveSys == null) {
            return;
        }
        driveSys.servoWrite(GasPin, velocity + 90);
    }

    @Override
    public void steer(boolean absolute, int angle) {
        if (!absolute) {
            angle = currentSteering + angle;
        }
        angle = MyMath.iMax(MyMath.iMin(angle, 90), -90);
        if (angle != 0) if (angle == currentSteering) return;
        currentSteering = angle;
        if (angle < 0) {
            //noinspection ConstantConditions
            if (LefScaleSt < 1.0) { // LefScaleSt = LeftSteer/90.0
                angle = (int) Math.round(LefScaleSt * ((double) angle));
            }
        } else if (angle > 0) {
            //noinspection ConstantConditions
            if (RitScaleSt > 1.0) {
                angle = (int) Math.round(RitScaleSt * ((double) angle));
            }
        }
        if (driveSys == null) {
            return;
        }
        driveSys.servoWrite(SteerPin, angle + 90);
    }
}
