package smarthome;

/** Anything that can be switched on or off. */
public interface Switchable {
    void turnOn();
    void turnOff();
    boolean isOn();
}
