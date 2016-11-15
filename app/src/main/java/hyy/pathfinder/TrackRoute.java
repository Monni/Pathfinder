package hyy.pathfinder;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by H8244 on 10/12/2016.
 */

public class TrackRoute extends Route
{
    public String departureTime;
    public String departureDate;

    public TrackRoute(){}
    public TrackRoute(LatLng Origin, LatLng Destination, String OriginAddress, String DestinationAddress, Integer Duration, Integer Distance, String DepartureTime, String DepartureDate)
    {
        origin = Origin;
        destination = Destination;
        originAddress = OriginAddress;
        destinationAddress = DestinationAddress;
        duration = Duration;
        distance = Distance;
        departureDate = DepartureDate;
        departureTime = DepartureTime;
    }
}
