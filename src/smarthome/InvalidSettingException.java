package smarthome;

/** Unchecked exception: a setting value is out of range or unknown. */
public class InvalidSettingException extends RuntimeException {
    public InvalidSettingException(String message) {
        super(message);
    }
}
