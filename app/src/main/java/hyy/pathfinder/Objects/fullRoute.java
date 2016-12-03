package hyy.pathfinder.Objects;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hyy.pathfinder.Data.ApplicationData;

/**
 * Created by Kotimonni on 15.11.2016.
 */

public class fullRoute implements Parcelable {
    public Location originLocation;
    public Location destinationLocation;
    public String originAddress;
    public String destinationAddress;
    public Station originClosestStation;
    public Station destinationClosestStation;
    public Integer duration;
    public Integer distance;
    public PolylineOptions polylineOptions;
    public List<routeSegment> routeSegmentList;
    public String originDate;
    public Date originTime;
    private String destinationDate;
    private Date destinationTime;

    private Integer walkDurationToOriginStation;
    private Integer walkDistanceToOriginStation;
    private Integer walkDurationFromDestinationStation;
    private Integer walkDistanceFromDestinationStation;


    public void addRouteSegment() {
        this.routeSegmentList.add(new routeSegment());
    }
    public void addRouteSegment(routeSegment segment){
        this.routeSegmentList.add(segment);
    }

    public fullRoute() {
        routeSegmentList = new ArrayList<>();
    }

    // Copy constructor
    public fullRoute(fullRoute masterRoute) {
        routeSegmentList = new ArrayList<>();
        this.originLocation = masterRoute.getOriginLocation();
        this.destinationLocation = masterRoute.getDestinationLocation();
        this.originAddress = masterRoute.getOriginAddress();
        this.destinationAddress = masterRoute.getDestinationAddress();
        this.originClosestStation = masterRoute.getOriginClosestStation();
        this.destinationClosestStation = masterRoute.getDestinationClosestStation();
        this.duration = masterRoute.getDuration();
        this.distance = masterRoute.getDistance();
        this.polylineOptions = masterRoute.getPolylineOptions();
        //  this.routeSegmentList = new ArrayList<>();
        this.originDate = masterRoute.getOriginDate();
        this.originTime = masterRoute.getOriginTime();
    }

    public String getOriginDate() {
        return originDate;
    }

    public void setOriginDate(String originDate) {
        this.originDate = originDate;
    }

    public Date getOriginTime() {
        return originTime;
    }

    public void setOriginTime(Date originTime) {
        this.originTime = originTime;
    }

    public Location getOriginLocation() {
        return originLocation;
    }

    public void setOriginLocation(Location originLocation) {
        this.originLocation = originLocation;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getOriginAddress() {
        return originAddress;
    }

    public void setOriginAddress(String originAddress) {
        this.originAddress = originAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Station getOriginClosestStation() {
        return originClosestStation;
    }

    public void setOriginClosestStation(Station originClosestStation) {
        this.originClosestStation = originClosestStation;
    }

    public Station getDestinationClosestStation() {
        return destinationClosestStation;
    }

    public void setDestinationClosestStation(Station destinationClosestStation) {
        this.destinationClosestStation = destinationClosestStation;
    }

    public Integer getDuration() {
        return duration;
    }

    public void addDuration(Integer duration) {
        this.duration += duration;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public PolylineOptions getPolylineOptions() {
        return polylineOptions;
    }

    public void setPolylineOptions(PolylineOptions polylineOptions) {
        this.polylineOptions = polylineOptions;
    }

    public Integer getWalkDurationToOriginStation() {
        return walkDurationToOriginStation;
    }

    public void setWalkDurationToOriginStation(Integer walkDurationToOriginStation) {
        this.walkDurationToOriginStation = walkDurationToOriginStation;
    }

    public Integer getWalkDistanceToOriginStation() {
        return walkDistanceToOriginStation;
    }

    public void setWalkDistanceToOriginStation(Integer walkDistanceToOriginStation) {
        this.walkDistanceToOriginStation = walkDistanceToOriginStation;
    }

    public Integer getWalkDurationFromDestinationStation() {
        return walkDurationFromDestinationStation;
    }

    public void setWalkDurationFromDestinationStation(Integer walkDurationFromDestinationStation) {
        this.walkDurationFromDestinationStation = walkDurationFromDestinationStation;
    }

    public Integer getWalkDistanceFromDestinationStation() {
        return walkDistanceFromDestinationStation;
    }

    public void setWalkDistanceFromDestinationStation(Integer walkDistanceFromDestinationStation) {
        this.walkDistanceFromDestinationStation = walkDistanceFromDestinationStation;
    }

    public String getDestinationDate() {
        return destinationDate;
    }

    public void setDestinationDate(String destinationDate) {
        this.destinationDate = destinationDate;
    }

    public Date getDestinationTime() {
        return destinationTime;
    }

    public void setDestinationTime(Date destinationTime) {
        this.destinationTime = destinationTime;
    }

    /** Parcelable magic below */

    protected fullRoute(Parcel in) {
        originLocation = (Location) in.readValue(Location.class.getClassLoader());
        destinationLocation = (Location) in.readValue(Location.class.getClassLoader());
        originAddress = in.readString();
        destinationAddress = in.readString();
        duration = in.readByte() == 0x00 ? null : in.readInt();
        distance = in.readByte() == 0x00 ? null : in.readInt();
        polylineOptions = (PolylineOptions) in.readValue(PolylineOptions.class.getClassLoader());
        if (in.readByte() == 0x01) {
            routeSegmentList = new ArrayList<routeSegment>();
            in.readList(routeSegmentList, routeSegment.class.getClassLoader());
        } else {
            routeSegmentList = null;
        }
        originDate = in.readString();
        long tmpOriginTime = in.readLong();
        originTime = tmpOriginTime != -1 ? new Date(tmpOriginTime) : null;
        walkDurationToOriginStation = in.readByte() == 0x00 ? null : in.readInt();
        walkDistanceToOriginStation = in.readByte() == 0x00 ? null : in.readInt();
        walkDurationFromDestinationStation = in.readByte() == 0x00 ? null : in.readInt();
        walkDistanceFromDestinationStation = in.readByte() == 0x00 ? null : in.readInt();
    }

    public void DrawRouteOnMap()
    {
        try
        {
            // piirretään
            ApplicationData.mMap.clear();
            for(int i = 0;i<routeSegmentList.size();i++)
            {
                Log.d("DrawRouteOnMap","Drawing segment " +i);
                //routeSegmentList.get(i).DrawSegmentInMap();
                ApplicationData.mMap.addPolyline(routeSegmentList.get(i).getPolylineOptions());
            }

            // muutetaan karttanäkymää
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (int i = 0; i < routeSegmentList.size();i++)
            {
                builder.include(routeSegmentList.get(i).getOrigin());
                builder.include(routeSegmentList.get(i).getDestination());
            }

            LatLngBounds bounds = builder.build();


            int padding = 80; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            try {
                ApplicationData.mMap.animateCamera(cu);// TODO meitsi kaataa softan tällä. Miika
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }


        }
        catch(NullPointerException e)
        {
            Log.d("DrawRouteOnMap", e.toString());
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(originLocation);
        dest.writeValue(destinationLocation);
        dest.writeString(originAddress);
        dest.writeString(destinationAddress);
        if (duration == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(duration);
        }
        if (distance == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(distance);
        }
        dest.writeValue(polylineOptions);
        if (routeSegmentList == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(routeSegmentList);
        }
        dest.writeString(originDate);
        dest.writeLong(originTime != null ? originTime.getTime() : -1L);
        if (walkDurationToOriginStation == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(walkDurationToOriginStation);
        }
        if (walkDistanceToOriginStation == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(walkDistanceToOriginStation);
        }
        if (walkDurationFromDestinationStation == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(walkDurationFromDestinationStation);
        }
        if (walkDistanceFromDestinationStation == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeInt(walkDistanceFromDestinationStation);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<fullRoute> CREATOR = new Parcelable.Creator<fullRoute>() {
        @Override
        public fullRoute createFromParcel(Parcel in) {
            return new fullRoute(in);
        }

        @Override
        public fullRoute[] newArray(int size) {
            return new fullRoute[size];
        }
    };
}