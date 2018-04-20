package at.str.evan.wakemecgm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.realm.implementation.RealmScatterDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

class BG {
    public float time;
    public int bg;
    public BG(float time, int bg) {
        this.time = time;
        this.bg = bg;

    }
}

public class MainActivity extends AppCompatActivity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();

        final Button confirmAlertBtn = findViewById(R.id.confirm_alert);
        confirmAlertBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                if (BgUpdateService.runningAlert != null) {
                    BgUpdateService.runningAlert.interrupt();
                }
            }
        });
        SharedPreferences sharedPref1 = context.getSharedPreferences(
                "com.at.str.evan.wakemecgm.BG_DATA", Context.MODE_PRIVATE);

        Realm realm = Realm.getDefaultInstance();
        //realm.beginTransaction();
        //BGReading bgReading = realm.createObject(BGReading.class); // Create a new object
        //long startDate1 = sharedPref1.getLong("startDate",-1);
        //bgReading.setTimestamp(new Date(),startDate1); //TODO: use the date passed in the object
        //bgReading.setReading(106);
        //bgReading.setTrend("→");
        //realm.commitTransaction();

        Log.i("WAKEMECGM", "made it here");
        RealmResults<BGReading> last24Hr = realm.where(BGReading.class).greaterThan("timestamp",new Date(System.currentTimeMillis() - 86400*1000)).findAll();
        //Log.i("WAKEMECGM",last24Hr.toString());
        ScatterChart chart = (ScatterChart) findViewById(R.id.chart);
        Log.i("WAKEMECGM", "made it here 2");

        RealmScatterDataSet<BGReading> dataSet = new RealmScatterDataSet<BGReading>(last24Hr,"timeDecimal","reading");
        dataSet.setColor(Color.BLACK);
        dataSet.setValueTextColor(Color.BLUE); // styling, ...
        dataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        ScatterData lineData = new ScatterData(dataSet);
        chart.setData(lineData);
        chart.setScaleYEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getLegend().setForm(Legend.LegendForm.CIRCLE);

        chart.getDescription().setEnabled(false);

        chart.setVisibleXRangeMinimum(1);
        chart.setVisibleXRangeMaximum(3);
        chart.setVisibleXRangeMaximum(24);
        lineData.setDrawValues(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinimum(40);
        rightAxis.setAxisMaximum(250);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        IAxisValueFormatter xAxisFormatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int hours = (int) value;
                int newHours = hours;
                int minutes;
                String AMPM = "AM";
                if (hours > 12 && hours > 0) {
                    newHours = hours - 12;
                    AMPM = "PM";
                } else if (hours == 0) {
                    newHours = 12;
                    AMPM = "AM";
                } else if (hours == 12) {
                    newHours = 12;
                    AMPM = "PM";
                }
                minutes = Math.round((value - hours)*60);


                String minuteString = (minutes < 10) ? ("0" + minutes) : "" + minutes;
                Log.i("wake-me-cgm", "getFormattedValue: value:" + value + " " + hours + " " + minutes);
                Log.i("wake-me-cgm", "getFormattedValue: value:" + value);
                return newHours /*+ ":" + minutes + " "*/+ " " + AMPM;
            }
        };
        xAxis.setValueFormatter(xAxisFormatter);
        LimitLine low = new LimitLine(65, "");
        low.setLineColor(Color.RED);
        low.setLineWidth(1f);
        low.setTextSize(12f);

        LimitLine high = new LimitLine(240, "");
        high.setLineColor(Color.rgb(255,171,0));
        high.setLineWidth(1f);
        high.setTextSize(12f);



        rightAxis.addLimitLine(low);
        rightAxis.addLimitLine(high);

        chart.invalidate(); // refresh

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("com.at.str.evan.wakemecgm.BG_DATA",Context.MODE_PRIVATE);
        int lastBg = sharedPref.getInt("lastBg", -1);
        String lastTrend = sharedPref.getString("lastTrend","");
        long startDate = sharedPref.getLong("startDate",-1);
        Log.d("WAKEMECGM", "stored prev BG: " + lastBg);;

        if (startDate == -1) {
            SharedPreferences.Editor editor2 = sharedPref.edit();
            editor2.putLong("startDate",new Date().getTime());
            editor2.apply();
        }


        final TextView lastBgTextView = (TextView) findViewById(R.id.bg);
        //Sensor stopped, latest_bg=1
        //Sensor warm-up, latest_bg=5
        if (lastBg == 1) {
            lastBgTextView.setText("No data: sensor stopped.");
        } else if (lastBg == 5) {
            lastBgTextView.setText("No data: sensor warm-up.");
        } else if (lastBg == 39) {
            lastBgTextView.setText("LOW");
        } else if (lastBg >= 40 && lastBg <= 401) {
            lastBgTextView.setText(lastBg + lastTrend);
        } else {
            lastBgTextView.setText("Unhandled edge case: " + lastBg + " " + lastTrend);
        }
    }
}
