package hyy.pathfinder;

/**
 * Created by Kotimonni on 15.11.2016.
 */

public class routeSegment {

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
}
