package hyy.pathfinder;

import android.content.Context;
import android.location.LocationManager;

/**
 * Created by Prometheus on 05-Oct-16.
 */

public class LocationPermissionAgent {

    public static boolean isLocationEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            //All location services are disabled
            return false;
        }
        else return true;
    }
}
