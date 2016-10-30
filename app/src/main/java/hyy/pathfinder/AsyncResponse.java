package hyy.pathfinder;

import com.google.android.gms.maps.model.PolylineOptions;


import org.json.JSONArray;

import java.util.List;

/**
 * Created by Prometheus on 04-Oct-16.
 */

public interface AsyncResponse
{
    void getRouteFinish(Route route);
    void getTotalDistanceAndDurationFinish(Route route);
    void onAsyncJsonFetcherComplete(int mode, JSONArray json, boolean jsonException);
    //void getBusRouteFinish(Route route);
}
