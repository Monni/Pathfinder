package hyy.pathfinder.Adapters;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import hyy.pathfinder.Activities.ApplicationData;
import hyy.pathfinder.Objects.routeSegment;
import hyy.pathfinder.R;

/**
 * Created by Kotimonni on 16.11.2016.
 */

public class routeSegmentAdapter extends RecyclerView.Adapter<routeSegmentAdapter.ViewHolder> {
    // Adapter data
    private List<routeSegment> routeSegmentList;
    private Activity context;

    // Adapter constructor, get data from activity
    public routeSegmentAdapter(Activity context, List<routeSegment> routeSegmentList) {
        this.routeSegmentList = routeSegmentList;
        this.context = context;
        Log.d("routeSegmentAdapter", "started");
    }

    // Return the size of routeList (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (routeSegmentList != null) {
            return routeSegmentList.size();
        }
        else return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.routesegment_card, parent, false);
        return new ViewHolder(view);
    }

    // replace the contents of a view (invoked by the layout manager)
    // get element from routeSegmentList at this position
    // replace the contents of the view with that element
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        routeSegment segment = routeSegmentList.get(position);
        viewHolder.depDateTextView.setText(segment.getDepDate());
        viewHolder.depTimeTextView.setText(segment.getDepTime());
        viewHolder.depTrackTextView.setText(segment.getDepTrack());
        viewHolder.trainTypeTextView.setText(segment.getTrainType());
        viewHolder.trainNumberTextView.setText(segment.getTrainNumber());
        viewHolder.arrTimeTextView.setText(segment.getArrTime());
        viewHolder.arrTrackTextView.setText(segment.getArrTrack());


    }

    // view holder class to specify card UI objects
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView depDateTextView;
        public TextView depTimeTextView;
        public TextView trainNumberTextView;
        public TextView trainTypeTextView;
        public TextView depTrackTextView;
        public TextView arrTimeTextView;
        public TextView arrTrackTextView;


        public ViewHolder (View itemView) {
            super (itemView);
            // get layout IDs
            //    kenttaImageView = (ImageView) itemView.findViewById(R.id.kenttaImageView);
            depDateTextView = (TextView) itemView.findViewById(R.id.depDateTextView);
            depTimeTextView = (TextView) itemView.findViewById(R.id.depTimeTextView);
            trainNumberTextView = (TextView) itemView.findViewById(R.id.trainNumberTextView);
            trainTypeTextView = (TextView) itemView.findViewById(R.id.trainTypeTextView);
            depTrackTextView = (TextView) itemView.findViewById(R.id.depTrackTextView);
            arrTimeTextView = (TextView) itemView.findViewById(R.id.arrTimeTextView);
            arrTrackTextView = (TextView) itemView.findViewById(R.id.arrTrackTextView);
            // add click listener for a card
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Toast.makeText(context, "Velp!", Toast.LENGTH_SHORT).show();
                    //routeSegmentList.get(position).DrawSegmentInMap();
                    ApplicationData.mMap.clear();
                    ApplicationData.selectedRoute.routeSegmentList.get(position).DrawSegmentInMap();


                }
            });
        }
    }
}
