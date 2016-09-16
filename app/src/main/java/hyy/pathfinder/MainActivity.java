package hyy.pathfinder;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> stationList = new ArrayList<>(); // For AutoCompleteTextView (Full station names only)
    private List<String[]> stationData = new ArrayList<>(); // More complete data of current train stations (incl. Full name, ShortCode, Longitude and Latitude)+
    // Create DecimalFormat to force date and time into two digit format
    private DecimalFormat doubleDigitFormat = new DecimalFormat("00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FetchDataTask stationDataTask = new FetchDataTask();
        stationDataTask.execute("http://rata.digitraffic.fi/api/v1/metadata/stations.json"); // Get data from current train stations

        // AutoCompleteTextView for start and end locations
        AutoCompleteTextView actv_start = (AutoCompleteTextView) findViewById(R.id.locStart);
        AutoCompleteTextView actv_end = (AutoCompleteTextView) findViewById(R.id.locEnd);
        ArrayAdapter<String> aa = new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,stationList);
        actv_start.setAdapter(aa);
        actv_end.setAdapter(aa);

        // ------- DEV KLUP ------ need to figure out how to get current timezone. Not displaying time correctly
        // Calendar for departure date and time, gets current system datetime
        final Calendar calendar = Calendar.getInstance();
        final int month = calendar.get(Calendar.MONTH) + 1;
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        final int year = calendar.get(Calendar.YEAR);
     //   final int hours = Integer.valueOf(doubleDigitFormat.format(Double.valueOf(Calendar.HOUR_OF_DAY)));
        final int hour = Integer.valueOf(doubleDigitFormat.format(Calendar.HOUR_OF_DAY));
        final int minute = Integer.valueOf(doubleDigitFormat.format(Calendar.MINUTE));

        // Create listener for "immediately" button. If checked, disable departure date
        CompoundButton locStartImmediately = (Switch) findViewById(R.id.locStartImmediately);
        locStartImmediately.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    // Disable date, set current date
                    findViewById(R.id.locStartDate).setEnabled(false);
                    EditText date = (EditText) findViewById(R.id.locStartDate);
                    date.setText(day +"."+ month +"."+ year);
                    // Disable time, set current time
                    findViewById(R.id.locStartTime).setEnabled(false);
                    EditText time = (EditText) findViewById(R.id.locStartTime);
                    time.setText(hour +":"+ minute);
                }
                else  {
                    // Return control to date / EditText
                    findViewById(R.id.locStartDate).setEnabled(true);
                    EditText date = (EditText) findViewById(R.id.locStartDate);
                    date.setHint("dd.mm.yyyy");
                    // Return control to time / EditText
                    findViewById(R.id.locStartTime).setEnabled(true);
                    EditText time = (EditText) findViewById(R.id.locStartTime);
                    time.setHint("hh:mm");
                }
            }
        });
    }

    class FetchDataTask extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            JSONArray json = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();   // Open connection to rata.digitraffic.fi
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
            try {
                  for (int i=0; i < json.length(); i++) {
                    JSONObject station = json.getJSONObject(i);
                      if (station.getString("passengerTraffic") == "true") {
                          // Add data to a two-dimensional array of passenger stations in Finland, including longitude and latitude
                          stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("longitude"), station.getString("latitude")});
                          stationList.add(station.getString("stationName"));
                          text.append(station.getString("stationName") + "\n"); // Just for debug! not needed
                      }
                }
            } catch (JSONException e) {
                Log.e("JSON", "Error getting data.");
            }

            String text2 = stationData.get(0)[0]; // Just for debugging! shows wanted stations on screen
            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setText(text2);
        }
    }

    public void find_route(View view) {
        EditText locStart = (AutoCompleteTextView) findViewById(R.id.locStart);
        String stationStartName = locStart.getText().toString();
        EditText locEnd = (AutoCompleteTextView) findViewById(R.id.locEnd);
        String stationEndName = locEnd.getText().toString();

        // Convert departure date into suitable format (YYYY-MM-DD)
        EditText locStartDate = (EditText) findViewById(R.id.locStartDate);
        String[] startDateString = locStartDate.getText().toString().split("\\.");
        String locStartDateConverted = "";
        for (int i = startDateString.length; i > 0; i--) {
            locStartDateConverted += doubleDigitFormat.format(Double.valueOf(startDateString[i-1]));
            if (i-1 != 0) {
                locStartDateConverted += "-";
            }
        }
        // Get departure time (HH:MM)
        String locStartTime = findViewById(R.id.locStartTime).toString();
        // --------DEV-------- KLUP Check if user put time in correct format

        //Toast.makeText(this,locStartDateConverted,Toast.LENGTH_LONG).show(); // JUST FOR DEBUGGING, NOT NEEDED

        // Data for starting station (name, shortCode, longitude, latitude)
        String stationStartShortCode = "";
        String stationStartLongitude = "";
        String stationStartLatitude = "";
            for (int i = 0; i < stationData.size(); i++ ) {
                if (stationData.get(i)[0].equals(stationStartName)) {
                    stationStartShortCode = stationData.get(i)[1];
                    stationStartLongitude = stationData.get(i)[2];
                    stationStartLatitude = stationData.get(i)[3];
                }
            }
       // Toast.makeText(this,stationStartShortCode,Toast.LENGTH_SHORT).show(); // JUST FOR DEBUGGING. NOT NEEDED

        // Data for end station (name, shortCode, longitude, latitude)
        String stationEndShortCode = "";
        String stationEndLongitude = "";
        String stationEndLatitude = "";
        for (int i = 0; i < stationData.size(); i++ ) {
            if (stationData.get(i)[0].equals(stationEndName)) {
                stationEndShortCode = stationData.get(i)[1];
                stationEndLongitude = stationData.get(i)[2];
                stationEndLatitude = stationData.get(i)[3];
            }
        }
        // Create new intent for search results
        Intent intent_findroute = new Intent(this,route_results.class);
        // Put start&end station data to new intent
        intent_findroute.putExtra("StationStartShortCode",stationStartShortCode);
        intent_findroute.putExtra("StationStartLongitude",stationStartLongitude);
        intent_findroute.putExtra("StationStartLatitude",stationStartLatitude);
        intent_findroute.putExtra("StationStartDate", locStartDateConverted);
        intent_findroute.putExtra("StationStartTime", locStartTime);
        intent_findroute.putExtra("StationEndShortCode", stationEndShortCode);
        intent_findroute.putExtra("StationEndLongitude", stationEndLongitude);
        intent_findroute.putExtra("StationEndLatitude", stationEndLatitude);
        startActivity(intent_findroute);
    }



}
