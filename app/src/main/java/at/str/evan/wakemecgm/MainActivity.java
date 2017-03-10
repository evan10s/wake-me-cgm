package at.str.evan.wakemecgm;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.data.realm.implementation.RealmScatterDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String firebaseToken = FirebaseInstanceId.getInstance().getToken();
        /*final Button startServiceBtn = (Button) findViewById(R.id.start_service);
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                Log.d("WAKEMECGM",FirebaseInstanceId.getInstance().getToken());
                final EditText ipAdrField = (EditText) findViewById(R.id.enter_ip_adr);
                String ipAdr = ipAdrField.getText().toString(); //trim is not necessary because input doesn't allow spaces
                makeNetworkRequest(ipAdr);
            }
        });*/

        final TextView firebaseTokenTextView = (TextView) findViewById(R.id.firebaseToken);
        firebaseTokenTextView.setText(firebaseToken);

        final Button confirmAlertBtn = (Button) findViewById(R.id.confirm_alert);
        confirmAlertBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                try {
                    BgUpdateService.runningAlert.interrupt();
                } catch (Error e) {
                    Log.e("WAKEMECGM","Error when confirm alert button pressed",e);
                }
            }
        });
      Realm realm = Realm.getDefaultInstance();
/*        realm.beginTransaction();
        BGReading bg = realm.createObject(BGReading.class); // Create a new object
        bg.setTimestamp(new Date());
        bg.setReading(84()]);
        bg.setTrend("→");
        realm.commitTransaction();*/


        RealmResults<BGReading> last24Hr = realm.where(BGReading.class).greaterThan("timestamp",new Date(System.currentTimeMillis() - 86400*1000)).findAll();
        Log.i("WAKEMECGM",last24Hr.toString());
        ScatterChart chart = (ScatterChart) findViewById(R.id.chart);


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

                return newHours + ":" + minuteString + " " + AMPM;
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
        Log.d("WAKEMECGM", "stored prev BG: " + lastBg);
        final TextView lastBgTextView = (TextView) findViewById(R.id.bg);
        //Sensor stopped, latest_bg=1
        //Sensor warm-up, latest_bg=5
        if (lastBg == 1) {
            lastBgTextView.setText("No data: sensor stopped.");
        } else if (lastBg == 5) {
            lastBgTextView.setText("No data: sensor warm-up.");
        } else if (lastBg == 39) {
            lastBgTextView.setText("LOW (The CGM reports 39, which should correspond to LOW)");
        } else if (lastBg >= 40 && lastBg <= 401) {
            lastBgTextView.setText(lastBg + lastTrend);
        } else {
            lastBgTextView.setText("Unhandled edge case: " + lastBg + " " + lastTrend);
        }
    }

    public void makeNetworkRequest(String ip) {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            //weeeeeeeee let's do this!
            String urlPrefix = "http://";
            String path = "/glucose.json";
            String url = urlPrefix + ip + path;
            new DownloadWebpageTask().execute(url);
        } else {
            //poop.
            showToast("Check your internet connection");
        }
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                Log.e("WAKEMECGM","Unable to retrieve webpage " + urls[0]);
                return "Unable to retrieve web page.  URL may be invalid";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            processBG(Integer.parseInt(result));
        }
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        int len = 500; //max characters of webpage to show

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            //Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("WAKEMECGM","Response: " + response);
            is = conn.getInputStream();

            //Convert InputStream into a string
            String result = readIt(is,len);
            return result;
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }

    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Object result = new JSONArray(sb.toString()).getJSONObject(0).get("glucose");
            Log.d("WAKEMECGM","Lowish");
            return result.toString();


        } catch (org.json.JSONException e) {
            e.printStackTrace();
            return "JSON error occurred";
        }


        /*Reader reader = null;
        reader = new InputStreamReader(stream,"UTF-8");
        char[] buffer = new char[160]; //this should be a slight overestimation
        reader.read(buffer);
        Log.d("WAKEMECGM",new String(buffer));
        return new String(buffer);*/
    }

    private void processBG(int glucose) {
        String glucoseString = Integer.toString(glucose);
        if (glucose < 65) {
            showToast("LOW BG!  Last reported: " + glucoseString);
        } else if (glucose > 240) {
            showToast("High BG, last reported: " + glucoseString);
        } else {
            showToast("BG is OK, last reported: " + glucoseString);
        }
    }

    private void showToast(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast.makeText(context, text, duration).show();
    }
}
