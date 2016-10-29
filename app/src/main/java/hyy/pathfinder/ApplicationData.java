package hyy.pathfinder;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

import java.util.List;

/**
 * Created by H8244 on 10/28/2016.
 */

public class ApplicationData extends Application
{
    // ALUSTA TÄNNE TARPEELLISET MUUTTUJAT. HUOM STATIC KEYWORD

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public static GoogleApiClient mGoogleApiClient;
    public static Context mContext;
    public static Location mLastLocation;
    public static LocationRequest mLocationRequest;
    public static LocationListener locationListener;
    public static ApplicationDataCallbacks applicationDataCallbacks;
    public static GoogleMap mMap;
    public static boolean deviceLocationIsOrigin;
    public static List<Route> routes; // testaukseen. Myöhemmin luultavasti tarvitaan List<List<Route>> routes


    // Pitää ajaa getApplicationContext(), setApplicationDataCallbacks(), setApplicationDataCallbacksDelegate, setLocationListener(), buildGoogleApiClient ja viimeisenä createLocationRequest() (järjestys oleellinen, nullpointerit herkässä)
    // Tämän luokan onCreatessa homma ei toimi, koska luokasta ei koskaan tehdä insanssia - sen toimintoja käytetään vain staattisten funktioiden ja muuttujien kautta.
    // Aina kun siirrytään uuteen aktiviteettiin täytyy asettaa se aktiviteetti delegaatiksi jolle interface paiskaa vastuun callbackista, pysäyttää edellisen mGoogleApiClientin updatet ja luoda uusi googleApiClient uudelle aktiviteetille

    // TODO: kun painetaan back nappulaa ja siirrytään aktiviteeteissa taaksepäin, niin delegaatti pitää uudelleenasettaa jotenkin edeltävään aktiviteettiin ja googleapiclient uusia. Voisiko onBackPressed callbackia hyödyntää?


    public static void setApplicationDataCallbacks()
    {

        Log.d("ApplicationData", "setApplicationDataCallbacks");
        applicationDataCallbacks = new ApplicationDataCallbacks();
    }

    public static void setGoogleMap(GoogleMap googleMap)
    {
        mMap = googleMap;
    }

    public static void setLocationListener()
    {

        Log.d("ApplicationData", "setLocationListener");
        ApplicationData.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                applicationDataCallbacks.delegate.atLocationChanged(location);
            }
        };
    }

    public static void setApplicationDataCallbacksDelegate(AppDataInterface Delegate)
    {
        Log.d("ApplicationData", "setting delegate");
        applicationDataCallbacks.delegate = Delegate;
    }


    public static void startLocationUpdates(Activity activity)
    {

        Log.d("ApplicationData", "startLocationUpdates");
        if(checkLocationPermission(activity)) {

            Log.d("ApplicationData", "permission granted, updating location");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, locationListener);
        }
    }

    protected static void stopLocationUpdates() {

        Log.d("ApplicationData", "stopLocationServices");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, locationListener);
    }


    public static void getLastLocation(Activity activity)
    {
        Log.d("ApplicationData", "getLastLocation");
        if(checkLocationPermission(activity)) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
    }


    protected static boolean checkLocationPermission(Activity activity){
        if (ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    public static void createLocationRequest() {

        Log.d("ApplicationData", "createLocationRequest");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }


    public static synchronized void buildGoogleApiClient(Activity activity) {
        Log.d("ApplicationData", "buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(applicationDataCallbacks)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
}
