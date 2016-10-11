package hyy.pathfinder;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Prometheus on 22-Sep-16.
 */
public class Router extends AsyncTask<String, Void, Void>
{
    private int mode;
    public AsyncResponse delegate = null;
    private List<String> list;
    private PolylineOptions polylineOptions;

    @Override
    protected Void doInBackground(String... urlString) {
        try {
            FetchUrl fetchUrl = new FetchUrl();
            String jsonString = fetchUrl.downloadUrl(urlString[0]);
            JSONObject jObject = new JSONObject(jsonString);
            DataParser parser = new DataParser();
            switch (mode) {
                case 1:
                    // halutaan vain etäisyys ja kesto
                    list = parser.parseTotalDistanceAndDuration(jObject);

                    break;
                case 2:
                    // halutaan piirretty reitti paikasta A paikkaan B kaikkine herkkuineen
                    List<List<HashMap<String, String>>> linelist = parser.parse(jObject);
                    polylineOptions = getDrawnRoute(linelist);
                    break;
                default:
                    throw new NullPointerException("No mode selected for Router");
            }
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
