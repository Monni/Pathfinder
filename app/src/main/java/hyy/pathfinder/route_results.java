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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class route_results extends AppCompatActivity {
    private List<String[]> trainData = new ArrayList<>();
    private List<String[]> trainDataTimeTableDeparture = new ArrayList<>();
    private List<String[]> trainDataTimeTableArrival = new ArrayList<>();
    private String stationStartShortCode = "";
    private String stationEndShortCode = "";
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private Date stationStartTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_results);

        // Get data from calling intent
        Bundle extras = getIntent().getExtras();
        stationStartShortCode = extras.getString("StationStartShortCode");
        String stationStartLongitude = extras.getString("StationStartLongitude");
        String stationStartLatitude = extras.getString("StationStartLatitude");
        String stationStartDate = extras.getString("StationStartDate");
        try {
            stationStartTime = timeFormat.parse(extras.getString("StationStartTime"));
        } catch (Exception ex) {
            // KLUP Time invalid
        }
        stationEndShortCode = extras.getString("StationEndShortCode");
        String stationEndLongitude = extras.getString("StationEndLongitude");
        String stationEndLatitude = extras.getString("StationEndLatitude");

        // Create and run new AsyncDataTask
        FetchDataTask trainDataTask = new FetchDataTask();
        trainDataTask.execute("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                + stationStartShortCode + "&arrival_station=" + stationEndShortCode + "&departure_date=" + stationStartDate);
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

        protected void onPreExecute() {
            // Search for station locations here and compare to user input(location) here, to find closest one
        }


        protected void onPostExecute(JSONArray json) {
            boolean directTrackConnectionFound = false;
           directTrackConnectionFound = searchDirectTrackConnection(json);

        }

        protected boolean searchDirectTrackConnection(JSONArray json) {
            // Find text block to put data, JUST FOR DEBUGGING. FINAL DATA DERIVED THROUGH BUNDLES!(?)
            TextView textView = (TextView) findViewById(R.id.result1);
            String text;
            Bundle routeData = new Bundle();
            String[] scheduledTimeTemp;
            String[] scheduledDate;
            // ----------DEV--------- Get all trains from station (start-end)
            try {
                // First get all trains from current station to destination
                for (int i = 0; i < json.length(); i++) {
                    JSONObject train = json.getJSONObject(i);
                    JSONArray timeTable = train.getJSONArray("timeTableRows");
                    for (int y = 0; y < timeTable.length(); y++) {
                        JSONObject tt = timeTable.getJSONObject(y);
                        if (tt.getString("stationShortCode").equals(stationStartShortCode) && tt.getString("type").equals("DEPARTURE")) {
                            // Split string "scheduledTime" into two different strings and convert to more user convenient format
                            // Create StringBuilder and remove last letter ('Z' in json array)
                            StringBuilder sb = new StringBuilder(tt.getString("scheduledTime"));
                            sb.deleteCharAt(sb.length() - 1);
                            // Split StringBuilder into two variables (scheduledDate and scheduledTime)
                            scheduledTimeTemp = sb.toString().split("T");
                            // Convert scheduledDate from YYYY-MM-DD to DD.MM.YYY
                            scheduledDate = scheduledTimeTemp[0].split("-");
                            String scheduledDateConverted = "";
                            for (int d = scheduledDate.length; d > 0; d--) {
                                scheduledDateConverted += scheduledDate[d - 1];
                                if (d - 1 > 0) {
                                    scheduledDateConverted += ".";
                                }
                            }
                            // Convert time
                            StringBuilder timeStringBuilder = new StringBuilder(scheduledTimeTemp[1]);
                            timeStringBuilder.delete(5, timeStringBuilder.length());
                            // Put data into arrayList if correct
                            trainDataTimeTableDeparture.add(new String[]{tt.getString("type"), tt.getString("commercialTrack"), scheduledDateConverted, timeStringBuilder.toString()});
                        } else if (tt.getString("stationShortCode").equals(stationEndShortCode) && tt.getString("type").equals("ARRIVAL")) {
                            StringBuilder sb = new StringBuilder(tt.getString("scheduledTime"));
                            sb.deleteCharAt(sb.length() - 1);
                            // Split StringBuilder into two variables (scheduledDate and scheduledTime)
                            scheduledTimeTemp = sb.toString().split("T");
                            // Convert scheduledDate from YYYY-MM-DD to DD.MM.YYY
                            scheduledDate = scheduledTimeTemp[0].split("-");
                            String scheduledDateConverted = "";
                            for (int d = scheduledDate.length; d > 0; d--) {
                                scheduledDateConverted += scheduledDate[d - 1];
                                if (d - 1 > 0) {
                                    scheduledDateConverted += ".";
                                }
                            }
                            // Convert time from HH:mm:ss:xxZ to HH:mm
                            StringBuilder timeStringBuilder = new StringBuilder(scheduledTimeTemp[1]);
                            timeStringBuilder.delete(5, timeStringBuilder.length());
                            // Put data into arrayList if correct
                            trainDataTimeTableArrival.add(new String[]{tt.getString("type"), tt.getString("commercialTrack"), scheduledDateConverted, timeStringBuilder.toString()});
                        }
                    }
                    trainData.add(new String[]{train.getString("trainNumber"), train.getString("departureDate"), train.getString("trainType")});
                }

                // Next remove all trains before selected time of day, uses iterator to go trough data
                Date timeTemp;
                Iterator<String[]> i = trainDataTimeTableDeparture.iterator();
                while (i.hasNext()) {
                    String[] s = i.next();
                    timeTemp = timeFormat.parse(s[3]);
                    if (stationStartTime.after(timeTemp)) {
                        // i.remove() removes trainDataTimeTableDeparture(0)
                        i.remove();
                        trainData.remove(0);
                        trainDataTimeTableArrival.remove(0);
                    }
                }

                /// ------- DEV ------ Null box. Use this to show data, debugging purposes
                TextView ruutu = (TextView) findViewById(R.id.test);
                ruutu.setText(stationStartTime + "\n");


            } catch (Exception e) { // JSONException?
                // If no straight trains found
                textView.setText("No trains found!");
                // --------DEV---------- KLUP now program should start looking for alternate routes (via stop/multiple stops)
            }


            boolean directTrackConnectionFound = false;
            // Put all retrieved data into a bundle for easier handling(?)
            if (trainData != null) {
                routeData.putString("trainNumber", trainData.get(0)[0]);
                routeData.putString("trainType", trainData.get(0)[2]);
                routeData.putString("depType", trainDataTimeTableDeparture.get(0)[0]);
                routeData.putString("depTrack", trainDataTimeTableDeparture.get(0)[1]);
                routeData.putString("depDate", trainDataTimeTableDeparture.get(0)[2]);
                routeData.putString("depTime", trainDataTimeTableDeparture.get(0)[3]);
                routeData.putString("arrType", trainDataTimeTableArrival.get(0)[0]);
                routeData.putString("arrTrack", trainDataTimeTableArrival.get(0)[1]);
                routeData.putString("arrDate", trainDataTimeTableArrival.get(0)[2]);
                routeData.putString("arrTime", trainDataTimeTableArrival.get(0)[3]);
                text = routeData.getString("trainType") + " " + routeData.getString("trainNumber") + "\n"
                        + routeData.getString("depType") + " " + routeData.getString("depTrack") + " " + routeData.getString("depDate") + " " + routeData.getString("depTime") + "\n"
                        + routeData.getString("arrType") + " " + routeData.getString("arrTrack") + " " + routeData.getString("arrDate") + " " + routeData.getString("arrTime") + "\n";
                textView.setText(text);
                directTrackConnectionFound = true;
            } else textView.setText("No direct trains today");
            return directTrackConnectionFound;
        }
    }
}
