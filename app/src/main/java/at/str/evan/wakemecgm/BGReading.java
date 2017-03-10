package at.str.evan.wakemecgm;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class BGReading extends RealmObject {
    private String TAG = "wake-me-cgm/bgreading";
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        //calendar stuff from StackOverflow - http://stackoverflow.com/a/907207/5434744
        Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
        calendar.setTime(timestamp);   // assigns calendar to given date
        float timeDec = calendar.get(Calendar.HOUR_OF_DAY);
        timeDec += calendar.get(Calendar.MINUTE)/60.0;
        Log.i(TAG, "calendar hours:" + calendar.get(Calendar.HOUR_OF_DAY) + "| min: " + calendar.get(Calendar.MINUTE));
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
