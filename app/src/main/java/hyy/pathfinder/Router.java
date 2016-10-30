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
public class Router extends AsyncTask<Route, Void, Route>
{
    private int mode;
    public AsyncResponse delegate = null;
    public Router(){}
    public Router(AsyncResponse Delegate)
    {
        delegate = Delegate;
    }
    @Override
    protected Route doInBackground(Route... r) {
        try {

            FetchUrl fetchUrl = new FetchUrl();
            String jsonString = fetchUrl.downloadUrl(r[0].url);
            JSONObject jObject = new JSONObject(jsonString);
            DataParser parser = new DataParser();
            Route route = parser.parseJsonToRoute(jObject);
            route.index = r[0].index;
            route.listIndex = r[0].listIndex;
            List<List<HashMap<String, String>>> linelist = parser.parseLineList(jObject);
            PolylineOptions pOptions = getDrawnRoute(linelist);
            route.polylineOptions = pOptions;
            switch(route.listIndex)
            {
                case 0:
                    route.polylineOptions.color(Color.RED);
                    break;
                case 1:
                    route.polylineOptions.color(Color.YELLOW);
                    break;
                case 2:
                    route.polylineOptions.color(Color.BLUE);
                    break;
            }
            return route;
        }
        catch (IOException e)
        {
            Log.d("Background Task", e.toString());
        }
        catch (JSONException e)
        {
            Log.d("Background Task", e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Route r)
    {
        switch (mode)
        {
            case 1:
                // halutaan vain etäisyys ja kesto
                delegate.getTotalDistanceAndDurationFinish(r);
                break;
            case 2:
                // halutaan piirretty reitti paikasta A paikkaan B
                delegate.getRouteFinish(r);
                break;
            default:
                throw new NullPointerException("No mode selected for Router");
        }
        //delegate.getRouteFinish(r);

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
            lineOptions.width(5);
        }

        return lineOptions;
    }


    public void getTravelDistanceAndDuration(String origin, String destination, Context context)
    {
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&mode=walking&key=" + context.getResources().getString(R.string.google_maps_key);
        mode = 1;
        Route route = new Route(urlString);
        this.execute(route);
    }

    public void getWalkingRoute(String origin, String destination, Context context, int index, int listIndex)
    {
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&mode=walking&key=" + context.getResources().getString(R.string.google_maps_key);
        mode = 2;
        Route route = new Route(urlString, index, listIndex);
        this.execute(route);
    }

    public void getBusRoute(String origin, String destination, Context context, int index, int listIndex)
    {
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&mode=transit&key=" + context.getResources().getString(R.string.google_maps_key);
        mode = 2;
        Route route = new Route(urlString, index, listIndex);
        this.execute(route);
    }
}
