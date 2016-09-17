package hyy.pathfinder;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class route_results extends AppCompatActivity {
    private List<String[]> trainData = new ArrayList<>();
    private List<String[]> trainDataTimeTableDeparture = new ArrayList<>();
    private List<String[]> trainDataTimeTableArrival = new ArrayList<>();
    private String stationStartShortCode = "";
    private String stationEndShortCode = "";
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private Date stationStartTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_results);
        // Get data from calling intent
        Bundle extras = getIntent().getExtras();
       // String test = extras.getString("test");
        stationStartShortCode = extras.getString("StationStartShortCode");
        String stationStartLongitude = extras.getString("StationStartLongitude");
        String stationStartLatitude = extras.getString("StationStartLatitude");
        String stationStartDate = extras.getString("StationStartDate");
        try {
            stationStartTime = timeFormat.parse(extras.getString("StationStartTime"));
        } catch (ParseException ex) {
            // KLUP Time invalid
        }
        stationEndShortCode = extras.getString("StationEndShortCode");
        String stationEndLongitude = extras.getString("StationEndLongitude");
        String stationEndLatitude = extras.getString("StationEndLatitude");

        // Create and run new AsyncDataTask
        FetchDataTask trainDataTask = new FetchDataTask();
        //trainDataTask.execute("http://rata.digitraffic.fi/api/v1/schedules?departure_station=HKI&arrival_station=TPE");
        trainDataTask.execute("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                + stationStartShortCode + "&arrival_station=" + stationEndShortCode + "&departure_date=" + stationStartDate);
        Toast.makeText(this,stationStartDate,Toast.LENGTH_LONG).show();
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
            // ---------DEV------- PROTO
            /*
            direct = findDirectConnection(startLoc, endLoc);
            if (direct = "true") {
            *löyty
            } else {
            indirect = findIndirectConnection(startLoc, endLoc);
            if (indirect == "true") {
            *ei löytyny
            }
            }
            */
            // /PROTO ---------------------
            // Find text block to put data
            TextView textView = (TextView) findViewById(R.id.result1);
            String text = "";
            Bundle routeData = new Bundle();
            String[] scheduledTimeTemp;
            // ----------DEV--------- Get all trains from station (start-end)
            try {
                // First get all trains from current station to destination
                for (int i=0; i < json.length(); i++) {
                    JSONObject train = json.getJSONObject(i);
                    JSONArray timeTable = train.getJSONArray("timeTableRows");
                    for (int y = 0; y < timeTable.length(); y++) {
                        JSONObject tt = timeTable.getJSONObject(y);
                        if (tt.getString("stationShortCode").equals(stationStartShortCode) && tt.getString("type").equals("DEPARTURE")) {
                            // Split string "scheduledTime" into two different strings and convert to more user convenient format
                            StringBuilder stringBuilder = new StringBuilder(tt.getString("scheduledTime"));
                            stringBuilder.deleteCharAt(stringBuilder.length()-1);
                            //scheduledTimeTemp = tt.getString("scheduledTime").split("T");
                            trainDataTimeTableDeparture.add(new String[] { tt.getString("type"), tt.getString("commercialTrack"), stringBuilder.toString()});
                        } else if (tt.getString("stationShortCode").equals(stationEndShortCode) && tt.getString("type").equals("ARRIVAL")) {
                            trainDataTimeTableArrival.add(new String[] { tt.getString("type"), tt.getString("commercialTrack"), tt.getString("scheduledTime")});
                        }
                    }
                    trainData.add(new String[] {train.getString("trainNumber"), train.getString("departureDate"), train.getString("trainType")});
                }
                // Next remove all trains before selected time of day
                Date timetemp;
                for (int i=0; i < trainData.size(); i++) {
                    timetemp = timeFormat.parse(trainDataTimeTableDeparture.get(i)[2]);
                if (timetemp.before(stationStartTime)) {
                    trainData.remove(i);
                    trainDataTimeTableDeparture.remove(i);
                    trainDataTimeTableArrival.remove(i);
                    i = 0;
                }
                }
            } catch (Exception e) { // JSONException?
                // If no straight trains found
                textView.setText("No trains found!");
                // --------DEV---------- KLUP now program should start looking for alternate routes (via stop/multiple stops)
            }
            for (int i = 0; i < trainData.size(); i++) {
            for (int x = 0; x < 3; x++) {
              //  text += trainData.get(i)[x] +" ";
            }
                for (int a = 0; a < 3; a++) {
                  //  text += trainDataTimeTableArrival.get(i)[a] +" ";
                }
                for (int d = 0; d < 3; d++) {
                 //   text += trainDataTimeTableDeparture.get(i)[d] +" ";
                }
           // text += "\n";
        }
            routeData.putString("trainNumber", trainData.get(0)[0]);
            routeData.putString("trainType", trainData.get(0)[2]);
            routeData.putString("depType", trainDataTimeTableDeparture.get(0)[0]);
            routeData.putString("depTrack", trainDataTimeTableDeparture.get(0)[1]);
            routeData.putString("depTime", trainDataTimeTableDeparture.get(0)[2]);
            routeData.putString("arrType", trainDataTimeTableArrival.get(0)[0]);
            routeData.putString("arrTrack", trainDataTimeTableArrival.get(0)[1]);
            routeData.putString("arrTime", trainDataTimeTableArrival.get(0)[2]);
            text = routeData.getString("trainType")+" "+routeData.getString("trainNumber")+"\n"
                    +routeData.getString("depType")+" "+routeData.getString("depTrack")+" "+routeData.getString("depTime")+"\n"
                    +routeData.getString("arrType")+" "+routeData.getString("arrTrack")+" "+routeData.getString("arrTime")+"\n"+"\n";
            textView.setText(text);
        }
    }
}
