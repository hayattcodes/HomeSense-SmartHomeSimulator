package smarthome;

import java.util.LinkedHashMap;
import java.util.Map;

public class AC extends SchedulableDevice {
    public enum Mode { COOL, DRY, FAN }

    private int targetTemp = 24;
    private Mode mode = Mode.COOL;

    public AC(String name, String room) { super(name, room); }

    public void setTargetTemp(int t) {
        if (t < 16 || t > 30) throw new InvalidSettingException("AC temperature must be between 16 and 30");
        this.targetTemp = t;
    }

    public int getTargetTemp() { return targetTemp; }

    @Override public String getType() { return "AC"; }

    /** Lower target temperature = harder compressor = more power. */
    @Override
    public double getCurrentPower() {
        return switch (mode) {
            case COOL -> Math.max(700, 1400 + (24 - targetTemp) * 80);
            case DRY -> 900;
            case FAN -> 300;
        };
    }

    @Override
    public String getStatus() {
        return mode + " @ " + targetTemp + "C" + scheduleText();
    }

    @Override
    public void configure(String setting, String value) {
        switch (setting.trim().toLowerCase()) {
            case "temp" -> setTargetTemp(parseInt("Temperature", value, 16, 30));
            case "mode" -> {
                try { mode = Mode.valueOf(value.trim().toUpperCase()); }
                catch (IllegalArgumentException e) { throw new InvalidSettingException("Mode must be COOL, DRY or FAN"); }
            }
            default -> throw new InvalidSettingException("Unknown AC setting: " + setting);
        }
    }

    @Override public String getSettingsHelp() { return "temp (16-30), mode (COOL/DRY/FAN)"; }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("temp", String.valueOf(targetTemp));
        m.put("mode", mode.name());
        return m;
    }
}
