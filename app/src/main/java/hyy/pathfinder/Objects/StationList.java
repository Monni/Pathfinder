package hyy.pathfinder.Objects;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Prometheus on 20-Nov-16.
 */

public class StationList extends ArrayList<Station>
{
    public LatLng GetLatLng(String shortCode)
    {
        for(int i = 0;i<this.size();i++)
        {
           // Log.d("StationList", "comparing " + this.get(i).getStationShortCode() + " and " + shortCode);
            if(this.get(i).getStationShortCode().equals(shortCode))
            {
                return new LatLng(Double.parseDouble(this.get(i).getLatitude()),Double.parseDouble(this.get(i).getLongitude()));
            }
        }
        Log.d("StationList", "Coordinates not found! Returning null.");
        return null;
    }
}
