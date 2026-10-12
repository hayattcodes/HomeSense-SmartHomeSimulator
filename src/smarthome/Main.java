package smarthome;

import java.util.Scanner;

/** Console menu for the Smart Home Simulator. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final Controller home = new Controller();

    public static void main(String[] args) {
        loadDemoHome();
        System.out.println("=== SMART HOME SIMULATOR ===");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = ask("Choose").trim();
            try {
                switch (choice) {
                    case "1" -> listDevices();
                    case "2" -> addDevice();
                    case "3" -> controlDevice();
                    case "4" -> controlRoom();
                    case "5" -> schedule();
                    case "6" -> runScene();
                    case "7" -> advance();
                    case "8" -> System.out.println(home.generateReport().render());
                    case "9" -> showLog();
                    case "10" -> motion();
                    case "11" -> removeDevice();
                    case "12" -> settings();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (DeviceNotFoundException | InvalidSettingException e) {
                System.out.println("!! " + e.getMessage());
            }
        }
        System.out.println("Final report:\n" + home.generateReport().render());
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.printf("%n--- Day %d, %s | Load now: %.0fW / %.0fW ---%n",
                home.getDay(), home.getClock(), home.totalWattsNow(), home.getPowerLimitWatts());
        System.out.println(" 1. List devices          7. Advance time");
        System.out.println(" 2. Add device            8. Energy report");
        System.out.println(" 3. Control a device      9. Activity log");
        System.out.println(" 4. Control a room       10. Simulate camera motion");
        System.out.println(" 5. Set/clear schedule   11. Remove device");
        System.out.println(" 6. Run a scene          12. Rate / load limit");
        System.out.println(" 0. Exit");
    }

    private static String ask(String prompt) {
        System.out.print(prompt + ": ");
        return in.hasNextLine() ? in.nextLine() : "0";
    }

    private static int askInt(String prompt) {
        try {
            return Integer.parseInt(ask(prompt).trim());
        } catch (NumberFormatException e) {
            throw new InvalidSettingException("Please enter a whole number");
        }
    }

    private static void listDevices() {
        if (home.getDevices().isEmpty()) { System.out.println("No devices yet."); return; }
        for (String room : home.getRooms()) {
            System.out.println("# " + room);
            for (Device d : home.getByRoom(room)) System.out.println("  " + d);
        }
    }

    private static void addDevice() {
        System.out.println("Type: 1) Light  2) Fan  3) AC  4) Camera");
        int t = askInt("Type");
        String name = ask("Name");
        String room = ask("Room");
        Device d = switch (t) {
            case 1 -> new Light(name, room);
            case 2 -> new Fan(name, room);
            case 3 -> new AC(name, room);
            case 4 -> new Camera(name, room);
            default -> throw new InvalidSettingException("Unknown device type");
        };
        home.addDevice(d);
        System.out.println("Added: " + d);
    }

    private static void controlDevice() throws DeviceNotFoundException {
        listDevices();
        int id = askInt("Device id");
        Device d = home.getDevice(id);
        System.out.println("1) Turn ON  2) Turn OFF  3) Change a setting (" + d.getSettingsHelp() + ")");
        switch (askInt("Action")) {
            case 1 -> home.switchDevice(id, true);
            case 2 -> home.switchDevice(id, false);
            case 3 -> home.configure(id, ask("Setting"), ask("Value"));
            default -> throw new InvalidSettingException("Unknown action");
        }
        System.out.println("OK: " + home.getDevice(id));
    }

    private static void controlRoom() throws DeviceNotFoundException {
        System.out.println("Rooms: " + home.getRooms());
        String room = ask("Room");
        boolean on = ask("ON or OFF").trim().equalsIgnoreCase("on");
        int n = home.switchRoom(room, on);
        System.out.println(n + " device(s) updated.");
    }

    private static void schedule() throws DeviceNotFoundException {
        listDevices();
        int id = askInt("Device id");
        if (ask("Set (s) or clear (c)").trim().equalsIgnoreCase("c")) {
            home.clearSchedule(id);
        } else {
            home.setSchedule(id, askInt("ON hour (0-23)"), askInt("OFF hour (0-23)"));
        }
        System.out.println("OK: " + home.getDevice(id));
    }

    private static void runScene() throws DeviceNotFoundException {
        home.getScenes().forEach((k, s) -> System.out.println("  " + k + " -> " + s.getName() + ": " + s.getDescription()));
        home.runScene(ask("Scene key"));
        System.out.println("Scene applied.");
    }

    private static void advance() {
        home.advanceTime(askInt("Hours to advance (1-72)"));
        System.out.println("Clock: Day " + home.getDay() + " " + home.getClock());
    }

    private static void showLog() {
        home.getLog().forEach(System.out::println);
    }

    private static void motion() throws DeviceNotFoundException {
        home.forEachOfType(Camera.class, c -> System.out.println("  " + c));
        System.out.println(home.simulateMotion(askInt("Camera id")));
    }

    private static void removeDevice() throws DeviceNotFoundException {
        listDevices();
        Device d = home.removeDevice(askInt("Device id"));
        System.out.println("Removed " + d.getName());
    }

    private static void settings() {
        home.setRatePerKwh(Double.parseDouble(ask("New rate per kWh (Rs)")));
        home.setPowerLimitWatts(Double.parseDouble(ask("New load limit (W)")));
        System.out.println("Updated.");
    }

    private static void loadDemoHome() {
        Light l1 = new Light("Ceiling", "Living Room");
        Fan f1 = new Fan("Fan", "Living Room");
        AC a1 = new AC("Split AC", "Bedroom");
        Light l2 = new Light("Lamp", "Bedroom");
        Camera c1 = new Camera("Door Cam", "Hall");
        for (Device d : new Device[]{l1, f1, a1, l2, c1}) home.addDevice(d);
    }
}
