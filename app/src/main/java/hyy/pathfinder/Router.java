package hyy.pathfinder;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Prometheus on 22-Sep-16.
 */
public class Router extends AsyncTask<String, Void, Void>
{
<<<<<<< HEAD
    private int mode;
    public AsyncResponse delegate = null;
    private List<String> list;
    private PolylineOptions polylineOptions;

    @Override
    protected Void doInBackground(String... urlString)
    {
        try
        {
            FetchUrl fetchUrl = new FetchUrl();
            String jsonString = fetchUrl.downloadUrl(urlString[0]);
            JSONObject jObject = new JSONObject(jsonString);
            DataParser parser = new DataParser();
            switch (mode)
            {
                case 1:
                    // halutaan vain etäisyys ja kesto
                     list = parser.parseTotalDistanceAndDuration(jObject);

                    break;
                case 2:
                    // halutaan piirretty reitti paikasta A paikkaan B
                    List<List<HashMap<String,String>>> linelist = parser.parse(jObject);
                    polylineOptions = getDrawnRoute(linelist);
                    break;
                default:
                    throw new NullPointerException("No mode selected for Router");
=======
    List<String> sunData;

    public void getTravelDistanceAndDuration(String originCoordinateString, String destinationCoordinateString)
    {
        String destination = URLEncoder.encode(destinationCoordinateString);
        String origin = URLEncoder.encode(originCoordinateString);

        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + getResources().getString(R.string.google_maps_key);
        Log.i("urlString", urlString);
        FetchUrl fetchUrl = new FetchUrl();
        fetchUrl.execute(urlString);

    }

    private class ParserTask extends AsyncTask<String, Integer, List<String>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<String> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<String> list = null;

            try {

                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                list = parser.parseTotalDistanceAndDuration(jObject);


            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
>>>>>>> origin/master
            }
        }
<<<<<<< HEAD
        catch (IOException e)
        {
            Log.d("Exception", e.toString());
        }
        catch (JSONException e)
        {
            Log.d("Exception", e.toString());
        }
        return null;
=======

        @Override
        protected void onPostExecute(List<String> result)
        {
            sunData = result;
        }
>>>>>>> origin/master
    }

    @Override
    protected void onPostExecute(Void v)
    {
        switch (mode)
        {
            case 1:
                // halutaan vain etäisyys ja kesto
                delegate.getSpaceTimeFinish(list);
                break;
            case 2:
                // halutaan piirretty reitti paikasta A paikkaan B
                delegate.getRouteFinish(polylineOptions);
                break;
            default:
                throw new NullPointerException("No mode selected for Router");
        }
    }

    private PolylineOptions getDrawnRoute(List<List<HashMap<String, String>>> data)
    {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;

        // Traversing through all the routes
        for (int i = 0; i < data.size(); i++) {
            points = new ArrayList<>();
            lineOptions = new PolylineOptions();

            // Fetching i-th route
            List<HashMap<String, String>> path = data.get(i);

            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);
            lineOptions.width(10);
            lineOptions.color(Color.RED);
        }

        return lineOptions;
    }


    public void getTravelDistanceAndDuration(String origin, String destination, Context context)
    {
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + context.getResources().getString(R.string.google_maps_key);
        mode = 1;
        this.execute(urlString);
    }

    public void getRoute(String origin, String destination, Context context)
    {
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=" + context.getResources().getString(R.string.google_maps_key);
        mode = 2;
        this.execute(urlString);
    }



}
