package hyy.pathfinder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Created by h4211 on 10/11/2016.
 */

public class RoutePresenter extends AppCompatActivity implements AsyncResponse, AppDataInterface {

    private List<String[]> trainData = new ArrayList<>();
    private List<String[]> trainDataTimeTableDeparture = new ArrayList<>();
    private List<String[]> trainDataTimeTableArrival = new ArrayList<>();
    private List<List<Route>> routes = new ArrayList<>();
    private int pendingRoutes = 0;
    private ProgressDialog progressDialog;
//    private String stationStartShortCode = "";
 //   private String stationEndShortCode = "";

    private Activity context = this;

    // Data collected from calling intent (MainActivity)
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private String origin;
    private Date originTime;
    private String originDate;
    private String destination;
    private Boolean useMyLocation;
    private List<String[]> stationData = new ArrayList<>(); // get info from current stations. Name, shortCode, latitude and longitude

    private String[] foundOriginStation;
    private String[] foundDestinationStation;

    private JSONArray trainJSON;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routepresenter);

        // asetetaan delegaatti, jotta callbackit palautuvat tälle aktiviteetille
        ApplicationData.setApplicationDataCallbacksDelegate(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(ApplicationData.applicationDataCallbacks);

        //testing
        //AppBarLayout layout = (AppBarLayout) findViewById(R.id.collapsing_toolbar);
        //layout.setExpanded(true);
        //layout.scroll

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



        // Main logic //////////////
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getCustomProgressDialogMessage());
        progressDialog.show();

        /// Fetch and parse current train stations from digitraffic
        Message message = handler.obtainMessage();
        message.what = 0;
        handler.sendMessage(message);
        Log.d("Handler Message", message.toString());


    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){
                case 0:     /// Fetch and parse current train stations from digitraffic
                    Log.d("AsyncTask called", "stationDataTask");
                    Log.d("AsyncTask data","http://rata.digitraffic.fi/api/v1/metadata/stations.json");

                    AsyncJsonFetcher asyncStationFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncStationFetcher.fetchStations("http://rata.digitraffic.fi/api/v1/metadata/stations.json");
                    break;
                case 1:
                    Log.d("findClosestStations", "called");
                    findClosestStations(); // Find closest stations from origin and destination after stationDataTask finishes

                    Log.d("AyncTask called", "trainDataTask");
                    Log.d("AsyncTask data", "http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                            + foundOriginStation[0] + "&arrival_station=" + foundDestinationStation[0] + "&departure_date=" + originDate);

                    AsyncJsonFetcher asyncTrainFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncTrainFetcher.delegate = RoutePresenter.this;
                    asyncTrainFetcher.fetchTrains("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                            + foundOriginStation[0] + "&arrival_station=" + foundDestinationStation[0] + "&departure_date=" + originDate);
                    break;
                case 2:
                    searchDirectTrackConnection(trainJSON);
                    progressDialog.dismiss();
                    break;
            }

            return false;
        }
    });

    @Override
    public void onAsyncJsonFetcherComplete(int mode, JSONArray json){
        Message message = handler.obtainMessage();
        switch (mode) {
            case 1:
                // Fetch Stations
                try {
                    for (int i=0; i < json.length(); i++) {
                        JSONObject station = json.getJSONObject(i);
                        if (station.getString("passengerTraffic") == "true") {
                            // Add data to a two-dimensional array of passenger stations in Finland, including longitude and latitude
                            stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("latitude"), station.getString("longitude")});
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("AsyncTask finished", "stationDataTask");

                message.what = 1;
                handler.sendMessage(message);
                Log.d("Handler Message", message.toString());
                break;
            case 2:
                // Fetch Trains
                Log.d("AsyncTask finished", "trainDataTask");
                trainJSON = json;

                message.what = 2;
                handler.sendMessage(message);
                Log.d("Handler Message", message.toString());
                break;
            default:
                Log.d("Async switch case", "mode incorrect");
                break;
        }

    }

    private void findClosestStations() {
        Log.d("findClosestStations","Started");
        Router router = new Router();
        router.delegate = RoutePresenter.this;

        if (useMyLocation) {
            foundOriginStation = findClosestStationFromPoint(ApplicationData.mLastLocation); // Find closest train station from origin
            String originStationLocation = (foundOriginStation[1] +","+ foundOriginStation[2]); // Get Latitude and Longitude from found closest station and convert into a string
            String originLocTemp = (ApplicationData.mLastLocation.getLatitude() +","+ ApplicationData.mLastLocation.getLongitude()); // Convert user location from Location to String
            /* KLUP tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?! */
            router.getTravelDistanceAndDuration(originLocTemp, originStationLocation, getBaseContext()); // Get dist&dur from user location to closest station
        } else {
            Location mCustomLocation = getLocationFromAddress(origin); // Convert given address to Location (lat&long)
            foundOriginStation = findClosestStationFromPoint(mCustomLocation); // Find closest train station from converted Location
            String originStationLocation = (foundOriginStation[1] +","+ foundOriginStation[2]); // Get Latitude and Longitude from found closest station and convert into a string
            /* KLUP tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?! */
            router.getTravelDistanceAndDuration(origin, originStationLocation, getBaseContext()); // Get dist&dur from given location to closest station KLUP!!! Tarviiko origin vaihtaa lat&long-stringiksi?
        }
        Location destinationLocation = getLocationFromAddress(destination); // Convert given destination address to Location (lat&long)
        foundDestinationStation = findClosestStationFromPoint(destinationLocation); // Find closest train station from destination
    }


    // Convert given street address into latitude and longitude
    private Location getLocationFromAddress(String strAddress) {
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


    @Override
    public void atSuspended(int errorCode)
    {

    }

    @Override
    public void atConnected(Bundle bundle)
    {

    }

    @Override
    public void atLocationChanged(Location location)
    {

    }

    @Override
    public void atMapReady(GoogleMap googleMap)
    {
        ApplicationData.mMap = googleMap;
        ApplicationData.mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ApplicationData.checkLocationPermission(RoutePresenter.this) && ApplicationData.deviceLocationIsUsable)
        {
            ApplicationData.mMap.setMyLocationEnabled(true);
        }

        ShowTestRouteInMap();
    }


    // Finds closest train station from given coordinates. Compares to every station within stationData List
    private String[] findClosestStationFromPoint(Location mLastLocation) {
        String[] closestStationList = new String[3];
        Location pointOrigin = new Location(mLastLocation);         //////// KLUP jos tyhjä niin kaataa softan
        String closestStationName = "";
        Float closestDist = 9999999999999999f;

        for (int i = 0; i < stationData.size(); i++) {
            Location pointDestination = new Location("pointDestination");
            pointDestination.setLatitude(Double.parseDouble(stationData.get(i)[2]));
            pointDestination.setLongitude(Double.parseDouble(stationData.get(i)[3]));
            Float distTemp = pointOrigin.distanceTo(pointDestination);
            if (distTemp < closestDist) {
                closestDist = distTemp;
                closestStationList[0] = stationData.get(i)[1].toString();
                closestStationList[1] = stationData.get(i)[2];
                closestStationList[2] = stationData.get(i)[3];
                closestStationName = stationData.get(i)[1];
            }
        }
        Log.d("Closest station", closestStationName);
        return closestStationList;
    }

    // Custom texts for progressdialog
    private String getCustomProgressDialogMessage(){
        List<String> messageList = new ArrayList<>();
        // You can freely add text here and it shows up in the progressDialog
        messageList.add("Lasketaan ratapalkkeja");
        messageList.add("Hölkätään jalkakäytävällä");
        messageList.add("Ihmetellään nähtävyyksiä");

        Random random = new Random();
        int i = random.nextInt(messageList.size());
        String message = messageList.get(i);
        return message;
    }


    @Override
    public void getTotalDistanceAndDurationFinish(Route route)
    {

   //     directTrackConnectionFound = searchDirectTrackConnection(trainJSON);
  //      if (!directTrackConnectionFound) {
            // Tee jotain jos  ei löydy suoraa reittiä
    //    } else {
            // Tee jotain muuta jos löytyy suora reitti

  //      }
    }


    @Override
    public void getRouteFinish(Route route) {
        // jos route on koko reitin ensimmäinen pätkä, niin lisätään uusi lista routesiin ja laitetaan uuteen listaan route
        if(route.index == 0)
        {
            List<Route> routeList = new ArrayList<>();
            routeList.add(route.index,route);
            routes.add(routeList);

        }
        // jos ei, niin lisätään route routesin viimeiseen olemassaolevaan listaan jatkoksi
        else
        {
            routes.get(routes.size()-1).add(route.index,route);
        }
        pendingRoutes--;
    }


    // tälle lista koordinaatteja tai osoitteita (tai molempia) joilla kutsutaan forloopissa getRoutea. getRoute tuottaa listan route-objekteja getRouteFinishissä
    public void getFullRoute(List<List<String>> coordinates)
    {
        /*
        String destination = URLEncoder.encode(destinationString);
        String origin = URLEncoder.encode(originString);
        Router router = new Router();
        router.delegate = this;
        router.getRoute(origin, destination, getBaseContext(), );*/
        for(int i = 0;i<coordinates.size();i++)
        {
            pendingRoutes++;
            // onko parempi luoda uusi router jokaiselle getRoutelle vai ajaa yhtä router olion getRoutea forloopissa?
            new Router(this).getRoute(coordinates.get(i).get(0), coordinates.get(i).get(1), getBaseContext(), i);
        }
    }


    protected void searchDirectTrackConnection(JSONArray json) {
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
                    if (tt.getString("stationShortCode").equals(foundOriginStation[0]) && tt.getString("type").equals("DEPARTURE")) {  // foundOriginStation.get(0) = stationShortCode
                        Log.d("OriginStation", foundOriginStation[0]);
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
                    } else if (tt.getString("stationShortCode").equals(foundDestinationStation[0]) && tt.getString("type").equals("ARRIVAL")) { // foundDestinationStation.get(0) = stationShortCode
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

        } else textView.setText("No direct trains today");
        Log.d("Function called", "Find closest station");

    }

    // näytetään annetun route-objektilistan kokonaisreitti kartalla
    protected void ShowRouteInMap(List<Route> routes)
    {
        PolylineOptions polylineOptions = new PolylineOptions();
        for(int i = 0; i<routes.size();i++)
        {
            // kannattaako fiksata päällekkäisten koordinaattien syntyminen?
            polylineOptions.add(routes.get(i).origin);
            polylineOptions.add(routes.get(i).destination);
        }
        polylineOptions.width(5).color(Color.RED);
        ApplicationData.mMap.addPolyline(polylineOptions);
    }

    protected void ShowTestRouteInMap()
    {
        LatLng c1 = new LatLng(51.11,10);
        LatLng c2 = new LatLng(51.9,10.1);
        LatLng c3 = new LatLng(51.5,9.9);

        PolylineOptions options = new PolylineOptions();
        options.add(c1).add(c2).add(c3);
        options.width(10);
        options.color(Color.RED);
        ApplicationData.mMap.addPolyline(options);
    }


}
