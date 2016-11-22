package hyy.pathfinder.Activities;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.fitness.data.Application;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;

import hyy.pathfinder.Adapters.routeSegmentAdapter;
import hyy.pathfinder.Interfaces.AppDataInterface;
import hyy.pathfinder.Objects.fullRoute;
import hyy.pathfinder.R;

public class segmentPresenter extends AppCompatActivity implements AppDataInterface {
    private RecyclerView recyclerView;
    private Bundle data;
    private fullRoute fRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("segmentPresenter", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segment_presenter);
        ApplicationData.setApplicationDataCallbacksDelegate(this);

        GetExtras();
        InitRecyclerView();
        InitAdapter();
        InitMap();

    }

    @Override
    public void atConnected(Bundle bundle)
    {

    }

    @Override
    public void atConnectionFailed(ConnectionResult connectionResult)
    {

    }

    @Override
    public void atSuspended(int errorCode)
    {

    }

    @Override
    public void atLocationChanged(Location location)
    {

    }

    @Override
    public void atMapReady(GoogleMap googleMap)
    {
        Log.d("atMapReady", "MAP READY IN SEGMENTPRESENTER");
        ApplicationData.mMap = googleMap;
        ApplicationData.mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ApplicationData.checkLocationPermission(segmentPresenter.this) && ApplicationData.deviceLocationListeningPermitted)
        {
            ApplicationData.mMap.setMyLocationEnabled(true);
        }
        ApplicationData.selectedRoute.DrawRouteOnMap();
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    protected void GetExtras()
    {
        // Get data from calling intent
        data = getIntent().getExtras();
        fRoute = (fullRoute) data.getParcelable("route");
    }

    protected void InitRecyclerView()
    {
        Log.d("InitRecyclerView", "called");
        // Create RecyclerView and LayoutManager
        recyclerView = (RecyclerView) findViewById(R.id.routesegment_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    protected void InitAdapter()
    {
        Log.d("InitAdapter", "called");

        RecyclerView.Adapter adapter = new routeSegmentAdapter(this, fRoute.routeSegmentList);
        recyclerView.setAdapter(adapter);
    }

    protected void InitMap()
    {
        Log.d("InitMap", "called");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(ApplicationData.applicationDataCallbacks);

    }
}
