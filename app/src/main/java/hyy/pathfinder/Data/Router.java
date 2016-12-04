package hyy.pathfinder.Data;

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

import hyy.pathfinder.Data.DataParser;
import hyy.pathfinder.Data.FetchUrl;
import hyy.pathfinder.Interfaces.AsyncResponse;
import hyy.pathfinder.Interfaces.RouterResponse;
//import hyy.pathfinder.Objects.Route;
import hyy.pathfinder.R;

/**
 * Created by Prometheus on 22-Sep-16.
 */



public class Router extends AsyncTask<String, Void, PolylineOptions>
{
    public RouterResponse delegate = null;
    public Router(){}
    public Router(RouterResponse Delegate)
    {
        delegate = Delegate;
    }
    @Override
    protected PolylineOptions doInBackground(String... strings) {
        try
        {
            FetchUrl fetchUrl = new FetchUrl();
            String jsonString = fetchUrl.downloadUrl(strings[0]);

            JSONObject jObject = new JSONObject(jsonString);
            DataParser parser = new DataParser();
            List<List<HashMap<String, String>>> linelist = parser.parseLineList(jObject);
            PolylineOptions pOptions = getDrawnRoute(linelist);
            pOptions.width(5);
            pOptions.color(Color.RED);
            return pOptions;
        }
        catch (IOException e)
        {
            Log.d("IOEXCEPTION", "AT ROUTER");
        }
        catch (JSONException e)
        {
            Log.d("JSONEXCEPTION", "AT ROUTER");

        }
        return null;
    }

    @Override
    protected void onPostExecute(PolylineOptions result)
    {
        delegate.PolylineOptionsFinished(result);
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


    public void GetPolylineOptions(String origin, String destination, Context context, RouterResponse Delegate)
    {
        delegate = Delegate;
        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&mode=walking&key=" + context.getResources().getString(R.string.google_maps_key);
        this.execute(urlString);
    }
}