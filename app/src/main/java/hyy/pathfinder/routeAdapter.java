package hyy.pathfinder;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Kotimonni on 31.10.2016.
 */

public class routeAdapter extends RecyclerView.Adapter<routeAdapter.ViewHolder> {
    // Adapter data
    private List<Route> routeList;
    private Activity context;
    // Adapter constructor, get data from activity
    public routeAdapter(Activity context, List<Route> routeList) {
        this.routeList = routeList;
        this.context = context;
        Log.d("routeAdapter", "started");
    }

    // Return the size of routeList (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (routeList != null) {
            return routeList.size();
        }
        else return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_card, parent, false);
        return new ViewHolder(view);
    }

    // replace the contents of a view (invoked by the layout manager)
    // get element from kenttaList at this position
    // replace the contents of the view with that element
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Route route = routeList.get(position);
      /*  Glide.with(this.context)
                .load("KUVA")
                .override(250,170)
                .into(viewHolder.kenttaImageView);
*/
        viewHolder.depDateTextView.setText(route.depDate);
        viewHolder.depTimeTextView.setText(route.depTime);
        viewHolder.depTrackTextView.setText(route.depTrack);
        viewHolder.trainTypeTextView.setText(route.trainType);
        viewHolder.trainNumberTextView.setText(route.trainNumber);
        viewHolder.arrTimeTextView.setText(route.arrTime);
        viewHolder.arrTrackTextView.setText(route.arrTrack);

    }

    // view holder class to specify card UI objects
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
       // public ImageView kenttaImageView;
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
                    Intent intent = new Intent(context, RouteDetail.class);

                    //view.getContext().startActivity(intent);
                }
            });
        }
    }


}
