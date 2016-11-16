package hyy.pathfinder;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

public class segmentPresenter extends AppCompatActivity {
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segment_presenter);

        // Get data from calling intent
        Bundle data = getIntent().getExtras();
        fullRoute fRoute = (fullRoute) data.getParcelable("route");


        // Create RecyclerView and LayoutManager
        recyclerView = (RecyclerView) findViewById(R.id.routesegment_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Log.d("RecyclerView", "called");
        RecyclerView.Adapter adapter = new routeSegmentAdapter(this, fRoute.routeSegmentList);
        recyclerView.setAdapter(adapter);
    }
}
