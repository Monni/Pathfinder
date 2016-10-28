package hyy.pathfinder;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;

/**
 * Created by Prometheus on 28-Oct-16.
 */

public class ApplicationDataCallbacks implements GoogleApiClient.ConnectionCallbacks, LocationListener {

    public AppDataInterface delegate = null;

    @Override
    public void onLocationChanged(Location location)
    {
        delegate.locationChanged(location);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        delegate.connected(bundle);
    }

    @Override
    public  void onConnectionSuspended(int errorCode)
    {
        delegate.suspended(errorCode);
    }

}
