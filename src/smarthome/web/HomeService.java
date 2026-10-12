package smarthome.web;

import smarthome.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Gives every user their own private Controller (their own house). */
public class HomeService {
    private final Map<String, Controller> homes = new ConcurrentHashMap<>();

    public Controller homeFor(String username) {
        return homes.computeIfAbsent(username.toLowerCase(), k -> createSampleHome());
    }

    private Controller createSampleHome() {
        Controller c = new Controller();
        Light ceiling = new Light("Ceiling Light", "Living Room");
        Fan fan = new Fan("Ceiling Fan", "Living Room");
        AC ac = new AC("Split AC", "Bedroom");
        Light lamp = new Light("Bedside Lamp", "Bedroom");
        Light pendant = new Light("Pendant Light", "Kitchen");
        Camera door = new Camera("Door Camera", "Entrance");
        Camera garden = new Camera("Garden Camera", "Garden");

        for (Device d : new Device[]{ceiling, fan, ac, lamp, pendant, door, garden}) c.addDevice(d);

        ceiling.setBrightness(80);
        ceiling.turnOn();
        fan.turnOn();
        pendant.turnOn();
        ac.setSchedule(22, 6);
        c.advanceTime(6);           // so the dashboard has some data to show
        return c;
    }
}
