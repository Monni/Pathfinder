package hyy.pathfinder;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.GoogleMap;

/**
 * Created by Prometheus on 28-Oct-16.
 */

public interface AppDataInterface
{
    void atConnected(Bundle bundle);
    void atConnectionFailed(ConnectionResult connectionResult);
    void atSuspended(int errorCode);
    void atLocationChanged(Location location);
    void atMapReady(GoogleMap googlemap);
}
