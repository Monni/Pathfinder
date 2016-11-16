package hyy.pathfinder.Objects;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kotimonni on 15.11.2016.
 */

public class routeSegment implements Parcelable {

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

    public routeSegment() {}

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