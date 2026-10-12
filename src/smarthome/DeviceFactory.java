package smarthome;

/** FACTORY PATTERN: creates the right Device subclass from a type name. */
public final class DeviceFactory {
    private DeviceFactory() { }

    public static Device create(String type, String name, String room) {
        if (type == null) throw new InvalidSettingException("Device type is required");
        return switch (type.trim().toLowerCase()) {
            case "light" -> new Light(name, room);
            case "fan" -> new Fan(name, room);
            case "ac" -> new AC(name, room);
            case "camera" -> new Camera(name, room);
            default -> throw new InvalidSettingException("Unknown device type: " + type);
        };
    }
}
