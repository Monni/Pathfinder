package hyy.pathfinder.Objects;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import hyy.pathfinder.Data.ApplicationData;
import hyy.pathfinder.Data.Router;
import hyy.pathfinder.Interfaces.RouterResponse;

/**
 * Created by Kotimonni on 15.11.2016.
 */

public class routeSegment implements Parcelable, RouterResponse {

    private String trainNumber;
    private String trainType;
    private String depType;
    private String depTrack;
    private String depDate;
    private String depTime;
    private String arrType;
    private String arrTrack;
    private String arrDate;
    private String arrTime;
    private LatLng origin;
    private LatLng destination;
    private PolylineOptions polylineOptions;
    private Boolean isTrainSegment;
    private List<LatLng> trainTrackData; // TODO: parempi nimi tälle? sisältää juna-asemat joidenka läpi juna kulkee.
    private String originStationName;
    private String destinationStationName;

    public List<LatLng> getTrainTrackData() {
        return trainTrackData;
    }

    public void setTrainTrackData(List<LatLng> TrainTrackData) {
        this.trainTrackData = TrainTrackData;
    }

    public routeSegment() {
        isTrainSegment = false;
        trainTrackData = new ArrayList<>();
    }

    public routeSegment(List<LatLng> trainRoute, String TrainNumber, String TrainType)
    {
        isTrainSegment = true;
        trainTrackData = trainRoute;
        origin = trainRoute.get(0);
        destination = trainRoute.get(trainRoute.size()-1);
        trainNumber = TrainNumber;
        trainType = TrainType;
    }

    public routeSegment(routeSegment segment)
    {
        trainNumber = segment.getTrainNumber();
        trainType = segment.getTrainType();
        depType = segment.getDepType();
        depTrack = segment.getDepTrack();
        depDate = segment.getDepDate();
        depTime = segment.getDepTime();
        arrType = segment.getArrType();
        arrTrack = segment.getArrTrack();
        arrDate = segment.getArrDate();
        arrTime = segment.getArrTime();
        trainTrackData = segment.getTrainTrackData();
        origin = segment.getOrigin();
        destination = segment.getDestination();
        originStationName = segment.getOriginStationName();
        destinationStationName = segment.getDestinationStationName();
        isTrainSegment = segment.IsTrainSegment();
    }

    public void DrawSegmentInMap()
    {
        try {
            ApplicationData.mMap.addPolyline(getPolylineOptions());

            // muutetaan karttanäkymää
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(getOrigin());
            builder.include(getDestination());
            LatLngBounds bounds = builder.build();
            int padding = 50; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            ApplicationData.mMap.animateCamera(cu);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Boolean IsTrainSegment() {
        return isTrainSegment;
    }

    public void IsTrainSegment(Boolean trainSegment) {
        isTrainSegment = trainSegment;
    }
    public PolylineOptions getPolylineOptions()
    {
        return polylineOptions;
    }

    public void setPolylineOptions(PolylineOptions pOptions)
    {
        polylineOptions = pOptions;
    }

    public LatLng getOrigin()
    {
        return origin;
    }

    public void setOrigin(LatLng Origin)
    {
        origin = Origin;
    }

    public LatLng getDestination()
    {
        return destination;
    }

    public void setDestination(LatLng Destination)
    {
        destination = Destination;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getTrainType() {
        return trainType;
    }

    public void setTrainType(String trainType) {
        this.trainType = trainType;
    }

    public String getDepType() {
        return depType;
    }

    public void setDepType(String depType) {
        this.depType = depType;
    }

    public String getDepTrack() {
        return depTrack;
    }

    public void setDepTrack(String depTrack) {
        this.depTrack = depTrack;
    }

    public String getDepDate() {
        return depDate;
    }

    public void setDepDate(String depDate) {
        this.depDate = depDate;
    }

    public String getDepTime() {
        return depTime;
    }

    public void setDepTime(String depTime) {
        this.depTime = depTime;
    }

    public String getArrType() {
        return arrType;
    }

    public void setArrType(String arrType) {
        this.arrType = arrType;
    }

    public String getArrTrack() {
        return arrTrack;
    }

    public void setArrTrack(String arrTrack) {
        this.arrTrack = arrTrack;
    }

    public String getArrTime() {
        return arrTime;
    }

    public void setArrTime(String arrTime) {
        this.arrTime = arrTime;
    }

    public String getArrDate() {
        return arrDate;
    }

    public void setArrDate(String arrDate) {
        this.arrDate = arrDate;
    }

    public String getOriginStationName() {
        return originStationName;
    }

    public void setOriginStationName(String originStationName) {
        this.originStationName = originStationName;
    }

    public String getDestinationStationName() {
        return destinationStationName;
    }

    public void setDestinationStationName(String destinationStationName) {
        this.destinationStationName = destinationStationName;
    }


    public void BuildPolylineOptions(Context Context)
    {
        if(isTrainSegment)
        {
            PolylineOptions pOptions = new PolylineOptions();
            pOptions.color(Color.GREEN);
            pOptions.width(5);
            for(int i = 0; i<trainTrackData.size();i++)
            {
                pOptions.add(trainTrackData.get(i));
            }
            polylineOptions = pOptions;
        }
        else
        {
            String originString = String.valueOf(origin.latitude) + "," + String.valueOf(origin.longitude);
            String destinationString = String.valueOf(destination.latitude) + "," + String.valueOf(destination.longitude);
            //Router router = new Router();
           // router.GetPolylineOptions(originString, destinationString, Context, this);
        }
    }

    @Override
    public void PolylineOptionsFinished(PolylineOptions options)
    {
        Log.d("PolylineOptionsFinished","Setting polylineoptions for routeSegment");
        polylineOptions = options;
    }

    /** Parcelable magic below */

    protected routeSegment(Parcel in) {
        trainNumber = in.readString();
        trainType = in.readString();
        depType = in.readString();
        depTrack = in.readString();
        depDate = in.readString();
        depTime = in.readString();
        arrType = in.readString();
        arrTrack = in.readString();
        arrDate = in.readString();
        arrTime = in.readString();
        originStationName = in.readString();
        destinationStationName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trainNumber);
        dest.writeString(trainType);
        dest.writeString(depType);
        dest.writeString(depTrack);
        dest.writeString(depDate);
        dest.writeString(depTime);
        dest.writeString(arrType);
        dest.writeString(arrTrack);
        dest.writeString(arrDate);
        dest.writeString(arrTime);
        dest.writeString(originStationName);
        dest.writeString(destinationStationName);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<routeSegment> CREATOR = new Parcelable.Creator<routeSegment>() {
        @Override
        public routeSegment createFromParcel(Parcel in) {
            return new routeSegment(in);
        }

        @Override
        public routeSegment[] newArray(int size) {
            return new routeSegment[size];
        }
    };
}