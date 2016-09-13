package hyy.pathfinder;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> stationList_short = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FetchDataTask task = new FetchDataTask();
        task.execute("http://rata.digitraffic.fi/api/v1/metadata/stations.json"); // Get current list of train stations

        // AutoCompleteTextView for start and end locations
        AutoCompleteTextView actv_start = (AutoCompleteTextView) findViewById(R.id.start_field);
        AutoCompleteTextView actv_end = (AutoCompleteTextView) findViewById(R.id.end_field);
        ArrayAdapter<String> aa = new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,stationList_short);
        actv_start.setAdapter(aa);
        actv_end.setAdapter(aa);
    }

    class FetchDataTask extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            JSONArray json = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                json = new JSONArray(stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                    e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

            return json;
        }

        protected void onPostExecute(JSONArray json) {
            StringBuilder text = new StringBuilder("");
            List<String[]> stationList = new ArrayList<>();

            try {
                  for (int i=0; i < json.length(); i++) {
                    JSONObject station = json.getJSONObject(i);
                      if (station.getString("passengerTraffic") == "true") {
                          // Add data to a two-dimensional array of passenger stations in Finland, including longitude and latitude
                          stationList.add(new String[] {station.getString("stationName"), station.getString("longitude"), station.getString("latitude")});
                          stationList_short.add(station.getString("stationName"));
                          text.append(station.getString("stationName") + "\n"); // Just for debug! not needed
                      }
                }
            } catch (JSONException e) {
                Log.e("JSON", "Error getting data.");
            }

            String text2 = stationList.get(0)[0].toString(); // Just for debugging! shows wanted stations on screen
            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setText(text2);





        }
    }



}
