package hyy.pathfinder;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        viewHolder.originTextView.setText(route.originAddress);
        viewHolder.destinationTextView.setText(route.destinationAddress);
        viewHolder.durationTextView.setText(route.duration);
        viewHolder.distanceTextView.setText(route.distance);
    }

    // view holder class to specify card UI objects
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
       // public ImageView kenttaImageView;
        public TextView originTextView;
        public TextView destinationTextView;
        public TextView durationTextView;
        public TextView distanceTextView;

        public ViewHolder (View itemView) {
            super (itemView);
            // get layout IDs
        //    kenttaImageView = (ImageView) itemView.findViewById(R.id.kenttaImageView);
            originTextView = (TextView) itemView.findViewById(R.id.originTextView);
            destinationTextView = (TextView) itemView.findViewById(R.id.destinationTextView);
            durationTextView = (TextView) itemView.findViewById(R.id.durationTextView);
            distanceTextView = (TextView) itemView.findViewById(R.id.distanceTextView);
            // add click listener for a card
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Intent intent = new Intent(context, RouteDetail.class);


                    view.getContext().startActivity(intent);
                }
            });
        }
    }


}
