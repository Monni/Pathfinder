package hyy.pathfinder;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Created by Prometheus on 11-Oct-16.
 */



public class Route
{
    public LatLng origin;
    public LatLng destination;
    public Integer duration;
    public Integer distance;
    public PolylineOptions polylineOptions;
    public Integer id;
    public String originAddress;
    public String destinationAddress;
    public int index;
    public int listIndex;
    public String url;
    public String originClosestStation;
    public String destinationClosestStation;

    public String trainNumber;
    public String trainType;
    public String depType;
    public String depTrack;
    public String depDate;
    public String depTime;
    public String arrType;
    public String arrTrack;
    public String arrTime;


    public Route(){}
    public Route (String Url)
    {
        url = Url;
    }
    // routen täytyy tietää mihin listaan (kokonaisreittiin, listIndex) se kuuluu ja monesko se on siinä listassa (monesko osa kokonaisreittiä, index)
    public Route(String Url, int Index, int ListIndex)
    {
        index = Index;
        url = Url;
        listIndex = ListIndex;
    }
    public Route(LatLng Origin, LatLng Destination, String OriginAddress, String DestinationAddress, Integer Duration, Integer Distance)
    {
        origin = Origin;
        destination = Destination;
        originAddress = OriginAddress;
        destinationAddress = DestinationAddress;
        duration = Duration;
        distance = Distance;
        url = "";

        CreatePolyline();
    }

    // MIIKAN TAIKAA
    public Route(String trainNumber, String trainType, String depType, String depTrack, String depDate, String depTime, String arrType, String arrTrack,
                 String arrDate, String arrTime) {
        this.trainNumber = trainNumber;
        this.trainType = trainType;
        this.depType = depType;
        this.depTrack = depTrack;
        this.depDate = depDate;
        this.depTime = depTime;
        this.arrType = arrType;
        this.arrTrack = arrTrack;
        this.arrTime = arrTime;

    }

    public void addStop(){

    }
    //

    protected void CreatePolyline()
    {
        polylineOptions = new PolylineOptions();
        polylineOptions.add(origin);
        polylineOptions.add(destination);
    }



}

