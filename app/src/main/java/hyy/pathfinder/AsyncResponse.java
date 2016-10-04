package hyy.pathfinder;

import com.google.android.gms.maps.model.PolylineOptions;


import java.util.List;

/**
 * Created by Prometheus on 04-Oct-16.
 */

public interface AsyncResponse
{
    void getRouteFinish(PolylineOptions polylineOptions);
    void getSpaceTimeFinish(List<String> output);
}
