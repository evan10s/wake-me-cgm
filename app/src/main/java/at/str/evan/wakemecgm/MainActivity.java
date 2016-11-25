package at.str.evan.wakemecgm;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseInstanceId.getInstance().getToken();
        /*final Button startServiceBtn = (Button) findViewById(R.id.start_service);
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                Log.d("WAKEMECGM",FirebaseInstanceId.getInstance().getToken());
                final EditText ipAdrField = (EditText) findViewById(R.id.enter_ip_adr);
                String ipAdr = ipAdrField.getText().toString(); //trim is not necessary because input doesn't allow spaces
                makeNetworkRequest(ipAdr);
            }
        });*/
        final Button confirmAlertBtn = (Button) findViewById(R.id.confirm_alert);
        confirmAlertBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick (View v) {
                BgUpdateService.runningAlert.interrupt();
            }
        });

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
