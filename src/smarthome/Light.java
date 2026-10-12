package smarthome;

import java.util.LinkedHashMap;
import java.util.Map;

public class Light extends SchedulableDevice {
    public enum ColorMode { WARM, COOL, DAYLIGHT }

    private static final double MAX_WATTS = 12.0;
    private int brightness = 100;
    private ColorMode colorMode = ColorMode.WARM;

    public Light(String name, String room) { super(name, room); }

    public void setBrightness(int b) {
        if (b < 0 || b > 100) throw new InvalidSettingException("Brightness must be between 0 and 100");
        this.brightness = b;
    }

    public int getBrightness() { return brightness; }

    @Override public String getType() { return "Light"; }

    @Override public double getCurrentPower() { return MAX_WATTS * brightness / 100.0; }

    @Override
    public String getStatus() {
        return "brightness " + brightness + "%, " + colorMode + scheduleText();
    }

    @Override
    public void configure(String setting, String value) {
        switch (setting.trim().toLowerCase()) {
            case "brightness" -> setBrightness(parseInt("Brightness", value, 0, 100));
            case "color" -> {
                try { colorMode = ColorMode.valueOf(value.trim().toUpperCase()); }
                catch (IllegalArgumentException e) { throw new InvalidSettingException("Color must be WARM, COOL or DAYLIGHT"); }
            }
            default -> throw new InvalidSettingException("Unknown light setting: " + setting);
        }
    }

    @Override public String getSettingsHelp() { return "brightness (0-100), color (WARM/COOL/DAYLIGHT)"; }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("brightness", String.valueOf(brightness));
        m.put("color", colorMode.name());
        return m;
    }
}
