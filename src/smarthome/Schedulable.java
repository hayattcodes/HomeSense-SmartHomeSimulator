package smarthome;

/** Devices that can run on a daily timetable (hours are 0-23). */
public interface Schedulable {
    void setSchedule(int onHour, int offHour);
    void clearSchedule();
    boolean hasSchedule();
    int getOnHour();
    int getOffHour();
}
