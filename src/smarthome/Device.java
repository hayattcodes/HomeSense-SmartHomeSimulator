package smarthome;

import java.util.Map;

/**
 * ABSTRACTION + ENCAPSULATION: base class for every smart device.
 * Subclasses must say how much power they draw and how to change their settings.
 */
public abstract class Device implements Switchable {
    private static int idCounter = 1;

    private final int id;
    private String name;
    private String room;
    private boolean on;
    private double energyKwh;      // lifetime usage
    private double hoursOn;        // lifetime run time

    protected Device(String name, String room) {
        if (name == null || name.isBlank()) throw new InvalidSettingException("Device name cannot be empty");
        if (room == null || room.isBlank()) throw new InvalidSettingException("Room cannot be empty");
        this.id = idCounter++;
        this.name = name.trim();
        this.room = room.trim();
    }

    // ---- abstract behaviour (each subclass decides) ----
    public abstract String getType();
    public abstract double getCurrentPower();            // watts right now
    public abstract String getStatus();                  // human readable details
    public abstract void configure(String setting, String value);
    public abstract String getSettingsHelp();
    /** Current settings as name/value pairs (used by the web UI). */
    public abstract Map<String, String> getSettings();

    // ---- Switchable ----
    @Override public void turnOn()  { on = true; }
    @Override public void turnOff() { on = false; }
    @Override public boolean isOn() { return on; }

    /** Simulates the passage of time; called by the Controller. */
    public void tick(double hours) {
        if (on) {
            energyKwh += getCurrentPower() * hours / 1000.0;
            hoursOn += hours;
        }
    }

    // ---- getters / validated setters ----
    public int getId() { return id; }
    public String getName() { return name; }
    public String getRoom() { return room; }
    public double getEnergyKwh() { return energyKwh; }
    public double getHoursOn() { return hoursOn; }
    public double getWattsNow() { return on ? getCurrentPower() : 0; }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new InvalidSettingException("Name cannot be empty");
        this.name = name.trim();
    }

    public void setRoom(String room) {
        if (room == null || room.isBlank()) throw new InvalidSettingException("Room cannot be empty");
        this.room = room.trim();
    }

    /** Helper for subclasses: parse and range-check an integer setting. */
    protected int parseInt(String label, String value, int min, int max) {
        int n;
        try {
            n = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidSettingException(label + " must be a whole number");
        }
        if (n < min || n > max)
            throw new InvalidSettingException(label + " must be between " + min + " and " + max);
        return n;
    }

    @Override
    public String toString() {
        return String.format("[%d] %-12s %-7s %-12s %-3s %6.0fW | %s",
                id, name, getType(), room, on ? "ON" : "OFF", getWattsNow(), getStatus());
    }
}
