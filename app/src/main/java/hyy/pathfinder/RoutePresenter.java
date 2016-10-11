package hyy.pathfinder;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by h4211 on 10/11/2016.
 */

public class RoutePresenter extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, AsyncResponse{
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private List<String[]> trainData = new ArrayList<>();
    private List<String[]> trainDataTimeTableDeparture = new ArrayList<>();
    private List<String[]> trainDataTimeTableArrival = new ArrayList<>();
//    private String stationStartShortCode = "";
 //   private String stationEndShortCode = "";

    // Data collected from calling intent (MainActivity)
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private String origin;
    private Date originTime;
    private String originDate;
    private String destination;
    private Boolean useMyLocation;
    private List<String[]> stationData = new ArrayList<>(); // get info from current stations. Name, shortCode, latitude and longitude

    private ArrayList foundOriginStation;
    private ArrayList foundDestinationStation;


    // Monitor AsyncTask status
    private boolean stationDataTaskFinished = false;
    private boolean trainDataTaskFinished = true; // KLUP MUISTA VAIHTAA FALSEKSI
    private JSONArray trainJSON;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routefinder);

        // Get data from calling intent
        Bundle extras = getIntent().getExtras();
        origin = extras.getString("origin"); // street address
        originDate = extras.getString("originDate");
        try {
            originTime = timeFormat.parse(extras.getString("originTime"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        destination = extras.getString("destination");
        useMyLocation = extras.getBoolean("useMyLocation");


        // build Google API client
        buildGoogleApiClient();
        mGoogleApiClient.connect();


        // Create and run needed AsyncTasks.
        Log.d("AyncTask called", "stationDataTask");
        FetchStationDataTask stationDataTask = new FetchStationDataTask();   // Get station metadata
        stationDataTask.execute("http://rata.digitraffic.fi/api/v1/metadata/stations.json");

      //  Log.d("AyncTask called", "trainDataTask");
      //  FetchDataTask trainDataTask = new FetchDataTask();
      //  trainDataTask.execute("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
      //          + stationStartShortCode + "&arrival_station=" + stationEndShortCode + "&departure_date=" + stationStartDate);

        Log.d("AyncTask called", "monitorDataTask");
        MonitorDataTask monitorDataTask = new MonitorDataTask();
        monitorDataTask.execute();

    }


    // MonitorDataTask monitors and waits until other AsyncTasks are ready, then magic happens
    class MonitorDataTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;
        @Override
        protected Void doInBackground(Void... params) {
            while (stationDataTaskFinished == false && trainDataTaskFinished == false) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(RoutePresenter.this, "Tekemistä", "Lasketaan ratapalkkeja", true);
        }
        // MAGIC HAPPENS HERE
        protected void onPostExecute(Void result) {
            Log.d("AsyncTask finished", "MonitorDataTask");
            boolean directTrackConnectionFound = false;
            progressDialog.dismiss();

            Router router = new Router();
            router.delegate = RoutePresenter.this;

            if (useMyLocation) {
                foundOriginStation = findClosestStationFromPoint(mLastLocation); // Find closest train station from origin
                String originStationTemp = (foundOriginStation.get(1) +","+ foundOriginStation.get(2)); // Get Latitude and Longitude from found closest station and convert them into a string
                Location destinationLocation = getLatLngFromAddress(destination); // Convert destination street address to latitude and longitude
                foundDestinationStation = findClosestStationFromPoint(destinationLocation); // Find closest train station from destination
                String originLocTemp = (mLastLocation.getLatitude() +","+ mLastLocation.getLongitude()); // Convert user location from Location to String
                router.getTravelDistanceAndDuration(originLocTemp, originStationTemp, getBaseContext()); /* KLUP tää tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?! */
            } else {
                Location mCustomLocation = getLatLngFromAddress(origin);
                foundOriginStation = findClosestStationFromPoint(mCustomLocation);
                String originStationTemp = (foundOriginStation.get(1) +","+ foundOriginStation.get(2));

                router.getTravelDistanceAndDuration(origin, originStationTemp, getBaseContext());

                Location destinationLocation = getLatLngFromAddress(destination);
                foundDestinationStation = findClosestStationFromPoint(destinationLocation);
            }


        }
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

            //      boolean directTrackConnectionFound = false;
            //          directTrackConnectionFound = searchDirectTrackConnection(json);
            trainDataTaskFinished = true;
            Log.d("AsyncTask finished", "trainDataTask");
            trainJSON = json;
        }
    }


    class FetchStationDataTask extends AsyncTask<String, Void, JSONArray> {
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
                        stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("latitude"), station.getString("longitude")});
                    }
                }
            } catch (JSONException e) {
                Log.d("JSON", "Error getting data.");
            }
            stationDataTaskFinished = true;
            Log.d("AsyncTask finished", "stationDataTask");
            //           findClosestStationFromPoint(65.5,45.5); // DESIRED LOCATION NEEDED!
        }
    }










    // Convert given street address into latitude and longitude
    private Location getLatLngFromAddress(String strAddress) {
        Geocoder geocoder = new Geocoder(this);
        Location point = new Location("");
        List<android.location.Address> address;
        try {
            address = geocoder.getFromLocationName(strAddress, 1);
            if (address != null) {
                android.location.Address location = address.get(0);

                point.setLatitude(location.getLatitude());
                point.setLongitude(location.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return point;
    }

    // Finds closest train station from given coordinates. Compares to every station within stationData List
    private ArrayList<String> findClosestStationFromPoint(Location mLastLocation) {
        ArrayList<String> closestStation = new ArrayList<>();
        Location pointOrigin = new Location(mLastLocation);
        String closestStationName = "";
        Float closestDist = 9999999999999999f;

        for (int i = 0; i < stationData.size(); i++) {
            Location pointDestination = new Location("pointDestination");
            pointDestination.setLatitude(Double.parseDouble(stationData.get(i)[2]));
            pointDestination.setLongitude(Double.parseDouble(stationData.get(i)[3]));
            Float distTemp = pointOrigin.distanceTo(pointDestination);
            if (distTemp < closestDist) {
                closestDist = distTemp;
                closestStation.add(stationData.get(i)[1].toString());
                closestStation.add(stationData.get(i)[2].toString());
                closestStation.add(stationData.get(i)[3].toString());
                closestStationName = stationData.get(i)[1];
            }
        }
        Log.d("Closest station", closestStationName);
        return closestStation;
    }


    @Override
    public void getSpaceTimeFinish(List<String> data)
    {

   //     directTrackConnectionFound = searchDirectTrackConnection(trainJSON);
  //      if (!directTrackConnectionFound) {
            // Tee jotain jos  ei löydy suoraa reittiä
    //    } else {
            // Tee jotain muuta jos löytyy suora reitti

  //      }
    }
    @Override
    public void getRouteFinish(PolylineOptions data) {}
    @Override
    public void onLocationChanged(Location location){}

    @Override
    public void onConnected(Bundle bundle) {

        createLocationRequest();
        if(useMyLocation)
        {
            if (LocationPermissionAgent.isLocationEnabled(getBaseContext()))
            {
                // tutkittava myöhemmin paremman/tarkemman paikannuksen mahdollisuuksia
               /* ProviderLocationTracker GpsProviderLocationTracker = new ProviderLocationTracker(getApplicationContext(), ProviderLocationTracker.ProviderType.GPS);
                GpsProviderLocationTracker.start();*/

                startLocationUpdates();
                getLastLocation();
                origin = String.valueOf(mLastLocation.getLatitude()) + "," + String.valueOf(mLastLocation.getLongitude());

            //    findRoute(origin, destination); tarvitaanko?!
            }
            else
            {
                PermissionDialogFragment permissionDialogFragment = new PermissionDialogFragment();
                permissionDialogFragment.show(getSupportFragmentManager(),"permissionRequest");
            }
        }
        else
        {
            //findRoute(origin, destination); tarvitaanko?!
        }
    }

    public void findRoute(String originString, String destinationString)
    {
        String destination = URLEncoder.encode(destinationString);
        String origin = URLEncoder.encode(originString);
        Router router = new Router();
        router.delegate = this;
        router.getRoute(origin, destination, getBaseContext());
    }
    protected void startLocationUpdates()
    {
        if (checkLocationPermission())
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }
    protected void getLastLocation()
    {
        if(checkLocationPermission())
        {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
    }
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}



    protected boolean searchDirectTrackConnection(JSONArray json) {
   /*     // Find text block to put data, JUST FOR DEBUGGING. FINAL DATA DERIVED THROUGH BUNDLES!(?)
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
                if (originTime.after(timeTemp)) {
                    // i.remove() removes trainDataTimeTableDeparture(0)
                    i.remove();
                    trainData.remove(0);
                    trainDataTimeTableArrival.remove(0);
                }
            }

            /// ------- DEV ------ Null box. Use this to show data, debugging purposes
            TextView ruutu = (TextView) findViewById(R.id.test);
            ruutu.setText("Set depTime: " + originTime + "\n");


        } catch (Exception e) { // JSONException?
            // If no straight trains found
            Log.d("No trains found", e.toString());
            // --------DEV---------- KLUP now program should start looking for alternate routes (via stop/multiple stops)
        }


        boolean directTrackConnectionFound = false;
        // Put all retrieved data into a bundle for easier handling(?)
        if (trainData.size() != 0) {
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
        Log.d("Function called", "Find closest station");

        return directTrackConnectionFound;*/
        return false;
    }



}
