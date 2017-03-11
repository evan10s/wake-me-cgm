package at.str.evan.wakemecgm;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static android.content.Context.MODE_PRIVATE;

public class BGReading extends RealmObject {
    private String TAG = "wake-me-cgm/bgreading";
    public Date getTimestamp() {
        return timestamp;
    }

    private void daysBetween(Date d1, Date d2) {

    }

    public void setTimestamp(Date timestamp,long startDate) {
        //calendar stuff from StackOverflow - http://stackoverflow.com/a/907207/5434744
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(timestamp);   // assigns calendar to given date
        float hours = calendar.get(Calendar.HOUR_OF_DAY);
        DateTime start = new DateTime(startDate);
        DateTime end = new DateTime();
        int diff = Days.daysBetween(start,end).getDays();
        double mins = calendar.get(Calendar.MINUTE)/60.0;
        double percentOfDay = (hours*60 + mins)/3600.0;
        float floatPercentOfDay = (float) percentOfDay;
        float timeDec = diff + floatPercentOfDay;
        Log.i(TAG, "calendar hours:" + calendar.get(Calendar.HOUR_OF_DAY) + "| min: " + calendar.get(Calendar.MINUTE) + " | diff: " + diff);
        Log.i(TAG, "setTimestamp:timedec: " + timeDec);
        this.timestamp = timestamp;
        this.timeDecimal = timeDec;
        //this.timeInSecs = (float) timestamp.getTime();
    }

    public float getTimeDecimal() { return timeDecimal; }

    public float getReading() {
        return reading;
    }

    public void setReading(float reading) {
        this.reading = reading;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

  /*  public float getTimeInSecs() {
        return timeInSecs;
    }*/

/*    private float timeInSecs;*/
    private float timeDecimal;
    private Date timestamp;
    private float reading;
    private String trend;
}
