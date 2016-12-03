package hyy.pathfinder.Adapters;

import android.app.Activity;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import hyy.pathfinder.Data.ApplicationData;
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
        // Ask for translation from stationShortCode to full stationName. If no match, returns the string used to ask translation
        viewHolder.originStationIdTextView.setText(ApplicationData.stationData.getStationName(segment.getOriginStationName()));
        viewHolder.destinationStationIdTextView.setText(ApplicationData.stationData.getStationName(segment.getDestinationStationName()));

        // What happens if routeSegment is not train segment
        if (!segment.IsTrainSegment()) {
            // Set big icon to walking type
            viewHolder.segmentTypeImageView.setImageResource(R.drawable.walk_icon_big);
            // Hide other icons
            viewHolder.clockIconOrigin.setVisibility(View.INVISIBLE);
            viewHolder.clockIconDestination.setVisibility(View.INVISIBLE);
            viewHolder.trackIconOrigin.setVisibility(View.INVISIBLE);
            viewHolder.trackIconDestination.setVisibility(View.INVISIBLE);
            viewHolder.trainIconOrigin.setVisibility(View.INVISIBLE);
        } else {
            // Else set big icon to train type
            viewHolder.segmentTypeImageView.setImageResource(R.drawable.train_icon_big);
        }
    }

    // view holder class to specify card UI objects
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        private TextView depDateTextView;
        private TextView depTimeTextView;
        private TextView trainNumberTextView;
        private TextView trainTypeTextView;
        private TextView depTrackTextView;
        private TextView arrTimeTextView;
        private TextView arrTrackTextView;
        private TextView originStationIdTextView;
        private TextView destinationStationIdTextView;
        // Icons
        private ImageView segmentTypeImageView;
        private ImageView clockIconOrigin;
        private ImageView clockIconDestination;
        private ImageView trackIconOrigin;
        private ImageView trackIconDestination;
        private ImageView trainIconOrigin;



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
            originStationIdTextView = (TextView) itemView.findViewById(R.id.originStationIdTV);
            destinationStationIdTextView = (TextView) itemView.findViewById(R.id.destinationStationIdTV);
            // Icons
            segmentTypeImageView = (ImageView) itemView.findViewById(R.id.segmentTypeIV);
            clockIconOrigin = (ImageView) itemView.findViewById(R.id.clock_icon_origin);
            clockIconDestination = (ImageView) itemView.findViewById(R.id.clock_icon_destination);
            trackIconOrigin = (ImageView) itemView.findViewById(R.id.track_icon_origin);
            trackIconDestination = (ImageView) itemView.findViewById(R.id.track_icon_destination);
            trainIconOrigin = (ImageView) itemView.findViewById(R.id.train_icon_origin);

            // add click listener for a card
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Toast.makeText(context, "Halp!", Toast.LENGTH_SHORT).show();
                    //routeSegmentList.get(position).DrawSegmentInMap();
                    ApplicationData.mMap.clear();
                    ApplicationData.selectedRoute.routeSegmentList.get(position).DrawSegmentInMap();


                }
            });
        }
    }
}
