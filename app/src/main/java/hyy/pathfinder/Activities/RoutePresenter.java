package hyy.pathfinder.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.fitness.data.Application;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

import hyy.pathfinder.Adapters.fullRouteAdapter;
import hyy.pathfinder.Data.DataParser;
import hyy.pathfinder.Data.FetchUrl;
import hyy.pathfinder.Interfaces.AppDataInterface;
import hyy.pathfinder.Data.AsyncJsonFetcher;
import hyy.pathfinder.Interfaces.AsyncResponse;
//import hyy.pathfinder.Objects.Route;
import hyy.pathfinder.Objects.Station;
import hyy.pathfinder.Objects.fullRoute;
import hyy.pathfinder.Objects.routeSegment;
import hyy.pathfinder.R;
import hyy.pathfinder.Data.Router;


/**
 * Created by h4211 on 10/11/2016.
 */

// TODO: Keksittävä miten haetaan reittivaihtoehtoja
// TODO: Tehtävä recyclerview, adapteri ja korttinäkymä kokonaisreittivaihtoehdoille

public class RoutePresenter extends AppCompatActivity implements AsyncResponse, AppDataInterface {

    /** Siirrä kaikki muuttujat ApplicationDataan staattisiksi muuttujiksi joita tarvisee kuljettaa aktiviteetista toiseen*/

    // Timeformat used to general time conversion between device and Digitraffic
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
    // Parsed JSONdata from digitraffic

    //private List<String[]> trainData = new ArrayList<>();
    private List<routeSegment> trainData = new ArrayList<>();
    private List<String[]> trainDataTimeTableDeparture = new ArrayList<>();
    private List<String[]> trainDataTimeTableArrival = new ArrayList<>();
    private JSONArray trainJSON;
    //
    private ProgressDialog progressDialog;
    private Context context = this;
    private RecyclerView recyclerView;
    // masterRoute used to save user input

    private GoogleMap fullRouteMap;
    // TODO: Mitä näistä tarvitaan tulevaisuudessa?
    //boolean listIsComplete;
    //private List<Route> routeList = new ArrayList<>();
    //private String origin;
    //private Date originTime;
    //private String originDate;
    //private String destination;
    //private String[] foundOriginStation;
    //private String[] foundDestinationStation;
    //private List<List<Route>> routes = new ArrayList<>();
    //private int pendingRoutes = 0;
    //private String stationStartShortCode = "";
    //private String stationEndShortCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routepresenter);

        InitControls();
        ApplicationData.masterRoute = new fullRoute();
        GetExtras();

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getCustomProgressDialogMessage());
        progressDialog.show();

        // Main logic //////////////
        /// Fetch and parse current train stations from Digitraffic

        Message message = handler.obtainMessage();
        message.what = 0;
        handler.sendMessage(message);
        Log.d("Handler Message", message.toString());
    }

    private void GetExtras()
    {
        // Get bundle data from calling intent
        Bundle extras = getIntent().getExtras();
        if(!ApplicationData.deviceLocationIsOrigin)
        {
            // origin = extras.getString("origin");// street address
            ApplicationData.masterRoute.setOriginAddress(extras.getString("origin")); /////////////////////////////////////////////////
        }

        // originDate = extras.getString("originDate");
        ApplicationData.masterRoute.setOriginDate(extras.getString("originDate")); //////////////////////////////////////////////////////
        try {
            //  originTime = timeFormat.parse(extras.getString("originTime"));
            ApplicationData.masterRoute.setOriginTime(timeFormat.parse(extras.getString("originTime")));////////////////////////////////////
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //destination = extras.getString("destination"); // Street address
        ApplicationData.masterRoute.setDestinationAddress(extras.getString("destination")); ////////////////////////////////////////////////

        if(!ApplicationData.deviceLocationIsOrigin)
        {
            ApplicationData.masterRoute.setOriginLocation(getLocationFromAddress(ApplicationData.masterRoute.getOriginAddress()));
        }
        else
        {
            ApplicationData.masterRoute.setOriginLocation(ApplicationData.mLastLocation);
        }
        ApplicationData.masterRoute.setDestinationLocation(getLocationFromAddress(ApplicationData.masterRoute.getDestinationAddress()));

    }

    private void InitControls()
    {
        recyclerView = (RecyclerView) findViewById(R.id.fullroute_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // asetettava delegaatti callbackeja varten
        ApplicationData.setApplicationDataCallbacksDelegate(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(ApplicationData.applicationDataCallbacks);

        // Asetetaan kuuntelija collapsingToolbarLayoutille, jotta saadaan title piiloon kun kartta tulee esiin

        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        ApplicationData.routePresenterAppBar = (AppBarLayout)findViewById(R.id.appBarLayout);
        ApplicationData.routePresenterAppBar.setExpanded(false);

        ApplicationData.routePresenterAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
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
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch(msg.what){
                case 0:     /// Fetch and parse current train stations from digitraffic
                    Log.d("Handler.what = 0", "Fetching current stationlist");
                    Log.d("JSON query URL","http://rata.digitraffic.fi/api/v1/metadata/stations.json");

                    AsyncJsonFetcher asyncStationFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncStationFetcher.fetchStations("http://rata.digitraffic.fi/api/v1/metadata/stations.json");
                    break;
                case 1:
                    Log.d("Handler.what = 1", "Look for closest stations");
                    findClosestStations(); // Find closest stations from origin and destination after stationDataTask finishes

                    // Get JSON data from digitraffic (between found originStation and destinationStation
                    Log.d("JSON query URL",
                            "http://rata.digitraffic.fi/api/v1/schedules?departure_station=" + ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode()
                                    + "&arrival_station=" + ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode()
                                    + "&departure_date=" + ApplicationData.masterRoute.getOriginDate());



                    AsyncJsonFetcher asyncTrainFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncTrainFetcher.delegate = RoutePresenter.this;
                    asyncTrainFetcher.fetchTrains("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                            + ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode() + "&arrival_station="
                            + ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode() + "&departure_date="
                            + ApplicationData.masterRoute.getOriginDate());

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
                        if (station.getString("passengerTraffic").equals("true")) {
                            // Add data to a two-dimensional array of passenger stations in Finland, including longitude and latitude
                            //stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("latitude"), station.getString("longitude")});
                            ApplicationData.stationData.add((new Station(station.getString("stationName"),station.getString("latitude"), station.getString("longitude"),station.getString("stationUICCode"), station.getString("stationShortCode"),station.getString("type"), station.getString("countryCode"))));
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
                    //createWalkingRoute();
                    //createRoutesUsingStations();
                    //createBusRoute();
                }
                else
                {
                    // Tekee kävelyreitin alkupisteestä kohteeseen
                    Log.d("Handler", "No trains found, only adding walking route");
                    //createWalkingRoute();
                    //createBusRoute();

                }

                // Fetch Trains
                Log.d("AsyncTask finished", "trainDataTask");
                trainJSON = json;

                message.what = 2;
                handler.sendMessage(message);
                Log.d("Handler Message", message.toString());
                break;
            default:
                Log.d("Handler switch", "mode incorrect");
                break;
        }
    }

    protected void CreateMultiTierFullRoutes()
    {
        // YKSI VAIHTO //
        // tehdään lista lähtöasemalta lähtevien junien päämääräasemista.
        // tehdään lista lopulliseen määräänpäähän saapuvien junien lähtöasemista.
        // vertaillaan näitä listoja. Jos molemmissa listoissa on sama asema, niin yhteys on löytynyt.
        // parsi kulkevat junat asemien läpi ja tutki onko junien aikataulu sellainen että niitä pitkin pääsee määränpäähän




        // KAKSI TAI USEAMPI VAIHTOA //
        // tehdään lista ORIGSTATIONS lähtöasemalta lähtevien junien päämääräasemista.
        // tehdään lista DESTSTATIONS lopulliseen määräänpäähän saapuvien junien lähtöasemista.
        // jos listoilla ei ole samoja asemia, niin iteroidaan listan ORIGSTATIONS asemat läpi ja tehdään kolmas lista joka sisältää päämääräasemat jokaiselta ORIGSTATIONS listalla olevan aseman lähtevistä junista.
        // vertaillaan kolmannen listan sisältöä DESTATIONSIIN ja katsotaan onko kummassakaan samoja asemia. Jos on, yhteys löydetty josta voidaan parsia junatiedot.
        // jos ei löydy, niin tehdään neljäs lista joka sisältää listan 3 asemilta lähtevien junien tiedot josta taas katsotaan minne ne menevät ja taas vertaillaan. Toista kunnes löytyy yheys.



    }

    private void findClosestStations() {
        Log.d("findClosestStations", "Started!");
         //Router router = new Router();
        //router.delegate = RoutePresenter.this;
        TrainDataFetcher trainDataFetcher = new TrainDataFetcher();
        Log.d("findClosestStations","Fetching origin");
        if (ApplicationData.deviceLocationIsOrigin) {
            Log.d("findClosestStations","Using device location for origin");

            //foundOriginStation = findClosestStationFromPoint(ApplicationData.mLastLocation); // Find closest train station from origin
            ////////////////////////////////////////////////////////////////////////////
            ApplicationData.masterRoute.setOriginClosestStation(findClosestStationFromPoint(ApplicationData.mLastLocation)); // Find closest train station from origin
            // Get Latitude and Longitude from found closest station and convert into a string
            String originStationLocation = (ApplicationData.masterRoute.getOriginClosestStation().getLatitude() +","+ ApplicationData.masterRoute.getOriginClosestStation().getLongitude());
            String originLocTemp = (ApplicationData.mLastLocation.getLatitude() +","+ ApplicationData.mLastLocation.getLongitude()); // Convert user location from Location to String
            // KLUP tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?!
            trainDataFetcher.GetDistanceAndDuration(originLocTemp, originStationLocation); // Get dist&dur from user location to closest station TODO: Tarvitaanko tätä?
        } else {
            Log.d("findClosestStations","Using other than device location for origin");
            Location mCustomLocation = getLocationFromAddress(ApplicationData.masterRoute.getOriginAddress()); // Convert given address to Location (lat&long)

            //foundOriginStation = findClosestStationFromPoint(mCustomLocation);
            ///////////////////////////////////////////////////////////////////////////////////
            ApplicationData.masterRoute.setOriginClosestStation(findClosestStationFromPoint(mCustomLocation)); // Find closest train station from converted Location
            // Get Latitude and Longitude from found closest station and convert into a string
            String originStationLocation = (ApplicationData.masterRoute.getOriginClosestStation().getLatitude() +","+ ApplicationData.masterRoute.getOriginClosestStation().getLongitude());
            // KLUP tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?!
            trainDataFetcher.GetDistanceAndDuration(ApplicationData.masterRoute.getOriginAddress(), originStationLocation); // Get dist&dur from user location to closest station. TODO: Tarvitaanko tätä?
        }


        Log.d("findClosestStations","Fetching destination");
       // Location destinationLocation = getLocationFromAddress(destination); // Convert given destination address to Location (lat&long)
       // foundDestinationStation = findClosestStationFromPoint(destinationLocation); // Find closest train station from destination
        //////////////////////////////////////////////////////////////////////////
        ApplicationData.masterRoute.setDestinationClosestStation(findClosestStationFromPoint(ApplicationData.masterRoute.getDestinationLocation()));

    }



    // Convert given street address into latitude and longitude
    private Location getLocationFromAddress(String strAddress) {
        Log.d("getLocationFromAddress", "started");
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
        Log.d("Found location", point.toString());
        return point;
    }



    // Finds closest train station from given coordinates. Compares to every station within stationData List
    private Station findClosestStationFromPoint(Location location) {
        //String[] closestStation = new String[3];
        Station closestStation = new Station();
        Location pointOrigin = new Location(location);         //////// KLUP jos tyhjä niin kaataa softan
        Float closestDist = 9999999999999999999f;
//stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("latitude"), station.getString("longitude")});
        for (int i = 0; i < ApplicationData.stationData.size(); i++) {
            Location pointDestination = new Location("pointDestination");
            pointDestination.setLatitude(Double.parseDouble(ApplicationData.stationData.get(i).getLatitude()));
            pointDestination.setLongitude(Double.parseDouble(ApplicationData.stationData.get(i).getLongitude()));
            Float distTemp = pointOrigin.distanceTo(pointDestination);
            if (distTemp < closestDist) {
                closestDist = distTemp;
                closestStation = ApplicationData.stationData.get(i);
            }
        }
        Log.d("Closest station found", closestStation.toString());
        return closestStation;
    }
/*
    // ---------------------------------- NÄMÄ TEKEVÄT ROUTERIA HYÖDYNTÄEN ROUTE-OBJEKTIT --------------------------------- //
    // createWalkingRouter, createBusRoute ja createRoutesUsingStations

    // suora reitti kohteeseen
    public void createWalkingRoute()
    {
        Log.d("createWalkingRoute","Started");
        Router router = new Router();
        router.delegate = this;
        if(ApplicationData.deviceLocationIsOrigin)
        {
            router.getWalkingRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()), masterRoute.getDestinationAddress(), context,0, 0);
        }
        else
        {
            router.getWalkingRoute(masterRoute.getOriginAddress(), masterRoute.getDestinationAddress(), context,0, 0);
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

            router.getBusRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()), masterRoute.getDestinationAddress(), context,0, 2);
        }
        else
        {
            Log.d("createBusRoute", "Using typed start location");

            router.getBusRoute(masterRoute.getOriginAddress(), masterRoute.getDestinationAddress(), context,0, 2);
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
            router.getWalkingRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()), masterRoute.getOriginClosestStation()[1]+","+masterRoute.getOriginClosestStation()[2],context,0, 1);
        }
        else
        {
            router.getWalkingRoute(masterRoute.getOriginAddress(), masterRoute.getOriginClosestStation()[1]+","+masterRoute.getOriginClosestStation()[2],context,0, 1);
        }

        // tehdään reittiobjekti lähimmältä asemalta kohdetta lähimmälle asemalle. Ei käytetä routeria koska se etsii reitin teitä pitkin. Halutaan linnuntie alustavasti.
        Log.d("Handler","Creating route 1");
        Double tempLat = Double.parseDouble(masterRoute.getOriginClosestStation()[1]);
        Double tempLng = Double.parseDouble(masterRoute.getOriginClosestStation()[2]);
        LatLng originLatLng = new LatLng(tempLat, tempLng);
        tempLat = Double.parseDouble(masterRoute.getDestinationClosestStation()[1]);
        tempLng = Double.parseDouble(masterRoute.getDestinationClosestStation()[2]);
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
        router.getWalkingRoute(masterRoute.getDestinationClosestStation()[1]+","+masterRoute.getDestinationClosestStation()[2], masterRoute.getDestinationAddress(), context,2, 1);
    }


*/
    /*public void setMapBounds()
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
    }*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {


        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){

        }
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
        fullRouteMap = googleMap;
        ApplicationData.mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ApplicationData.checkLocationPermission(RoutePresenter.this))
        {
            ApplicationData.mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        ApplicationData.mMap = fullRouteMap;
        ApplicationData.setApplicationDataCallbacksDelegate(this);
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
        return messageList.get(i);
    }


/*

    @Override
    public void getTotalDistanceAndDurationFinish(Route route)
    {
        ApplicationData.masterRoute.setWalkDistanceToOriginStation(KLUP);
        ApplicationData.masterRoute.setWalkDurationToOriginStation(KLUP);
        ApplicationData.masterRoute.setWalkDistanceToOriginStation(KLUP);
        ApplicationData.masterRoute.setWalkDurationToOriginStation(KLUP);


    }



    @Override
    public void getWalkingRouteFinish(Route route) {
        // mätetään reitti listaan oikealle paikalleen. jos reitti oli kokonaisreitin viimeinen puuttuva osa, niin piirretään koko reitti kartalle.
            if (ApplicationData.routeListList.size() < route.listIndex) {
                Log.d("getWalkingRouteFinish", "List size smaller than intended index at adding list");
                ApplicationData.routeListList.add(new ArrayList<Route>());
            } else {
                ApplicationData.routeListList.add(route.listIndex, new ArrayList<Route>());
            }

            try {
                ApplicationData.routeListList.get(route.listIndex).add(route.index, route);
            } catch (IndexOutOfBoundsException e) {
                Log.d("getWalkingRouteFinish", "IndexOutOfBoundsException at adding route. Adding to the end of the list.");
                ApplicationData.routeListList.get(route.listIndex).add(route);
            }

            Log.d("getRouteFinish", "ROUTE " + route.index + " CREATED FOR LIST " + route.listIndex);
            ShowRouteInMap(route);

    }

    @Override
    public void getBusRouteFinish(Route route)
    { // TODO: miten toi valueof toimii? pitäs muuttaa Appdatan sisään arvot.
        // TODO: JOS KÄYTTÄÄ getWalkingRoutea tässä, niin softa kaatuu!
        // bussireitti valmis, tehdään kävelyteitti lähtöpysäkille lähtöpisteestä
        Router router = new Router();
        router.delegate = this;
        if(ApplicationData.deviceLocationIsOrigin)
        {
         //   router.getWalkingRoute(String.valueOf(ApplicationData.mLastLocation.getLatitude())+","+String.valueOf(ApplicationData.mLastLocation.getLongitude()),String.valueOf(route.origin.latitude)+","+String.valueOf(route.origin.longitude),getBaseContext(),1,2);
        }
        else
        {
         //   router.getWalkingRoute(origin,String.valueOf(route.origin.latitude)+","+String.valueOf(route.origin.longitude),getBaseContext(),1,2);
        }

        // ja kävelyreitti päätepysäkiltä kohteeseen
        router = new Router();
        router.delegate = this;
      //  router.getWalkingRoute(String.valueOf(route.destination.latitude)+","+String.valueOf(route.destination.longitude),destination,getBaseContext(),1,2);

      //  ShowRouteInMap(route);
    }

*/

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
        Log.d("searchDirectTrackConn..", "started");
        // Find text block to put data, JUST FOR DEBUGGING. FINAL DATA DERIVED THROUGH BUNDLES!(?)
        TextView textView = (TextView) findViewById(R.id.result1);
        String[] scheduledTimeTemp;
        String[] scheduledDate;
        List<LatLng> trainRoute;
        boolean stationIsRelevant = false;
        List<String> stationShortCodes = new ArrayList<>(); // junareitin karttaan piirtämistä varten

        // ----------DEV--------- Get all trains from station (start-end)
        try {
            // First get all trains from current station to destination
            for (int i = 0; i < json.length(); i++) {
                JSONObject train = json.getJSONObject(i);
                JSONArray timeTable = train.getJSONArray("timeTableRows");
                for (int y = 0; y < timeTable.length(); y++) {
                    JSONObject tt = timeTable.getJSONObject(y);
                    if(stationIsRelevant && !stationShortCodes.contains(tt.getString("stationShortCode")))
                    {
                        Log.d("Adding short code,loop " + y, tt.getString("stationShortCode"));
                        stationShortCodes.add(tt.getString("stationShortCode"));
                    }
                    if (tt.getString("stationShortCode").equals(ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode()) // [0] = stationshortcode
                            && tt.getString("type").equals("DEPARTURE")) {
                        stationIsRelevant = true;
                        stationShortCodes.add(tt.getString("stationShortCode"));
                        Log.d("Adding short code,loop " + y, tt.getString("stationShortCode"));
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

                    } else if (tt.getString("stationShortCode").equals(ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode())
                            && tt.getString("type").equals("ARRIVAL")) {
                        stationIsRelevant = false;
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

                //trainData.add(new String[]{train.getString("trainNumber"), train.getString("departureDate"), train.getString("trainType")});
                trainRoute = new ArrayList<>();

                for(int x = 0;x<stationShortCodes.size();x++)
                {
                    //Log.d("searchDirectTrackConn", ""+stationShortCodes.size());

                    // tarvitsee uudemman api levelin
                    //ApplicationData.stationData.stream().filter(o -> o.getStationShortCode().equals(stationShortCodes.get(1))).findFirst().isPresent();

                    LatLng tempLatLng = ApplicationData.stationData.GetLatLng(stationShortCodes.get(x));
                    if(tempLatLng != null)
                    {
                        trainRoute.add(tempLatLng);
                    }
                }
                String trainNumber = String.valueOf(train.getInt("trainNumber"));
                String trainType = train.getString("trainType");
                trainData.add(new routeSegment(trainRoute, trainNumber, trainType));
                stationShortCodes.clear();
            }

            // Next remove all trains before selected time of day, uses iterator to go trough data
            Date timeTemp;
            Iterator<String[]> i = trainDataTimeTableDeparture.iterator();
            while (i.hasNext()) {
                String[] s = i.next();
                timeTemp = timeFormat.parse(s[3]);
                if (ApplicationData.masterRoute.getOriginTime().after(timeTemp)) {
                    i.remove(); // removes trainDataTimeTableDeparture(0)
                    trainData.remove(0);
                    trainDataTimeTableArrival.remove(0);
                }
            }

            if (trainData != null) {
                createFullRouteObjects();
            } else {
                Log.d("directTrackConnection", "Couldn't find any suitable trains!");
                textView.setText("No direct trains today");
                // TODO: siirrä haku seuraavalle päivälle?
            }

            Log.d("RecyclerView", "called");
            final RecyclerView.Adapter adapter = new fullRouteAdapter(this, ApplicationData.fullRouteList);
            recyclerView.setAdapter(adapter);
            AddItemTouchHelper(adapter);


            /// ------- DEV ------ Null box. Use this to show data, debugging purposes
            //TextView ruutu = (TextView) findViewById(R.id.test);
            //ruutu.setText("Set depTime: " + masterRoute.originTime + "\n");


        } catch (Exception e) { // JSONException?
            // If no straight trains found
            Log.d("No trains found", e.toString());
            // --------DEV---------- KLUP now program should start looking for alternate routes (via stop/multiple stops)
        }
    }

    protected void AddItemTouchHelper(final RecyclerView.Adapter rAdapter)
    {
        final Paint p = new Paint();
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if(swipeDir == ItemTouchHelper.LEFT)
                {
                    // remove data
                    ApplicationData.fullRouteList.remove(viewHolder.getAdapterPosition());
                    // update adapter
                    rAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                }
                else if (swipeDir == ItemTouchHelper.RIGHT)
                {
                    Intent intent = new Intent(context, segmentPresenter.class);
                    intent.putExtra("route", ApplicationData.fullRouteList.get(viewHolder.getAdapterPosition()));
                    rAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    ApplicationData.selectedRoute = ApplicationData.fullRouteList.get(viewHolder.getAdapterPosition());
                    startActivityForResult(intent, 1); // second parameter is a filler. startActivityForResult called to make use of onActivityResult()
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                Bitmap icon;
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){

                    View itemView = viewHolder.itemView;
                    float height = (float) itemView.getBottom() - (float) itemView.getTop();
                    float width = height / 3;

                    if(dX > 0){
                        p.setColor(Color.parseColor("#388E3C"));
                        RectF background = new RectF((float) itemView.getLeft(), (float) itemView.getTop(), dX,(float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.zoom);
                        RectF icon_dest = new RectF((float) itemView.getLeft() + width ,(float) itemView.getTop() + width,(float) itemView.getLeft()+ 2*width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                    } else {
                        p.setColor(Color.parseColor("#D32F2F"));
                        RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                        c.drawRect(background,p);
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.trash_can);
                        RectF icon_dest = new RectF((float) itemView.getRight() - 2*width ,(float) itemView.getTop() + width,(float) itemView.getRight() - width,(float)itemView.getBottom() - width);
                        c.drawBitmap(icon,null,icon_dest,p);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
            }
        };

        // connect item touch helper to recycler view
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }



    private void createFullRouteObjects() {
        // TODO: trainData,trainDataTimeTableArrival, trainDataTimeTableDeparture jokaisesta paikallinen muuttuja(?) ja siitä työnnetään objekteihin
        ApplicationData.fullRouteList = new ArrayList<>();
        try
        {
            Log.d("trainData", "Size " + trainData.size());
            if (trainData != null) {
                for (int index = 0; index < trainData.size(); index++) {
                    Log.d("Current index", Integer.toString(index));
                    Log.d("Create new route", "index " + index);
                    // Copy masterRoute and put copy into arraylist
                    fullRoute route = new fullRoute(ApplicationData.masterRoute);
                    ApplicationData.fullRouteList.add(route);
                    ApplicationData.fullRouteList.get(index).setOriginDate(Integer.toString(index)); // TODO: welp?

                    Log.d("Add data to segment", Integer.toString(index));
                    Log.d("createFullRouteObjects", "Tehdään kävelyetappi 1");
                    // Kävele asemalle ensin
                    ApplicationData.fullRouteList.get(index).addRouteSegment();
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(0).setDepTime("Tässä minä kävelen juna-asemalle");
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(0).setOrigin(new LatLng(ApplicationData.masterRoute.getOriginLocation().getLatitude(),ApplicationData.masterRoute.getOriginLocation().getLongitude()));
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(0).setDestination(new LatLng(Double.parseDouble(ApplicationData.masterRoute.getOriginClosestStation().getLatitude()),Double.parseDouble(ApplicationData.masterRoute.getOriginClosestStation().getLongitude())));
                    Log.d("createFullRouteObjects", "Building polylineoptions");
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(0).BuildPolylineOptions(getBaseContext());
                    Log.d("createFullRouteObjects ", "Tehdään junaetappia");
                    // Junatiedot
                    ApplicationData.fullRouteList.get(index).addRouteSegment(trainData.get(index));
                    //fullRouteList.get(index).routeSegmentList.get(1).setTrainNumber(trainData.get(index).getTrainNumber());
                    //fullRouteList.get(index).routeSegmentList.get(1).setTrainType(trainData.get(index).getTrainType());
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setDepType(trainDataTimeTableDeparture.get(index)[0]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setDepTrack(trainDataTimeTableDeparture.get(index)[1]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setDepDate(trainDataTimeTableDeparture.get(index)[2]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setDepTime(trainDataTimeTableDeparture.get(index)[3]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setArrType(trainDataTimeTableArrival.get(index)[0]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setArrTrack(trainDataTimeTableArrival.get(index)[1]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setArrDate(trainDataTimeTableArrival.get(index)[2]);
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).setArrTime(trainDataTimeTableArrival.get(index)[3]);
                    Log.d("createFullRouteObjects", "Building polylineoptions for traintrip");
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(1).BuildPolylineOptions(getBaseContext());
                    Log.d("createFullRouteObjects", "Tehdään kävelyetappi 2");
                    // Kävele asemalta kohteeseen
                    ApplicationData.fullRouteList.get(index).addRouteSegment();
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(2).setArrTime("Tässä minä kävelen asemalta kohteeseen");
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(2).setOrigin(new LatLng(Double.parseDouble(ApplicationData.masterRoute.getDestinationClosestStation().getLatitude()),Double.parseDouble(ApplicationData.masterRoute.getDestinationClosestStation().getLongitude())));
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(2).setDestination(new LatLng(ApplicationData.masterRoute.getDestinationLocation().getLatitude(),ApplicationData.masterRoute.getDestinationLocation().getLongitude()));
                    Log.d("createFullRouteObjects", "Building polylineoptions");
                    ApplicationData.fullRouteList.get(index).routeSegmentList.get(2).BuildPolylineOptions(getBaseContext());
                }
            }
        }
        catch(Exception e)
        {
            Log.d( "createFullRouteObjects" , e.toString());
        }
    }


    // TODO: Täytyy uudelleennimetä
    protected class TrainDataFetcher extends AsyncTask<String, Void, Void>
    {
        @Override
        public Void doInBackground(String... data)
        {
            FetchUrl fUrl = new FetchUrl();
            try
            {
                String jsonData = fUrl.downloadUrl(data[0]);
                JSONObject jsonObject = new JSONObject(jsonData);
                DataParser dataParser = new DataParser();

                // strinlist sisältää etäisyyden ja keston.
                List<String> stringList = dataParser.parseTotalDistanceAndDuration(jsonObject);


            }
            catch(IOException e)
            {
                Log.d("IOEXCEPTION", "In TrainDataFetcher");
            }
            catch (JSONException e)
            {
                Log.d("JSONEXCEPTION", "In TrainDataFetcher");
            }
            return null;
        }

        public void GetDistanceAndDuration(String origin, String destination)
        {
            String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&mode=walking&key=" + context.getResources().getString(R.string.google_maps_key);
            this.execute(urlString);
        }
    }
}
