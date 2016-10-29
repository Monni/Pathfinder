package hyy.pathfinder;

import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

/**
 * Created by Prometheus on 28-Oct-16.
 */

public class ApplicationDataCallbacks implements GoogleApiClient.ConnectionCallbacks, LocationListener, OnMapReadyCallback {

    public AppDataInterface delegate = null;

    @Override
    public void onLocationChanged(Location location)
    {
        delegate.atLocationChanged(location);
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        delegate.atConnected(bundle);
    }

    @Override
    public void onConnectionSuspended(int errorCode)
    {
        delegate.atSuspended(errorCode);
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        delegate.atMapReady(googleMap);
    }

}
