package smarthome;

import java.util.*;

/** Builds a usage summary (per device, per room, total, cost) from a list of devices. */
public class EnergyReport {
    private final List<Device> devices;
    private final double ratePerKwh;

    public EnergyReport(Collection<Device> devices, double ratePerKwh) {
        this.devices = new ArrayList<>(devices);
        this.ratePerKwh = ratePerKwh;
    }

    public List<Device> getDevices() { return Collections.unmodifiableList(devices); }
    public double getRatePerKwh() { return ratePerKwh; }

    public double totalKwh() {
        double sum = 0;
        for (Device d : devices) sum += d.getEnergyKwh();
        return sum;
    }

    public double totalCost() { return totalKwh() * ratePerKwh; }

    public Map<String, Double> byRoom() {
        Map<String, Double> map = new TreeMap<>();
        for (Device d : devices) map.merge(d.getRoom(), d.getEnergyKwh(), Double::sum);
        return map;
    }

    public Map<String, Double> byType() {
        Map<String, Double> map = new TreeMap<>();
        for (Device d : devices) map.merge(d.getType(), d.getEnergyKwh(), Double::sum);
        return map;
    }

    public Optional<Device> topConsumer() {
        return devices.stream().max(Comparator.comparingDouble(Device::getEnergyKwh))
                .filter(d -> d.getEnergyKwh() > 0);
    }

    public String suggestion() {
        Optional<Device> top = topConsumer();
        if (top.isEmpty()) return "No usage recorded yet. Advance the clock and try again.";
        Device d = top.get();
        double share = d.getEnergyKwh() / totalKwh() * 100;
        String tip = "Consider reducing its run time.";
        if (d instanceof AC) tip = "Raising the AC temperature by 2C can cut its consumption noticeably.";
        else if (d instanceof Light) tip = "Lower the brightness or add an OFF schedule.";
        else if (d instanceof Fan) tip = "Use a lower speed or a schedule.";
        return String.format("%s (%s) uses %.0f%% of your energy. %s", d.getName(), d.getRoom(), share, tip);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(62);
        sb.append(line).append("\n  ENERGY USAGE REPORT\n").append(line).append("\n");
        sb.append(String.format("%-14s %-8s %-12s %8s %10s%n", "Device", "Type", "Room", "Hours", "kWh"));
        sb.append("-".repeat(62)).append("\n");
        for (Device d : devices)
            sb.append(String.format("%-14s %-8s %-12s %8.1f %10.3f%n",
                    d.getName(), d.getType(), d.getRoom(), d.getHoursOn(), d.getEnergyKwh()));
        sb.append("-".repeat(62)).append("\n");
        sb.append("By room:\n");
        byRoom().forEach((k, v) -> sb.append(String.format("  %-14s %8.3f kWh%n", k, v)));
        sb.append("By device type:\n");
        byType().forEach((k, v) -> sb.append(String.format("  %-14s %8.3f kWh%n", k, v)));
        sb.append("-".repeat(62)).append("\n");
        sb.append(String.format("TOTAL: %.3f kWh   |   Rate: Rs %.2f/kWh   |   Estimated bill: Rs %.2f%n",
                totalKwh(), ratePerKwh, totalCost()));
        sb.append("Tip: ").append(suggestion()).append("\n").append(line);
        return sb.toString();
    }
}
