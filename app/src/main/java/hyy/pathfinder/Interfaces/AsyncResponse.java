package hyy.pathfinder.Interfaces;


import org.json.JSONArray;

//import hyy.pathfinder.Objects.Route;

/**
 * Created by Prometheus on 04-Oct-16.
 */

public interface AsyncResponse
{
    void onAsyncJsonFetcherComplete(int mode, JSONArray json, boolean jsonException);
}
