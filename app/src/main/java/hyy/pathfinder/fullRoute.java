package hyy.pathfinder;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Kotimonni on 15.11.2016.
 */

public class fullRoute {
    public Location originLocation;
    public Location destinationLocation;
    public String originAddress;
    public String destinationAddress;
    public String[] originClosestStation;
    public String[] destinationClosestStation;
    public Integer duration;
    public Integer distance;
    public PolylineOptions polylineOptions;
    public List<routeSegment> routeSegmentList = new ArrayList<>();
    public String originDate;
    public Date originTime;

    private Integer walkDurationToOriginStation;
    private Integer walkDistanceToOriginStation;
    private Integer walkDurationFromDestinationStation;
    private Integer walkDistanceFromDestinationStation;

    public void addRouteSegment() {
        this.routeSegmentList.add(new routeSegment());
    }


    public fullRoute(){}


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

    public String[] getOriginClosestStation() {
        return originClosestStation;
    }

    public void setOriginClosestStation(String[] originClosestStation) {
        this.originClosestStation = originClosestStation;
    }

    public String[] getDestinationClosestStation() {
        return destinationClosestStation;
    }

    public void setDestinationClosestStation(String[] destinationClosestStation) {
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
}
