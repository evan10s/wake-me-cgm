package at.str.evan.wakemecgm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.realm.implementation.RealmScatterDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "WAKEMECGM";
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(new NotificationChannel("HIGH_ALERTS", "High BG Alerts", NotificationManager.IMPORTANCE_HIGH));
                notificationManager.createNotificationChannel(new NotificationChannel("LOW_ALERTS", "Low BG Alerts", NotificationManager.IMPORTANCE_HIGH));
            }
        }

        final Button confirmAlertBtn = findViewById(R.id.confirm_alert);
        confirmAlertBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                if (BgUpdateService.runningAlert != null) {
                    BgUpdateService.runningAlert.interrupt();
                }
            }
        });

        Realm realm = Realm.getDefaultInstance();

        final RealmResults<BGReading> last24Hr = realm.where(BGReading.class).greaterThan("timestamp", new Date(System.currentTimeMillis() - 86400 * 1000)).findAll();
        final ScatterChart chart = findViewById(R.id.chart);
        RealmScatterDataSet<BGReading> dataSet = new RealmScatterDataSet<>(last24Hr, "timeDecimal", "reading");
        dataSet.setColor(Color.BLACK);
        dataSet.setValueTextColor(Color.BLUE);
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
        rightAxis.setAxisMaximum(400);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1);
        IAxisValueFormatter xAxisFormatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int hours = (int) value;
                float mins = value - hours;
                int newHours = hours;
                Log.i(TAG, "getFormattedValue: " + value);
                Log.i(TAG, "getFormattedValue: " + mins);
                int minsInt = Math.round(mins * 60);
                String AMPM = "AM";
                if (hours > 12) {
                    newHours = hours - 12;
                    AMPM = "PM";
                } else if (hours == 0) {
                    newHours = 12;
                    AMPM = "AM";
                } else if (hours == 12) {
                    newHours = 12;
                    AMPM = "PM";
                }

                String minsStr = (minsInt < 10) ? "0" + minsInt : "" + minsInt;

                return newHours + ":" + minsStr + " " + AMPM;
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
        Log.d("WAKEMECGM", "stored prev BG: " + lastBg);

        if (startDate == -1) {
            SharedPreferences.Editor editor2 = sharedPref.edit();
            editor2.putLong("startDate",new Date().getTime());
            editor2.apply();
        }

        RealmChangeListener<Realm> realmListener = new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm realm) {
                BGReading lastBGObj = realm.where(BGReading.class).sort("timestamp").findAll().last();
                if (lastBGObj != null) {
                    int bg = (int) lastBGObj.getReading();
                    String trend = lastBGObj.getTrend();
                    Log.i(TAG, "onChange: BG reading is " + bg);
                    Log.i(TAG, "onChange: Trend is " + trend);

                    updateBGTextView(bg, trend);

                    chart.notifyDataSetChanged();
                    chart.invalidate();
                }
            }
        };
        realm.addChangeListener(realmListener);

        updateBGTextView(lastBg, lastTrend);


    }

    private void updateBGTextView(int bg, String trend) {
        //Sensor stopped, latest_bg=1
        //Sensor warm-up, latest_bg=5
        TextView lastBgTextView = findViewById(R.id.bg);
        if (bg == 1) {
            lastBgTextView.setText(R.string.sensor_stopped);
        } else if (bg == 5) {
            lastBgTextView.setText(R.string.sensor_warmup);
        } else if (bg == 39) {
            lastBgTextView.setText(R.string.LOW);
        } else if (bg >= 40 && bg <= 401) {
            lastBgTextView.setText(String.format(Locale.getDefault(), "%d %s", bg, trend));
        } else {
            lastBgTextView.setText("---");
        }
    }
}
