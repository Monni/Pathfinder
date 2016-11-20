package hyy.pathfinder.Objects;

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
            if(this.get(i).getStationShortCode().equals(shortCode))
            {
                return new LatLng(Double.parseDouble(this.get(i).getLatitude()),Double.parseDouble(this.get(i).getLongitude()));
            }
        }
        return null;
    }
}
