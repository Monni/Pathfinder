package hyy.pathfinder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.Application;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

// TODO: Keksittävä miten haetaan reittivaihtoehtoja
// TODO: Tehtävä recyclerview, adapteri ja korttinäkymä kokonaisreittivaihtoehdoille

public class RoutePresenter extends AppCompatActivity implements AsyncResponse, AppDataInterface {

    // Siirrä kaikki muuttujat ApplicationDataan staattisiksi muuttujiksi joita tarvisee kuljettaa aktiviteetista toiseen
    boolean listIsComplete;

    private List<String[]> trainData = new ArrayList<>();
    private List<String[]> trainDataTimeTableDeparture = new ArrayList<>();
    private List<String[]> trainDataTimeTableArrival = new ArrayList<>();
   // private List<List<Route>> routes = new ArrayList<>();
   // private int pendingRoutes = 0;
    private ProgressDialog progressDialog;
//    private String stationStartShortCode = "";
 //   private String stationEndShortCode = "";

    private Context context = this;

    // Data collected from calling intent (MainActivity)
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    private String origin;
    private Date originTime;
    private String originDate;
    private String destination;
    private List<String[]> stationData = new ArrayList<>(); // get info from current stations. Name, shortCode, latitude and longitude

    private String[] foundOriginStation;
    private String[] foundDestinationStation;

    private JSONArray trainJSON;

    // MIIKAN TAIKAA
    private List<Route> routeList = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private Location destinationLocation;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routepresenter);

        recyclerView = (RecyclerView) findViewById(R.id.route_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // asetettava delegaatti callbackeja varten, pysäytettävä edellisen googleapiclientin päivitykset ja luotava uusi googleapiclient uudelle aktiviteetille
        ApplicationData.setApplicationDataCallbacksDelegate(this);
        ApplicationData.stopLocationUpdates();
        ApplicationData.buildGoogleApiClient(this);
        if(ApplicationData.deviceLocationListeningPermitted)
        {
            Log.d("onCreate","Connecting googleapiclient");
            ApplicationData.connectGoogleApiClient();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(ApplicationData.applicationDataCallbacks);

        // Asetetaan kuuntelija collapsingToolbarLayoutille, jotta saadaan title piiloon kun kartta tulee esiin

        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        final AppBarLayout appbarLayout = (AppBarLayout)findViewById(R.id.appBarLayout);

        appbarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbarLayout.setTitle("Select preferred route");
                    isShow = true;
                } else if(isShow) {
                    collapsingToolbarLayout.setTitle(" ");//careful there should a space between double quote otherwise it wont work
                    isShow = false;
                }
            }
        });

        // kuuntelija kelluvalle napille - myöhemmin tarpeeton. Esimerkkinä siitä kuinka toolbar collapsataan onclickillä cardien karttaa varten

        FloatingActionButton fabRight = (FloatingActionButton)findViewById(R.id.fabRight);

        fabRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (appbarLayout.getTop() < 0)
                    appbarLayout.setExpanded(true);
                else
                    appbarLayout.setExpanded(false);
            }
        });


        FloatingActionButton fabLeft = (FloatingActionButton)findViewById(R.id.fabLeft);

        fabLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMapBounds();
            }
        });

        // Get data from calling intent
        Bundle extras = getIntent().getExtras();
        if(!ApplicationData.deviceLocationIsOrigin)
        {
            origin = extras.getString("origin");// street address
        }


        originDate = extras.getString("originDate");
        try {
            originTime = timeFormat.parse(extras.getString("originTime"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        destination = extras.getString("destination");



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
        ApplicationData.routeListList = new ArrayList<>();
        ApplicationData.routeListList.add(new ArrayList<Route>());
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){
                case 0:     /// Fetch and parse current train stations from digitraffic
                    Log.d("Handler.what = 0", "Fetching stationlist");
                    Log.d("JSON query URL","http://rata.digitraffic.fi/api/v1/metadata/stations.json");

                    AsyncJsonFetcher asyncStationFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncStationFetcher.fetchStations("http://rata.digitraffic.fi/api/v1/metadata/stations.json");
                    break;
                case 1:
                    Log.d("Handler.what = 1", "Looking for closest stations");
                    findClosestStations(); // Find closest stations from origin and destination after stationDataTask finishes

                    // Get JSON data from digitraffic (between found originStation and destinationStation
                    Log.d("JSON query URL", "http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                            + foundOriginStation[0] + "&arrival_station=" + foundDestinationStation[0] + "&departure_date=" + originDate);

                    AsyncJsonFetcher asyncTrainFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncTrainFetcher.delegate = RoutePresenter.this;
                    asyncTrainFetcher.fetchTrains("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                            + foundOriginStation[0] + "&arrival_station=" + foundDestinationStation[0] + "&departure_date=" + originDate);

                    break;
                case 2:
                    Log.d("Handler.what = 2", "Searching for direct track connections between found stations");
                    searchDirectTrackConnection(trainJSON);
                    progressDialog.dismiss();
                    break;
            }

            return false;
        }
    });

    @Override
    public void onAsyncJsonFetcherComplete(int mode, JSONArray json, boolean jsonException){
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
                // tarkistus löysikö AsyncJsonFetcher.
                if(!jsonException)
                {
                    // Tekee kävelyreitin aloituspaikasta lähtöasemalle, lähtöasemalta suora viiva kohdeasemalle ja lopulta kävelyreitti kohdeasemalta kohteeseen
                    Log.d("Handler", "Trains found, creating route using them along with walking route");
                    createWalkingRoute();
                    createRoutesUsingStations();
                    createBusRoute();
                }
                else
                {
                    // Tekee kävelyreitin alkupisteestä kohteeseen
                    Log.d("Handler", "No trains found, only adding walking route");
                    createWalkingRoute();
                    createBusRoute();

                }

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

        Log.d("findClosestStations", "Started!");
        Router router = new Router();
        router.delegate = RoutePresenter.this;
        Log.d("findClosestStations","Fetching origin");
        if (ApplicationData.deviceLocationIsOrigin) {
            Log.d("findClosestStations","Using device location for origin");
            foundOriginStation = findClosestStationFromPoint(ApplicationData.mLastLocation); // Find closest train station from origin
            String originStationLocation = (foundOriginStation[1] +","+ foundOriginStation[2]); // Get Latitude and Longitude from found closest station and convert into a string
            String originLocTemp = (ApplicationData.mLastLocation.getLatitude() +","+ ApplicationData.mLastLocation.getLongitude()); // Convert user location from Location to String
            /* KLUP tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?! */
            router.getTravelDistanceAndDuration(originLocTemp, originStationLocation, getBaseContext()); // Get dist&dur from user location to closest station
        } else {
            Log.d("findClosestStations","Using other than device location for origin");
            Location mCustomLocation = getLocationFromAddress(origin); // Convert given address to Location (lat&long)
            foundOriginStation = findClosestStationFromPoint(mCustomLocation); // Find closest train station from converted Location
            String originStationLocation = (foundOriginStation[1] +","+ foundOriginStation[2]); // Get Latitude and Longitude from found closest station and convert into a string
            /* KLUP tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?! */
            router.getTravelDistanceAndDuration(origin, originStationLocation, getBaseContext()); // Get dist&dur from given location to closest station KLUP!!! Tarviiko origin vaihtaa lat&long-stringiksi?
        }
        Log.d("findClosestStations","Fetching destination");
        destinationLocation = getLocationFromAddress(destination); // Convert given destination address to Location (lat&long)
        foundDestinationStation = findClosestStationFromPoint(destinationLocation); // Find closest train station from destination
    }

    // ---------------------------------- NÄMÄ TEKEVÄT ROUTERIA HYÖDYNTÄEN ROUTE-OBJEKTIT --------------------------------- //
    // createWalkingRouter, createBusRoute ja createRoutesUsingStations

    // suora reitti kohteeseen
    public void createWalkingRoute()
    {
        Router router = new Router();
        router.delegate = this;
        if(ApplicationData.deviceLocationIsOrigin)
        {
            router.getWalkingRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()), destination,context,0, 0);
        }
        else
        {
            router.getWalkingRoute(origin, destination,context,0, 0);
        }
    }

    public void createBusRoute()
    {
        // bussimatka
        Log.d("createBusRoute", "Creating station to station -route");
        Router router = new Router();
        router.delegate = this;

        if(ApplicationData.deviceLocationIsOrigin)
        {
            Log.d("createBusRoute", "Using device location as reference point");

            router.getBusRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()), destination,context,0, 2);
        }
        else
        {
            Log.d("createBusRoute", "Using typed start location");

            router.getBusRoute(origin, destination,context,0, 2);
        }
    }


    public void createRoutesUsingStations()
    {
        // tarvitaan toinen lista routesListListin sisälle näitä varten, koska tämä on erillinen reittikokonaisuus
        ApplicationData.routeListList.add(new ArrayList<Route>());
        // myöhemmin keksittävä malli joka tekee uusia listoja oikean määrän suhteessa kokonaisreittien määrään. Nyt aluksi toimitaan kahdella; suora kävelymatka kohteeseen ja matka juna-asemien kautta kohteeseen


        Router router = new Router();
        router.delegate = RoutePresenter.this;

        Log.d("Handler","Creating route 0");

        // tehdään reittiobjekti laitteen lokaatiosta lähimmälle asemalle
        if(ApplicationData.deviceLocationIsOrigin)
        {
            router.getWalkingRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()), foundOriginStation[1]+","+foundOriginStation[2],context,0, 1);
        }
        else
        {
            router.getWalkingRoute(origin, foundOriginStation[1]+","+foundOriginStation[2],context,0, 1);
        }

        // tehdään reittiobjekti lähimmältä asemalta kohdetta lähimmälle asemalle. Ei käytetä routeria koska se etsii reitin teitä pitkin. Halutaan linnuntie alustavasti.
        Log.d("Handler","Creating route 1");
        Double tempLat = Double.parseDouble(foundOriginStation[1]);
        Double tempLng = Double.parseDouble(foundOriginStation[2]);
        LatLng originLatLng = new LatLng(tempLat, tempLng);
        tempLat = Double.parseDouble(foundDestinationStation[1]);
        tempLng = Double.parseDouble(foundDestinationStation[2]);
        LatLng destinationLatLng = new LatLng(tempLat, tempLng);
        // annetaan tälle reitille tyhjät osoitteet ja nollat pituuteen ja kestoon koska se on linnuntie.
        Route stationToStationRoute = new Route(originLatLng, destinationLatLng, "", "", 0,0);
        stationToStationRoute.listIndex = 1;
        stationToStationRoute.index = 1;
        stationToStationRoute.polylineOptions.color(Color.YELLOW);

        try
        {
            ApplicationData.routeListList.add(1,new ArrayList<Route>());
        }
        catch (IndexOutOfBoundsException e)
        {
            Log.d("Handler", "IndexOutOfBoundsException at adding list");
            ApplicationData.routeListList.add(new ArrayList<Route>());
        }
        try
        {
            ApplicationData.routeListList.get(1).add(1,stationToStationRoute);
        }
        catch (IndexOutOfBoundsException e)
        {
            Log.d("Handler", "IndexOutOfBoundsException at adding route");
            ApplicationData.routeListList.get(1).add(stationToStationRoute);
        }
        ShowRouteInMap(stationToStationRoute);
        // tehdään reittiobjekti kohdetta lähimmältä asemalta kohteeseen
        router = new Router();
        router.delegate = RoutePresenter.this;
        Log.d("Handler","Creating route 2");
        router.getWalkingRoute(foundDestinationStation[1]+","+foundDestinationStation[2], destination,context,2, 1);
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

    public void setMapBounds()
    {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < ApplicationData.routeListList.size();i++)
        {
            for(int j = 0;j<ApplicationData.routeListList.get(i).size();i++)
            {
                builder.include(ApplicationData.routeListList.get(i).get(j).origin);
                builder.include(ApplicationData.routeListList.get(i).get(j).destination);
            }
        }

        LatLngBounds bounds = builder.build();

        int padding = 80; // offset from edges of the map in pixels
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        ApplicationData.mMap.animateCamera(cu);
    }

    @Override
    public void atSuspended(int errorCode)
    {
        Log.d("atSuspended", "CONNECTION SUSPENDED IN ROUTEPRESENTER");
    }

    @Override
    public void atConnected(Bundle bundle)
    {
        Log.d("atConnected", "CONNECTED SUCCESFULLY IN ROUTEPRESENTER");
        ApplicationData.startLocationUpdates(this);
        ApplicationData.getLastLocation(this);
    }

    @Override
    public void atConnectionFailed(ConnectionResult connectionResult)
    {
        Log.d("atConnectedFailed", "CONNECTION FAILED IN ROUTEPRESENTER");
    }

    @Override
    public void atLocationChanged(Location location)
    {

    }

    @Override
    public void atMapReady(GoogleMap googleMap)
    {
        Log.d("atMapReady", "MAP READY IN ROUTEPRESENTER");
        ApplicationData.mMap = googleMap;
        ApplicationData.mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ApplicationData.checkLocationPermission(RoutePresenter.this))
        {
            ApplicationData.mMap.setMyLocationEnabled(true);
        }
    }


    // Finds closest train station from given coordinates. Compares to every station within stationData List
    private String[] findClosestStationFromPoint(Location location) {

        String[] closestStationList = new String[3];
        Location pointOrigin = new Location(location);         //////// KLUP jos tyhjä niin kaataa softan
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
        messageList.add("apuva");

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
    public void getWalkingRouteFinish(Route route) {
        // mätetään reitti listaan oikealle paikalleen. jos reitti oli kokonaisreitin viimeinen puuttuva osa, niin piirretään koko reitti kartalle.
        if(ApplicationData.routeListList.size() < route.listIndex)
        {
            Log.d("getWalkingRouteFinish", "List size smaller than intended index at adding list");
            ApplicationData.routeListList.add(new ArrayList<Route>());
        }
        else
        {
            ApplicationData.routeListList.add(route.listIndex, new ArrayList<Route>());
        }

        try
        {
            ApplicationData.routeListList.get(route.listIndex).add(route.index,route);
        }
        catch(IndexOutOfBoundsException e)
        {
            Log.d("getWalkingRouteFinish", "IndexOutOfBoundsException at adding route. Adding to the end of the list.");
            ApplicationData.routeListList.get(route.listIndex).add(route);
        }

        Log.d("getRouteFinish", "ROUTE " + route.index + " CREATED FOR LIST " + route.listIndex);
        ShowRouteInMap(route);

        /*
        listIsComplete = true;
        int i;
        for(i = 0; i<ApplicationData.routeListList.get(route.listIndex).size();i++)
        {
            if (ApplicationData.routeListList.get(route.listIndex).get(i) == null) {
                listIsComplete = false;
            }
        }
        // paska varmistus, tehtävä parempi myöhemmin
        if (i != 2)
        {
            listIsComplete = false;
        }
        if(listIsComplete)
        {
            ShowRouteInMap(ApplicationData.routeListList.get(route.listIndex));
        }*/
    }

    @Override
    public void getBusRouteFinish(Route route)
    {
        // bussireitti valmis, tehdään kävelyteitti lähtöpysäkille lähtöpisteestä
        Router router = new Router();
        router.delegate = this;
        if(ApplicationData.deviceLocationIsOrigin)
        {
            router.getWalkingRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()),String.valueOf(route.origin.latitude)+","+String.valueOf(route.origin.longitude),getBaseContext(),1,2);
        }
        else
        {
            router.getWalkingRoute(origin,String.valueOf(route.origin.latitude)+","+String.valueOf(route.origin.longitude),getBaseContext(),1,2);
        }

        // ja kävelyreitti päätepysäkiltä kohteeseen
        router = new Router();
        router.delegate = this;
        router.getWalkingRoute(String.valueOf(route.destination.latitude)+","+String.valueOf(route.destination.longitude),destination,getBaseContext(),1,2);

        ShowRouteInMap(route);
    }


    // tälle lista koordinaatteja tai osoitteita (tai molempia) joilla kutsutaan forloopissa getRoutea. getRoute tuottaa listan route-objekteja getRouteFinishissä
    /*  public void getFullRoute(List<List<String>> coordinates)
    {

        //String destination = URLEncoder.encode(destinationString);
        //String origin = URLEncoder.encode(originString);
        //Router router = new Router();
        //router.delegate = this;
        //router.getRoute(origin, destination, getBaseContext(), );
        for(int i = 0;i<coordinates.size();i++)
        {
            pendingRoutes++;
            // onko parempi luoda uusi router jokaiselle getRoutelle vai ajaa yhtä router olion getRoutea forloopissa?
            new Router(this).getRoute(coordinates.get(i).get(0), coordinates.get(i).get(1), getBaseContext(), i);
        }
    }*/



    protected void searchDirectTrackConnection(JSONArray json) {
        // Find text block to put data, JUST FOR DEBUGGING. FINAL DATA DERIVED THROUGH BUNDLES!(?)
        TextView textView = (TextView) findViewById(R.id.result1);
       // String text;
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
          //  text = routeData.getString("trainType") + " " + routeData.getString("trainNumber") + "\n"
          //          + routeData.getString("depType") + " " + routeData.getString("depTrack") + " " + routeData.getString("depDate") + " " + routeData.getString("depTime") + "\n"
          //          + routeData.getString("arrType") + " " + routeData.getString("arrTrack") + " " + routeData.getString("arrDate") + " " + routeData.getString("arrTime") + "\n";
           // textView.setText(text);

        } else textView.setText("No direct trains today");
        Log.d("Function called", "Find closest station");

        // MIIKAN TAIKAA
        if (trainData.size() != 0) {
            for(int index = 0; index < trainData.size(); index++) {
                routeList.add(new Route(trainData.get(index)[0], trainData.get(index)[2], trainDataTimeTableDeparture.get(index)[0], trainDataTimeTableDeparture.get(index)[1],
                        trainDataTimeTableDeparture.get(index)[2], trainDataTimeTableDeparture.get(index)[3], trainDataTimeTableArrival.get(index)[0], trainDataTimeTableArrival.get(index)[1],
                        trainDataTimeTableArrival.get(index)[2], trainDataTimeTableArrival.get(index)[3]));
            }
        } else textView.setText("No direct trains today");
        adapter = new routeAdapter(this, routeList);
        recyclerView.setAdapter(adapter);
        //
    }

    // näytetään annettu route-objektin kartalla
    protected void ShowRouteInMap(Route route)
    {
        Log.d("ShowRouteInMap","Drawing route " + route.index + " in list "+ route.listIndex);
        ApplicationData.mMap.addPolyline(route.polylineOptions);
    }
}
