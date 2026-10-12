package smarthome;

/** Checked exception: a device id or name does not exist. */
public class DeviceNotFoundException extends Exception {
    public DeviceNotFoundException(String message) {
        super(message);
    }
}
