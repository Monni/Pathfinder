package hyy.pathfinder;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Created by Prometheus on 11-Oct-16.
 */

public class Route
{
    public LatLng origin;
    public LatLng destination;
    public Integer duration;
    public Integer distance;
    public PolylineOptions polylineOptions;
    public Integer id;
    public String originAddress;
    public String destinationAddress;
    public int index;
    public String url;

    public Route(){}
    public Route (String Url)
    {
        url = Url;
    }
    public Route(String Url, int Index)
    {
        index = Index;
        url = Url;
    }
    public Route(LatLng Origin, LatLng Destination, String OriginAddress,String DestinationAddress, Integer Duration, Integer Distance)
    {
        origin = Origin;
        destination = Destination;
        originAddress = OriginAddress;
        destinationAddress = DestinationAddress;
        duration = Duration;
        distance = Distance;
        url = "";

        CreatePolyline();
    }

    protected void CreatePolyline()
    {
        polylineOptions = new PolylineOptions();
        polylineOptions.add(origin);
        polylineOptions.add(destination);
        polylineOptions.width(5).color(Color.RED);
    }

}

