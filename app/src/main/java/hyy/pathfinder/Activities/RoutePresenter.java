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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import hyy.pathfinder.Adapters.fullRouteAdapter;
import hyy.pathfinder.Data.ApplicationData;
import hyy.pathfinder.Data.DataParser;
import hyy.pathfinder.Data.FetchUrl;
import hyy.pathfinder.Data.LocationPermissionAgent;
import hyy.pathfinder.Interfaces.AppDataInterface;
import hyy.pathfinder.Data.AsyncJsonFetcher;
import hyy.pathfinder.Interfaces.AsyncResponse;
//import hyy.pathfinder.Objects.Route;
import hyy.pathfinder.Objects.Station;
import hyy.pathfinder.Objects.Train;
import hyy.pathfinder.Objects.fullRoute;
import hyy.pathfinder.Objects.routeSegment;
import hyy.pathfinder.R;


/**
 * Created by h4211 on 10/11/2016.
 */

// TODO: Keksittävä miten haetaan reittivaihtoehtoja
// TODO: Tehtävä recyclerview, adapteri ja korttinäkymä kokonaisreittivaihtoehdoille

public class RoutePresenter extends AppCompatActivity implements AsyncResponse, AppDataInterface {

    /** Siirrä kaikki muuttujat ApplicationDataan staattisiksi muuttujiksi joita tarvisee kuljettaa aktiviteetista toiseen*/

    // Timeformat used to general time conversion between device and Digitraffic
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

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

    private List<JSONArray> multiTierTimeTables = new ArrayList<>();
    private List<List<Train>> stationTrains;
    private List<fullRoute> fullRouteList;


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
        fullRouteList = new ArrayList<>(); // Todo do I belong here..?
        stationTrains = new ArrayList<>();

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
            ApplicationData.masterRoute.setOriginAddress(extras.getString("origin"));
        }

        ApplicationData.masterRoute.setOriginDate(extras.getString("originDate"));
        try {
            ApplicationData.masterRoute.setOriginTime(timeFormat.parse(extras.getString("originTime")));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        ApplicationData.masterRoute.setDestinationAddress(extras.getString("destination"));

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
                    // Direct routes here
                    Log.d("JSON query URL",
                            "http://rata.digitraffic.fi/api/v1/schedules?departure_station=" + ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode()
                                    + "&arrival_station=" + ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode()
                                    + "&departure_date=" + ApplicationData.masterRoute.getOriginDate());

                    // Fetch direct routes
                    AsyncJsonFetcher asyncTrainFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    asyncTrainFetcher.delegate = RoutePresenter.this;
                    asyncTrainFetcher.fetchDirectTrains("http://rata.digitraffic.fi/api/v1/schedules?departure_station="
                            + ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode() + "&arrival_station="
                            + ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode() + "&departure_date="
                            + ApplicationData.masterRoute.getOriginDate());

                    // Indirect routes below (timetable queries)
                    AsyncJsonFetcher originStationTimeTableJsonFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    originStationTimeTableJsonFetcher.delegate = RoutePresenter.this;
                    originStationTimeTableJsonFetcher.fetchStationTimeTables("https://rata.digitraffic.fi/api/v1/live-trains?station="
                            + ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode() + "&departing_trains=20&include_nonstopping=false");

                    AsyncJsonFetcher destinationStationTimeTableJsonFetcher = new AsyncJsonFetcher(RoutePresenter.this);
                    destinationStationTimeTableJsonFetcher.delegate = RoutePresenter.this;
                    destinationStationTimeTableJsonFetcher.fetchStationTimeTables("https://rata.digitraffic.fi/api/v1/live-trains?station="
                            + ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode() + "&arriving_trains=20&include_nonstopping=false");
                    break;
                case 2: // After fetching direct route JSON
                    Log.d("Handler.what = 2", "Searching for direct track connections between found stations");
                    searchDirectTrackConnection(trainJSON);
                    break;
                case 3: // After fetching timetables for indirect routes
                    createTrainObjects();
                    //searchIndirectTrackConnection();
                  //  setAdapter();
                  //  progressDialog.dismiss();
                    break;
                case 4:
                    if (trainData != null) {
                        createFullRouteObjects();
                    } else {
                        Log.d("directTrackConnection", "Couldn't find any suitable trains!");
                        //textView.setText("No direct trains today");
                        // TODO: siirrä haku seuraavalle päivälle?
                    }
                    break;
                case 5:
                    setAdapter();
                    progressDialog.dismiss();
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
                        //if (station.getString("passengerTraffic").equals("true")) {
                            // Add data to a two-dimensional array of passenger stations in Finland, including longitude and latitude
                            //stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("latitude"), station.getString("longitude")});
                            ApplicationData.stationData.add((new Station(station.getString("stationName"),station.getString("latitude"), station.getString("longitude"),station.getString("stationUICCode"), station.getString("stationShortCode"),station.getString("type"), station.getString("countryCode"), station.getBoolean(("passengerTraffic")))));
                        //}
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
                    Log.d("Handler", "Trains found, creating routes");
                    trainJSON = json;
                    message.what = 2;
                    handler.sendMessage(message);
                    Log.d("Handler Message", message.toString());
                }
                else
                {
                    Log.d("Handler", "No trains found, unable to create routes");
                    //progressDialog.dismiss();
                    Toast.makeText(this, "Suoria junia ei löytynyt, haetaan epäsuorat", Toast.LENGTH_SHORT).show();
                }
                break;
            case 3:
                Log.d("Handler","Creating multitiertimetable-arraylist");
                if (!jsonException) {
                    multiTierTimeTables.add(json);
                } else {
                    Log.d("multiTierTimeTables", "Something went wrong..");
                }
                message.what = 3;
                handler.sendMessage(message);
                break;
            default:
                Log.d("Handler switch", "mode incorrect");
                break;
        }
    }



//private List<Train> originStationTrains = new ArrayList<>();
    protected void createTrainObjects() {
        Log.d("Called", "createTrainObjects!");
        int size = multiTierTimeTables.size();
        Log.d("multiTT size", Integer.toString(size));
        // Check if there's at least 2 timetables available (origin & destination)
        if (multiTierTimeTables.size() >= 2) {
            for (int iterator = 0; iterator < multiTierTimeTables.size(); iterator++) {
                Log.d("createTrainObjects", "iterator " + Integer.toString(iterator));
                JSONObject train;
                JSONArray trainData = multiTierTimeTables.get(iterator);
                // Check if trainData is not null
                if (trainData != null) {
                    // Create new object-arraylist inside arraylist
                    stationTrains.add(new ArrayList<Train>());
                    for (int i = 0; i < trainData.length(); i++) {
                        try {
                            train = trainData.getJSONObject(i);
                            JSONArray timeTableRows = train.getJSONArray("timeTableRows");
                            // Accept only commercial trains
                            if (train.getString("trainCategory").matches("Commuter|Locomotive|Long-distance")) {
                                // Create new Train object and add data to it
                                int position = stationTrains.size() - 1;
                                stationTrains.get(position).add(new Train());
                                int inner_position = stationTrains.get(position).size() - 1;

                                stationTrains.get(position).get(inner_position).setTrainNumber(train.getInt("trainNumber"));
                                stationTrains.get(position).get(inner_position).setDepartureDate(train.getString("departureDate"));
                                stationTrains.get(position).get(inner_position).setOperatorUICCode(train.getInt("operatorUICCode"));
                                stationTrains.get(position).get(inner_position).setOperatorShortCode(train.getString("operatorShortCode"));
                                stationTrains.get(position).get(inner_position).setTrainType(train.getString("trainType"));
                                stationTrains.get(position).get(inner_position).setTrainCategory(train.getString("trainCategory"));
                                stationTrains.get(position).get(inner_position).setCommuterLineID(train.getString("commuterLineID"));
                                stationTrains.get(position).get(inner_position).setRunningCurrently(train.getBoolean("runningCurrently"));
                                stationTrains.get(position).get(inner_position).setCancelled(train.getBoolean("cancelled"));
                                stationTrains.get(position).get(inner_position).setVersion(train.getInt("version"));

                                // Add data to TrainTimeTables-arraylist object within Train object
                                for (int x = 0; x < timeTableRows.length(); x++) {
                                    JSONObject tt = timeTableRows.getJSONObject(x);

                                    if (tt.getBoolean("trainStopping") && tt.getBoolean("commercialStop")) {

                                        stationTrains.get(position).get(inner_position).createTimeTableRow();
                                        int index = stationTrains.get(position).get(inner_position).timeTableRows.size() -1;
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setStationShortCode(tt.getString("stationShortCode"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setStationUICCode(Integer.parseInt(tt.getString("stationUICCode")));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setCountryCode(tt.getString("countryCode"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setType(tt.getString("type"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setTrainStopping(tt.getBoolean("trainStopping"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setCommercialStop(tt.getBoolean("commercialStop"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setCommercialTrack(tt.getString("commercialTrack"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setCancelled(tt.getBoolean("cancelled"));
                                        stationTrains.get(position).get(inner_position).timeTableRows.get(index).setScheduledTime(tt.getString("scheduledTime"));
                                    }

                                    // lisätään junalle tiedot kaikista asemista joidenka läpi kuljetaan, jotta saadaan piirrettyä reitti kartalle
                                    LatLng stationCoordinates = ApplicationData.stationData.GetLatLng(tt.getString("stationShortCode"));
                                    String stationLatitude = String.valueOf(stationCoordinates.latitude);
                                    String stationLongitude = String.valueOf(stationCoordinates.longitude);
                                    stationTrains.get(position).get(inner_position).trainRouteStations.add(new Station(stationLatitude, stationLongitude, tt.getString("stationUICCode"), tt.getString("stationShortCode"), tt.getString("type"),tt.getString("countryCode")));
                                }
                            }
                        } catch (JSONException e) {
                            Log.d("JSONException", "createTrainObjects!");
                            e.printStackTrace();
                        }
                        // Log.d("Trainobject", "created");
                    }
                    Log.d("TrainArraylist", "created");
                }
                }
            searchIndirectTrackConnection();
        }
    }



    protected void searchIndirectTrackConnection() {
        //Log.d("Called", "searchIndirectTrackConnection!");
        try{
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                List<String> departireTimes = new ArrayList<String>();
                String stationShortCode;
                String station2ShortCode;
                routeSegment station1 = new routeSegment();
                routeSegment station2 = new routeSegment();
                String[] datetime;
                boolean originFound = false;
                List<LatLng> trackCoordinates;
                Station tempStation;
                boolean stationIsRelevant;

                // For route time comparison
                Date route1;
                Date route2;

                boolean runsDirectly;


                // Käydään läpi kaikki valitun aseman junat
                for (int i1 = 0; i1 < stationTrains.get(0).size(); i1++) {
                    originFound = false;

                        // Käydään läpi kaikki valitun junan aikataululistan pysäkit
                        for (int i2 = 0; i2 < stationTrains.get(0).get(i1).timeTableRows.size(); i2++) {
                            // Hyväksy aikatauluista vain lähtöpaikan jälkeiset stopit
                            if (originFound) {
                                stationShortCode = stationTrains.get(0).get(i1).timeTableRows.get(i2).getStationShortCode();

                                // Käydään läpi kohdeaseman junat
                                for (int x0 = 0; x0 < stationTrains.get(1).size(); x0++) {
                                    // Käydään läpi valitun junan aikataulut
                                    for (int x1 = 0; x1 < stationTrains.get(1).get(x0).timeTableRows.size(); x1++) {
                                        station2ShortCode = stationTrains.get(1).get(x0).timeTableRows.get(x1).getStationShortCode();
                                        String[] routeTimeDate1 = convertTime(stationTrains.get(0).get(i1).timeTableRows.get(i2).getScheduledTime());
                                        route1 = convertStringToDate(routeTimeDate1[1]);
                                        String[] routeTimeDate2 = convertTime(stationTrains.get(1).get(x0).timeTableRows.get(x1).getScheduledTime());
                                        route2 = convertStringToDate(routeTimeDate2[1]);

                                        int time1 = Integer.parseInt(routeTimeDate1[1].substring(0, 2));
                                        int time2 = Integer.parseInt(routeTimeDate2[1].substring(0, 2));
                                        int timeBetween = time2 - time1;

                                        Date timeTemp = convertStringToDate(routeTimeDate1[1]);

                                        // vertailu
                                        if (stationTrains.get(0).get(i1).timeTableRows.get(i2).getType().equals("DEPARTURE")
                                                && stationTrains.get(1).get(x0).timeTableRows.get(x1).getType().equals("ARRIVAL")
                                                && stationShortCode.equals(station2ShortCode)
                                                && route1.before(route2)
                                                && timeBetween <= 2) {

                                            //Log.d("indirect route", "match found between origTrain " + i1 + " and destTrain " + x0);
                                          //  Log.d("SHORTCODE", stationShortCode);
                                          //  Log.d("SHORTCODE", station2ShortCode);
                                            //Log.d("timeBetween", Integer.toString(timeBetween));
                                          //  Log.d("TIME", route1.toString());
                                           // Log.d("TIME", route2.toString());
                                            trackCoordinates = new ArrayList<LatLng>();
                                            stationIsRelevant = false;

                                            // tarkistetaan duplikaattien varalta
                                            if(departireTimes.contains(stationTrains.get(0).get(i1).timeTableRows.get(i2).getScheduledTime())){
                                                Log.d("FOUND DUPLICATE!", "BREAKING"); break;}
                                            else {departireTimes.add(stationTrains.get(0).get(i1).timeTableRows.get(i2).getScheduledTime());}

                                            if(departireTimes.contains(stationTrains.get(1).get(x0).timeTableRows.get(x1).getScheduledTime())){
                                                Log.d("FOUND DUPLICATE!", "BREAKING"); break;}
                                            else {departireTimes.add(stationTrains.get(1).get(x0).timeTableRows.get(x1).getScheduledTime());}


                                            // iteroidaan juna-asemat ja seulotaan sieltä käyttäjälle oleelliset koordinaatit talteen piirrettäväksi. Lähtöaseman jälkeen haetaan kaikki uniikit koordinaatit kunnes löytyy käyttäjän päätepysäkki
                                            for(int j = 0;j<stationTrains.get(0).get(i1).trainRouteStations.size();j++)
                                            {
                                                tempStation = stationTrains.get(0).get(i1).trainRouteStations.get(j);
                                                // tarkista onko iteroitava lähtöasema ja onko se jo listassa
                                                if(tempStation.getStationShortCode().equals(ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode())  && !trackCoordinates.contains(new LatLng(Double.parseDouble(tempStation.getLatitude()),Double.parseDouble(tempStation.getLongitude()))))
                                                {
                                                    Log.d("asd", "FOUND FIRST STATION! " + tempStation.getStationShortCode());
                                                    stationIsRelevant = true;
                                                    trackCoordinates.add(new LatLng(Double.parseDouble(tempStation.getLatitude()), Double.parseDouble(tempStation.getLongitude())));
                                                }

                                                // tarkista onko löytynyt jo käyttäjän lähtöasema ja onko koordinaatti jo listassa
                                                else if(stationIsRelevant && !trackCoordinates.contains(new LatLng(Double.parseDouble(tempStation.getLatitude()),Double.parseDouble(tempStation.getLongitude()))))
                                                {
                                                  //  Log.d("asd", "FOUND RELEVANT STATION! " + tempStation.getStationShortCode());
                                                    trackCoordinates.add(new LatLng(Double.parseDouble(tempStation.getLatitude()), Double.parseDouble(tempStation.getLongitude())));
                                                }

                                                // tarkista onko koordinaatti käyttäjän pysäkin koordinaatti. Jos on, se on viimeinen ja looppi voidaan katkaista.
                                                if(tempStation.getStationShortCode().equals(stationTrains.get(0).get(i1).timeTableRows.get(i2).getStationShortCode()))
                                                {
                                                    Log.d("asd", "FOUND LAST STATION! " + tempStation.getStationShortCode());
                                                    break;
                                                }
                                            }

                                            // jos ei löydy lähtöasemaa listasta jostain syystä, skipataan fullrouten tekeminen
                                            if(!stationIsRelevant)
                                            {
                                                break;
                                            }
                                            // Train route from origin station, stop data of point where trains collide
                                            datetime = convertTime(stationTrains.get(0).get(i1).timeTableRows.get(i2).getScheduledTime());
                                            station1.setArrTime(datetime[1]);
                                            station1.setArrTrack(stationTrains.get(0).get(i1).timeTableRows.get(i2).getCommercialTrack());
                                            station1.setDestination(ApplicationData.stationData.GetLatLng(stationTrains.get(0).get(i1).timeTableRows.get(i2).getStationShortCode()));
                                            station1.setTrainTrackData(trackCoordinates);
                                            station1.IsTrainSegment(true);
                                            trackCoordinates = new ArrayList<LatLng>();
                                            stationIsRelevant = false;
                                            // Second station

                                            // iteroidaan juna-asemat ja seulotaan sieltä käyttäjälle oleelliset koordinaatit talteen piirrettäväksi. Lähtöaseman jälkeen haetaan kaikki uniikit koordinaatit kunnes löytyy käyttäjän päätepysäkki
                                            for(int j = 0;j<stationTrains.get(1).get(x0).trainRouteStations.size();j++)
                                            {
                                                tempStation = stationTrains.get(1).get(x0).trainRouteStations.get(j);
                                                // tarkista onko iteroitava lähtöasema ja onko se jo listassa
                                                if(tempStation.getStationShortCode().equals(stationTrains.get(1).get(x0).timeTableRows.get(x1).getStationShortCode())  && !trackCoordinates.contains(new LatLng(Double.parseDouble(tempStation.getLatitude()),Double.parseDouble(tempStation.getLongitude()))))
                                                {
                                                    Log.d("asd", "FOUND FIRST STATION! " + tempStation.getStationShortCode());
                                                    stationIsRelevant = true;
                                                    trackCoordinates.add(new LatLng(Double.parseDouble(tempStation.getLatitude()), Double.parseDouble(tempStation.getLongitude())));
                                                }
                                                // tarkista onko löytynyt jo käyttäjän lähtöasema ja onko koordinaatti jo listassa
                                                  else if(stationIsRelevant && !trackCoordinates.contains(new LatLng(Double.parseDouble(tempStation.getLatitude()), Double.parseDouble(tempStation.getLongitude()))))
                                                {
                                                    //Log.d("asd", "FOUND RELEVANT STATION! " + tempStation.getStationShortCode());
                                                    trackCoordinates.add(new LatLng(Double.parseDouble(tempStation.getLatitude()), Double.parseDouble(tempStation.getLongitude())));
                                                }
                                                // tarkista onko koordinaatti käyttäjän pysäkin koordinaatti. Jos on, se on viimeinen ja looppi voidaan katkaista.
                                                if(tempStation.getStationShortCode().equals(ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode()))
                                                {
                                                    Log.d("asd", "FOUND LAST STATION! " + tempStation.getStationShortCode());
                                                    break;
                                                }
                                            }

                                            // jos ei löydy lähtöasemaa listasta jostain syystä, skipataan fullrouten tekeminen
                                            if(!stationIsRelevant)
                                            {
                                                break;
                                            }


                                            datetime = convertTime(stationTrains.get(1).get(x0).timeTableRows.get(x1).getScheduledTime());
                                            station2.setDepTime(datetime[1]);
                                            station2.setDepTrack(stationTrains.get(1).get(x0).timeTableRows.get(x1).getCommercialTrack());
                                            station2.setDepType(stationTrains.get(1).get(x0).timeTableRows.get(x1).getType());
                                            station2.setOrigin(ApplicationData.stationData.GetLatLng(stationTrains.get(1).get(x0).timeTableRows.get(x1).getStationShortCode()));
                                            station2.setTrainTrackData(trackCoordinates);
                                            station2.IsTrainSegment(true);

                                            // Instantly search for destination point data to create card
                                            int[] values = findDestinationTimeTable(1, x0);
                                            datetime = convertTime(stationTrains.get(values[0]).get(values[1]).timeTableRows.get(values[2]).getScheduledTime());
                                            station2.setArrTime(datetime[1]);
                                            station2.setArrTrack(stationTrains.get(values[0]).get(values[1]).timeTableRows.get(values[2]).getCommercialTrack());
                                            station2.setDestination(ApplicationData.stationData.GetLatLng(stationTrains.get(values[0]).get(values[1]).timeTableRows.get(values[2]).getStationShortCode()));
                                            createIndirectFullRouteObjects(station1, station2);
                                        }
                                    }
                                }
                            }
                            // Tarkista origin vasta lopussa, ettei vertaa originasemaa.
                            if (stationTrains.get(0).get(i1).timeTableRows.get(i2).getStationShortCode().equals(ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode())) {
                                originFound = true;
                                datetime = convertTime(stationTrains.get(0).get(i1).timeTableRows.get(i2).getScheduledTime());
                                station1.setDepTime(datetime[1]);
                                station1.setDepTrack(stationTrains.get(0).get(i1).timeTableRows.get(i2).getCommercialTrack());
                                station1.setDepType(stationTrains.get(0).get(i1).timeTableRows.get(i2).getType());
                                station1.setOrigin(ApplicationData.stationData.GetLatLng(stationTrains.get(0).get(i1).timeTableRows.get(i2).getStationShortCode()));
                            }

                        }
                    }
                Message message = handler.obtainMessage();
                message.what = 5;
                handler.sendMessage(message);

                return null;
            }
        }.execute();
        }
        catch(Exception e)
        {
            Log.d("Exception", "searchIndirectTrackConnection!");
            e.printStackTrace();
        }
    }



    private void createIndirectFullRouteObjects(routeSegment station1, routeSegment station2) {
        // Copy masterRoute and put copy into arraylist
        fullRoute route = new fullRoute(ApplicationData.masterRoute);

        Log.d("Called", "createIndirectFullRouteObjects");
        // Create walking segment
        route.addRouteSegment();
        int iterator = route.routeSegmentList.size() - 1;
        route.routeSegmentList.get(iterator).setDepTime("Tässä minä kävelen juna-asemalle");
        route.routeSegmentList.get(iterator).setOrigin(new LatLng(ApplicationData.masterRoute.getOriginLocation().getLatitude(), ApplicationData.masterRoute.getOriginLocation().getLongitude()));
        route.routeSegmentList.get(iterator).setDestination(new LatLng(Double.parseDouble(ApplicationData.masterRoute.getOriginClosestStation().getLatitude()),Double.parseDouble(ApplicationData.masterRoute.getOriginClosestStation().getLongitude())));
        route.routeSegmentList.get(iterator).BuildPolylineOptions(getBaseContext());

        // Create first stop segment
        route.addRouteSegment();
        iterator = route.routeSegmentList.size() - 1;
        route.routeSegmentList.get(iterator).setTrainTrackData(station1.getTrainTrackData());
        route.routeSegmentList.get(iterator).IsTrainSegment(true);
        route.routeSegmentList.get(iterator).setDepTime(station1.getDepTime());
        route.setOriginDate(station1.getDepDate());
        route.routeSegmentList.get(iterator).setDepType(station1.getDepType());
        route.routeSegmentList.get(iterator).setDepTrack(station1.getDepTrack());
        route.routeSegmentList.get(iterator).setArrTrack(station1.getArrTrack());
        route.routeSegmentList.get(iterator).setArrTime(station1.getArrTime());
        route.routeSegmentList.get(iterator).setOrigin(station1.getOrigin());
        route.routeSegmentList.get(iterator).setDestination(station1.getDestination());
        route.routeSegmentList.get(iterator).BuildPolylineOptions(getBaseContext());

        // Create second stop segment
        route.addRouteSegment();
        iterator = route.routeSegmentList.size() - 1;
        route.routeSegmentList.get(iterator).setTrainTrackData(station2.getTrainTrackData());
        route.routeSegmentList.get(iterator).IsTrainSegment(true);
        route.routeSegmentList.get(iterator).setDepTime(station2.getDepTime());
        route.routeSegmentList.get(iterator).setDepType(station2.getDepType());
        route.routeSegmentList.get(iterator).setDepTrack(station2.getDepTrack());
        route.routeSegmentList.get(iterator).setArrTrack(station2.getArrTrack());
        route.routeSegmentList.get(iterator).setArrTime(station2.getArrTime());
        route.routeSegmentList.get(iterator).setOrigin(station2.getOrigin());
        route.routeSegmentList.get(iterator).setDestination(station2.getDestination());
        route.routeSegmentList.get(iterator).BuildPolylineOptions(getBaseContext());

        // Create walking segment
        route.addRouteSegment();
        iterator = route.routeSegmentList.size() - 1;
        route.routeSegmentList.get(iterator).setDepTime("Tässä minä kävelen juna-asemalle");
        route.routeSegmentList.get(iterator).setOrigin(new LatLng(Double.parseDouble(ApplicationData.masterRoute.getDestinationClosestStation().getLatitude()),Double.parseDouble(ApplicationData.masterRoute.getDestinationClosestStation().getLongitude())));
        route.routeSegmentList.get(iterator).setDestination(new LatLng(ApplicationData.masterRoute.getDestinationLocation().getLatitude(),ApplicationData.masterRoute.getDestinationLocation().getLongitude()));
        route.routeSegmentList.get(iterator).BuildPolylineOptions(getBaseContext());

        // Create fullRoute-object of found route
        fullRouteList.add(route);

    }




    private boolean checkDirectRoute(int i, int i1) {
        boolean runsDirectly = false;
        for (int i2 = 0; i2 < stationTrains.get(i).get(i1).timeTableRows.size(); i2++) {
            String stationShortCode = stationTrains.get(i).get(i1).timeTableRows.get(i2).getStationShortCode();
            if (stationShortCode.equals(ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode())) {
                Log.d("station", stationShortCode);
                Log.d("stationDest", ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode());
                runsDirectly = true;
                Log.d("checkDirectRoute", "Train runs directly, ignoring" + i1);
            }
        }
        Log.d("runsDirectly", Boolean.toString(runsDirectly));
        return runsDirectly;
    }

    private Date convertStringToDate(String time) {
        Date data = new Date();
        try {
            data = timeFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return data;
    }


    private int[] findDestinationTimeTable(int i, int x0) {
        String stationShortCode;
        int[] values = {0, 0, 0};
        for (int x1 = 0; x1 < stationTrains.get(i).get(x0).timeTableRows.size(); x1++) {
            stationShortCode = stationTrains.get(i).get(x0).timeTableRows.get(x1).getStationShortCode();
            if (stationShortCode.equals(ApplicationData.masterRoute.getDestinationClosestStation().getStationShortCode())) {
                values[0] = i;
                values[1] = x0;
                values[2] = x1;
            }
        }
        return values;
    }




    private void findClosestStations() {
        Log.d("findClosestStations", "Started!");
         //Router router = new Router();
        //router.delegate = RoutePresenter.this;
        TrainDataFetcher trainDataFetcher = new TrainDataFetcher();
        Log.d("findClosestStations","Fetching origin");
        if (ApplicationData.deviceLocationIsOrigin) {
            Log.d("findClosestStations","Using device location for origin");

            ApplicationData.masterRoute.setOriginClosestStation(findClosestStationFromPoint(ApplicationData.mLastLocation)); // Find closest train station from origin
            // Get Latitude and Longitude from found closest station and convert into a string
            String originStationLocation = (ApplicationData.masterRoute.getOriginClosestStation().getLatitude() +","+ ApplicationData.masterRoute.getOriginClosestStation().getLongitude());
            String originLocTemp = (ApplicationData.mLastLocation.getLatitude() +","+ ApplicationData.mLastLocation.getLongitude()); // Convert user location from Location to String
            // TODO tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?!
            trainDataFetcher.GetDistanceAndDuration(originLocTemp, originStationLocation); // Get dist&dur from user location to closest station TODO: Tarvitaanko tätä?
        } else {
            Log.d("findClosestStations","Using other than device location for origin");
            Location mCustomLocation = getLocationFromAddress(ApplicationData.masterRoute.getOriginAddress()); // Convert given address to Location (lat&long)

            ApplicationData.masterRoute.setOriginClosestStation(findClosestStationFromPoint(mCustomLocation)); // Find closest train station from converted Location
            // Get Latitude and Longitude from found closest station and convert into a string
            String originStationLocation = (ApplicationData.masterRoute.getOriginClosestStation().getLatitude() +","+ ApplicationData.masterRoute.getOriginClosestStation().getLongitude());
            // TODO tää alempi tieto pitäs tallentaa johonkin ja käyttää sitä searchDirectTrackConnectionissa plus-aikana?!
            trainDataFetcher.GetDistanceAndDuration(ApplicationData.masterRoute.getOriginAddress(), originStationLocation); // Get dist&dur from user location to closest station. TODO: Tarvitaanko tätä?
        }
        Log.d("findClosestStations","Fetching destination");
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
        Location pointOrigin = new Location(location);         //////// TODO jos tyhjä niin kaataa softan. UPDATE: KAATAAKO EDELLEEN?
        Float closestDist = 9999999999999999999f;
//stationData.add(new String[] {station.getString("stationName"), station.getString("stationShortCode"), station.getString("latitude"), station.getString("longitude")});
        for (int i = 0; i < ApplicationData.stationData.size(); i++) {
            if(ApplicationData.stationData.get(i).isPassengerTraffic())
            {
                Location pointDestination = new Location("pointDestination");
                pointDestination.setLatitude(Double.parseDouble(ApplicationData.stationData.get(i).getLatitude()));
                pointDestination.setLongitude(Double.parseDouble(ApplicationData.stationData.get(i).getLongitude()));
                Float distTemp = pointOrigin.distanceTo(pointDestination);
                if (distTemp < closestDist) {
                    closestDist = distTemp;
                    closestStation = ApplicationData.stationData.get(i);
                }
            }
        }
        Log.d("Closest station found", closestStation.toString());
        return closestStation;
    }



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
        if(ApplicationData.mMarker != null)
        {
            ApplicationData.mMarker.remove();
        }

        LatLng myLoc = new LatLng(ApplicationData.mLastLocation.getLatitude(), ApplicationData.mLastLocation.getLongitude());
        MarkerOptions userIndicator = new MarkerOptions()
                .position(myLoc)
                .title("You are here");
        ApplicationData.mMarker = ApplicationData.mMap.addMarker(userIndicator);
    }

    @Override
    public void atMapReady(GoogleMap googleMap)
    {
        Log.d("atMapReady", "MAP READY IN ROUTEPRESENTER");
        ApplicationData.mMap = googleMap;
        fullRouteMap = googleMap;
        ApplicationData.mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        ApplicationData.mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if(LocationPermissionAgent.isLocationEnabled(this))
        {
            if(ApplicationData.checkLocationPermission(this))
            {
                ApplicationData.mMap.setMyLocationEnabled(true);
            }
        }
        else
        {
            if(ApplicationData.checkLocationPermission(this))
            {
                ApplicationData.mMap.setMyLocationEnabled(false);

            }
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

    private String[] convertTime(String datetime) {
        String timeTemp[];
        String dateTemp[];
        String date = "";
        String time = "";
        String returnDateTime[] = {"",""};
        // Split string "scheduledTime" into two different strings and convert to more user convenient format
        // Create StringBuilder and remove last letter ('Z' in json array)
        StringBuilder sb = new StringBuilder(datetime);
        sb.deleteCharAt(sb.length() - 1);
        // Split StringBuilder into two variables (scheduledDate and scheduledTime)
        timeTemp = sb.toString().split("T");
        // Convert scheduledDate from YYYY-MM-DD to DD.MM.YYY
        dateTemp = timeTemp[0].split("-");
        for (int d = dateTemp.length; d > 0; d--) {
            date += dateTemp[d - 1];
            if (d - 1 > 0) {
                date += ".";
            }
        }
        // Convert time
        StringBuilder timeStringBuilder = new StringBuilder(timeTemp[1]);
        timeStringBuilder.delete(5, timeStringBuilder.length());
        time = timeTemp[1];

        returnDateTime[0] = date;
        returnDateTime[1] = time;

        return returnDateTime;
    }



    protected void searchDirectTrackConnection(JSONArray json) {
        Log.d("searchDirectTrackConn..", "started");
        // Find text block to put data, JUST FOR DEBUGGING. FINAL DATA DERIVED THROUGH BUNDLES!(?)
        //TextView textView = (TextView) findViewById(R.id.result1);
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
                      //  Log.d("Adding short code,loop " + y, tt.getString("stationShortCode"));
                        stationShortCodes.add(tt.getString("stationShortCode"));
                    }
                    if (tt.getString("stationShortCode").equals(ApplicationData.masterRoute.getOriginClosestStation().getStationShortCode()) // [0] = stationshortcode
                            && tt.getString("type").equals("DEPARTURE")) {
                        stationIsRelevant = true;
                        stationShortCodes.add(tt.getString("stationShortCode"));
                       // Log.d("Adding short code,loop " + y, tt.getString("stationShortCode"));
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

            Message message = handler.obtainMessage();
            message.what = 4;
            handler.sendMessage(message);
            Log.d("Handler Message", message.toString());

        } catch (Exception e) { // JSONException?
            // If no straight trains found
            Log.d("No trains found", e.toString());
            // --------DEV---------- KLUP now program should start looking for alternate routes (via stop/multiple stops)
        }
    }



    protected void setAdapter() {
        Log.d("RecyclerView", "called");
        final RecyclerView.Adapter adapter = new fullRouteAdapter(RoutePresenter.this, fullRouteList);
        recyclerView.setAdapter(adapter);
        AddItemTouchHelper(adapter);
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
                    fullRouteList.remove(viewHolder.getAdapterPosition());
                    // update adapter
                    rAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                }
                else if (swipeDir == ItemTouchHelper.RIGHT)
                {
                    Intent intent = new Intent(context, segmentPresenter.class);
                    intent.putExtra("route", fullRouteList.get(viewHolder.getAdapterPosition()));
                    rAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    ApplicationData.selectedRoute = fullRouteList.get(viewHolder.getAdapterPosition());
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
        try {
            Log.d("trainData", "Size " + trainData.size());
            if (trainData != null) {
                for (int index = 0; index < trainData.size(); index++) {
                    Log.d("Current index", Integer.toString(index));
                    Log.d("Create new route", "index " + index);
                    // Copy masterRoute and put copy into arraylist
                    fullRoute route = new fullRoute(ApplicationData.masterRoute);
                    fullRouteList.add(route);
                    int iterator = fullRouteList.size() - 1; // Need to know where in dynamic loop
                    fullRouteList.get(iterator).setOriginDate(Integer.toString(index)); // TODO: welp?

                    Log.d("Add data to segment", Integer.toString(index));
                    Log.d("createFullRouteObjects", "Tehdään kävelyetappi 1");
                    // Kävele asemalle ensin
                    fullRouteList.get(iterator).addRouteSegment();
                    fullRouteList.get(iterator).routeSegmentList.get(0).setDepTime("Tässä minä kävelen juna-asemalle");
                    fullRouteList.get(iterator).routeSegmentList.get(0).setOrigin(new LatLng(ApplicationData.masterRoute.getOriginLocation().getLatitude(),ApplicationData.masterRoute.getOriginLocation().getLongitude()));
                    fullRouteList.get(iterator).routeSegmentList.get(0).setDestination(new LatLng(Double.parseDouble(ApplicationData.masterRoute.getOriginClosestStation().getLatitude()),Double.parseDouble(ApplicationData.masterRoute.getOriginClosestStation().getLongitude())));
                    Log.d("createFullRouteObjects", "Building polylineoptions");
                    fullRouteList.get(iterator).routeSegmentList.get(0).BuildPolylineOptions(getBaseContext());
                    Log.d("createFullRouteObjects ", "Tehdään junaetappia");
                    // Junatiedot
                    fullRouteList.get(iterator).addRouteSegment(trainData.get(index));
                    //fullRouteList.get(index).routeSegmentList.get(1).setTrainNumber(trainData.get(index).getTrainNumber());
                    //fullRouteList.get(index).routeSegmentList.get(1).setTrainType(trainData.get(index).getTrainType());
                    fullRouteList.get(iterator).routeSegmentList.get(1).setDepType(trainDataTimeTableDeparture.get(index)[0]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setDepTrack(trainDataTimeTableDeparture.get(index)[1]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setDepDate(trainDataTimeTableDeparture.get(index)[2]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setDepTime(trainDataTimeTableDeparture.get(index)[3]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setArrType(trainDataTimeTableArrival.get(index)[0]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setArrTrack(trainDataTimeTableArrival.get(index)[1]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setArrDate(trainDataTimeTableArrival.get(index)[2]);
                    fullRouteList.get(iterator).routeSegmentList.get(1).setArrTime(trainDataTimeTableArrival.get(index)[3]);
                    Log.d("createFullRouteObjects", "Building polylineoptions for traintrip");
                    fullRouteList.get(iterator).routeSegmentList.get(1).BuildPolylineOptions(getBaseContext());
                    Log.d("createFullRouteObjects", "Tehdään kävelyetappi 2");
                    // Kävele asemalta kohteeseen
                    fullRouteList.get(iterator).addRouteSegment();
                    fullRouteList.get(iterator).routeSegmentList.get(2).setArrTime("Tässä minä kävelen asemalta kohteeseen");
                    fullRouteList.get(iterator).routeSegmentList.get(2).setOrigin(new LatLng(Double.parseDouble(ApplicationData.masterRoute.getDestinationClosestStation().getLatitude()),Double.parseDouble(ApplicationData.masterRoute.getDestinationClosestStation().getLongitude())));
                    fullRouteList.get(iterator).routeSegmentList.get(2).setDestination(new LatLng(ApplicationData.masterRoute.getDestinationLocation().getLatitude(),ApplicationData.masterRoute.getDestinationLocation().getLongitude()));
                    Log.d("createFullRouteObjects", "Building polylineoptions");
                    fullRouteList.get(iterator).routeSegmentList.get(2).BuildPolylineOptions(getBaseContext());
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
