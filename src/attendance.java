import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class attendance {
    private String id, date, clockIn, clockOut;
    private double totalHour;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("hh:mm a");

    public attendance(String id, String date, String clockIn) {
        this.id = id;
        this.date = date;
        this.clockIn = clockIn;
    }

    public void clockOut(String time) {
        clockOut = time;
        try {
            LocalTime start = LocalTime.parse(clockIn.toLowerCase().replace(".", ""), dtf);
            LocalTime end = LocalTime.parse(time.toLowerCase().replace(".", ""), dtf);
            totalHour = Duration.between(start, end).toMinutes() / 60.0;
        } catch (Exception e) {
            totalHour = 0.0;
        }
    }

    public String getID() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getClockIn() {
        return clockIn;
    }

    public String getClockOut() {
        return clockOut;
    }

    public double getTotalHour() {
        return totalHour;
    }
}
