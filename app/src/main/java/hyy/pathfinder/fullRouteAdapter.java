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
 * Created by Kotimonni on 15.11.2016.
 */

public class fullRouteAdapter extends RecyclerView.Adapter<fullRouteAdapter.ViewHolder> {
    // Adapter data
    private List<fullRoute> fullRouteList;
    private Activity context;

    // Adapter constructor, get data from activity
    public fullRouteAdapter(Activity context, List<fullRoute> fullRouteList) {
        this.fullRouteList = fullRouteList;
        this.context = context;
        Log.d("fullRouteAdapter", "started");
    }

    // Return the size of routeList (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (fullRouteList != null) {
            return fullRouteList.size();
        }
        else return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fullroute_card, parent, false);
        return new ViewHolder(view);
    }

    // replace the contents of a view (invoked by the layout manager)
    // get element from kenttaList at this position
    // replace the contents of the view with that element
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        fullRoute fRoute = fullRouteList.get(position);
      /*  Glide.with(this.context)
                .load("KUVA")
                .override(250,170)
                .into(viewHolder.kenttaImageView);
*/
        viewHolder.originDateTV.setText(fRoute.getOriginDate());
        viewHolder.originTimeTV.setText(fRoute.routeSegmentList.get(position).getDepTime()); // TODO: Miksi position tässä että toimii?
        int asd = fRoute.routeSegmentList.size();
        Log.d("ASDASDASD", Integer.toString(asd));
        viewHolder.originClosestStationTV.setText(fRoute.getOriginClosestStation()[0]);
        viewHolder.destinationClosestStationTV.setText(fRoute.getDestinationClosestStation()[0]);


    }

    // view holder class to specify card UI objects
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        // public ImageView kenttaImageView;
        public TextView originDateTV;
        public TextView originTimeTV;
        public TextView originClosestStationTV;
        public TextView destinationClosestStationTV;


        public ViewHolder (View itemView) {
            super (itemView);
            // get layout IDs
            //    kenttaImageView = (ImageView) itemView.findViewById(R.id.kenttaImageView);
            originDateTV = (TextView) itemView.findViewById(R.id.originDateTV);
            originTimeTV = (TextView) itemView.findViewById(R.id.originTimeTV);
            originClosestStationTV = (TextView) itemView.findViewById(R.id.originClosestStationTV);
            destinationClosestStationTV = (TextView) itemView.findViewById(R.id.destinationClosestStationTV);
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
