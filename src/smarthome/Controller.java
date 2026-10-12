package smarthome;

import java.util.*;
import java.util.function.Consumer;

/** The brain of the house: owns devices, the simulated clock, scenes and the activity log. */
public class Controller {
    private final Map<Integer, Device> devices = new LinkedHashMap<>();
    private final Map<String, Scene> scenes = new LinkedHashMap<>();
    private final List<String> log = new ArrayList<>();

    private int day = 1;
    private int hour = 8;                       // simulated clock starts at 08:00
    private double ratePerKwh = 8.0;
    private double powerLimitWatts = 3000;      // overload threshold

    public Controller() {
        registerDefaultScenes();
        log("System started");
    }

    // ---------- device management ----------
    public void addDevice(Device d) {
        devices.put(d.getId(), d);
        log("Added " + d.getType() + " '" + d.getName() + "' in " + d.getRoom());
    }

    public Device removeDevice(int id) throws DeviceNotFoundException {
        Device d = getDevice(id);
        devices.remove(id);
        log("Removed '" + d.getName() + "'");
        return d;
    }

    public Device getDevice(int id) throws DeviceNotFoundException {
        Device d = devices.get(id);
        if (d == null) throw new DeviceNotFoundException("No device with id " + id);
        return d;
    }

    public Collection<Device> getDevices() { return devices.values(); }

    public List<Device> getByRoom(String room) {
        List<Device> result = new ArrayList<>();
        for (Device d : devices.values())
            if (d.getRoom().equalsIgnoreCase(room.trim())) result.add(d);
        return result;
    }

    public Set<String> getRooms() {
        Set<String> rooms = new TreeSet<>();
        for (Device d : devices.values()) rooms.add(d.getRoom());
        return rooms;
    }

    /** Generic helper: run an action on every device of a given class (polymorphism + generics). */
    public <T extends Device> void forEachOfType(Class<T> type, Consumer<T> action) {
        for (Device d : devices.values())
            if (type.isInstance(d)) action.accept(type.cast(d));
    }

    // ---------- commands ----------
    public void switchDevice(int id, boolean on) throws DeviceNotFoundException {
        Device d = getDevice(id);
        if (on) d.turnOn(); else d.turnOff();
        log("'" + d.getName() + "' turned " + (on ? "ON" : "OFF"));
    }

    public int switchRoom(String room, boolean on) throws DeviceNotFoundException {
        List<Device> list = getByRoom(room);
        if (list.isEmpty()) throw new DeviceNotFoundException("No devices in room '" + room + "'");
        for (Device d : list) { if (on) d.turnOn(); else d.turnOff(); }
        log("Room '" + room + "' -> all " + (on ? "ON" : "OFF") + " (" + list.size() + " devices)");
        return list.size();
    }

    public void configure(int id, String setting, String value) throws DeviceNotFoundException {
        Device d = getDevice(id);
        d.configure(setting, value);
        log("'" + d.getName() + "' " + setting + " = " + value);
    }

    public void setSchedule(int id, int onHour, int offHour) throws DeviceNotFoundException {
        Device d = getDevice(id);
        if (!(d instanceof Schedulable s))
            throw new InvalidSettingException(d.getType() + " does not support schedules");
        s.setSchedule(onHour, offHour);
        log("'" + d.getName() + "' scheduled " + String.format("%02d:00-%02d:00", onHour, offHour));
    }

    public void clearSchedule(int id) throws DeviceNotFoundException {
        Device d = getDevice(id);
        if (!(d instanceof Schedulable s))
            throw new InvalidSettingException(d.getType() + " does not support schedules");
        s.clearSchedule();
        log("'" + d.getName() + "' schedule cleared");
    }

    public String simulateMotion(int id) throws DeviceNotFoundException {
        Device d = getDevice(id);
        if (!(d instanceof Camera cam))
            throw new InvalidSettingException("'" + d.getName() + "' is not a camera");
        if (cam.detectMotion()) {
            String msg = "ALERT: motion detected by '" + cam.getName() + "' in " + cam.getRoom();
            log(msg);
            return msg;
        }
        return "Camera is OFF, motion was not recorded";
    }

    // ---------- time simulation ----------
    public void advanceTime(int hours) {
        if (hours < 1 || hours > 72) throw new InvalidSettingException("Hours must be between 1 and 72");
        for (int i = 0; i < hours; i++) {
            applySchedules();
            for (Device d : devices.values()) d.tick(1.0);
            checkOverload();
            hour++;
            if (hour == 24) { hour = 0; day++; }
        }
        log("Advanced " + hours + "h, clock is now " + getClock());
    }

    private void applySchedules() {
        forEachOfType(SchedulableDevice.class, s -> {
            if (!s.hasSchedule()) return;
            if (s.getOnHour() == hour && !s.isOn()) {
                s.turnOn();
                log("[Schedule] '" + s.getName() + "' switched ON");
            } else if (s.getOffHour() == hour && s.isOn()) {
                s.turnOff();
                log("[Schedule] '" + s.getName() + "' switched OFF");
            }
        });
    }

    private void checkOverload() {
        double total = totalWattsNow();
        if (total > powerLimitWatts)
            log(String.format("WARNING: load %.0fW exceeds limit %.0fW at %s", total, powerLimitWatts, getClock()));
    }

    public double totalWattsNow() {
        double sum = 0;
        for (Device d : devices.values()) sum += d.getWattsNow();
        return sum;
    }

    // ---------- scenes ----------
    private void registerDefaultScenes() {
        scenes.put("night", new Scene("Night Mode", "Lights off, ACs on at 24C, cameras recording")
                .addAction(c -> c.forEachOfType(Light.class, Light::turnOff))
                .addAction(c -> c.forEachOfType(AC.class, a -> { a.setTargetTemp(24); a.turnOn(); }))
                .addAction(c -> c.forEachOfType(Camera.class, cam -> { cam.turnOn(); cam.configure("recording", "on"); })));

        scenes.put("away", new Scene("Away Mode", "Everything off except cameras")
                .addAction(c -> c.forEachOfType(Light.class, Light::turnOff))
                .addAction(c -> c.forEachOfType(Fan.class, Fan::turnOff))
                .addAction(c -> c.forEachOfType(AC.class, AC::turnOff))
                .addAction(c -> c.forEachOfType(Camera.class, Camera::turnOn)));

        scenes.put("movie", new Scene("Movie Mode", "Lights dimmed to 20%, AC at 22C")
                .addAction(c -> c.forEachOfType(Light.class, l -> { l.setBrightness(20); l.turnOn(); }))
                .addAction(c -> c.forEachOfType(AC.class, a -> { a.setTargetTemp(22); a.turnOn(); })));
    }

    public Map<String, Scene> getScenes() { return scenes; }

    public void runScene(String key) throws DeviceNotFoundException {
        Scene s = scenes.get(key.trim().toLowerCase());
        if (s == null) throw new DeviceNotFoundException("No scene called '" + key + "'");
        s.execute(this);
        log("Scene '" + s.getName() + "' activated");
    }

    // ---------- reporting, log, config ----------
    public EnergyReport generateReport() { return new EnergyReport(devices.values(), ratePerKwh); }

    private void log(String message) { log.add(String.format("Day %d %s | %s", day, getClock(), message)); }

    public List<String> getLog() { return Collections.unmodifiableList(log); }
    public String getClock() { return String.format("%02d:00", hour); }
    public int getDay() { return day; }
    public int getHour() { return hour; }
    public double getRatePerKwh() { return ratePerKwh; }
    public double getPowerLimitWatts() { return powerLimitWatts; }

    public void setRatePerKwh(double rate) {
        if (rate <= 0) throw new InvalidSettingException("Rate must be positive");
        this.ratePerKwh = rate;
    }

    public void setPowerLimitWatts(double watts) {
        if (watts <= 0) throw new InvalidSettingException("Limit must be positive");
        this.powerLimitWatts = watts;
    }
}
