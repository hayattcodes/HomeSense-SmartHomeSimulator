package smarthome;

import java.util.LinkedHashMap;
import java.util.Map;

public class Fan extends SchedulableDevice {
    private int speed = 3;
    private boolean oscillating = false;

    public Fan(String name, String room) { super(name, room); }

    public void setSpeed(int s) {
        if (s < 1 || s > 5) throw new InvalidSettingException("Fan speed must be between 1 and 5");
        this.speed = s;
    }

    public int getSpeed() { return speed; }

    @Override public String getType() { return "Fan"; }

    @Override public double getCurrentPower() { return 15.0 * speed; }   // 15W..75W

    @Override
    public String getStatus() {
        return "speed " + speed + (oscillating ? ", oscillating" : "") + scheduleText();
    }

    @Override
    public void configure(String setting, String value) {
        switch (setting.trim().toLowerCase()) {
            case "speed" -> setSpeed(parseInt("Speed", value, 1, 5));
            case "oscillate" -> oscillating = value.trim().equalsIgnoreCase("on");
            default -> throw new InvalidSettingException("Unknown fan setting: " + setting);
        }
    }

    @Override public String getSettingsHelp() { return "speed (1-5), oscillate (on/off)"; }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("speed", String.valueOf(speed));
        m.put("oscillate", oscillating ? "on" : "off");
        return m;
    }
}
