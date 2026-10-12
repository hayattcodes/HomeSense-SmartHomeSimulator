package smarthome;

/** INHERITANCE: an intermediate class for devices that support a daily schedule. */
public abstract class SchedulableDevice extends Device implements Schedulable {
    private int onHour = -1;
    private int offHour = -1;

    protected SchedulableDevice(String name, String room) {
        super(name, room);
    }

    @Override
    public void setSchedule(int onHour, int offHour) {
        if (onHour < 0 || onHour > 23 || offHour < 0 || offHour > 23)
            throw new InvalidSettingException("Hours must be between 0 and 23");
        if (onHour == offHour)
            throw new InvalidSettingException("ON and OFF hours cannot be the same");
        this.onHour = onHour;
        this.offHour = offHour;
    }

    @Override public void clearSchedule() { onHour = -1; offHour = -1; }
    @Override public boolean hasSchedule() { return onHour >= 0; }
    @Override public int getOnHour() { return onHour; }
    @Override public int getOffHour() { return offHour; }

    protected String scheduleText() {
        return hasSchedule() ? String.format(" | sched %02d:00-%02d:00", onHour, offHour) : "";
    }
}
