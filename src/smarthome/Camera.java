package smarthome;

import java.util.LinkedHashMap;
import java.util.Map;

/** Not Schedulable: a security camera is meant to stay on 24/7. */
public class Camera extends Device {
    private boolean recording = false;
    private int motionEvents = 0;

    public Camera(String name, String room) {
        super(name, room);
        turnOn();                       // cameras start ON
    }

    /** Called by the simulation when motion is "seen". Returns false if the camera is off. */
    public boolean detectMotion() {
        if (!isOn()) return false;
        motionEvents++;
        recording = true;
        return true;
    }

    public void stopRecording() { recording = false; }
    public int getMotionEvents() { return motionEvents; }

    @Override public String getType() { return "Camera"; }

    @Override public double getCurrentPower() { return recording ? 12.0 : 5.0; }

    @Override
    public String getStatus() {
        return (recording ? "RECORDING" : "idle") + ", motion events: " + motionEvents;
    }

    @Override
    public void configure(String setting, String value) {
        if (setting.trim().equalsIgnoreCase("recording")) {
            recording = value.trim().equalsIgnoreCase("on");
        } else {
            throw new InvalidSettingException("Unknown camera setting: " + setting);
        }
    }

    @Override public String getSettingsHelp() { return "recording (on/off)"; }

    @Override
    public void turnOff() {
        super.turnOff();
        recording = false;
    }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("recording", recording ? "on" : "off");
        m.put("motionEvents", String.valueOf(motionEvents));
        return m;
    }
}
