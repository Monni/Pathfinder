package hyy.pathfinder.Objects;

/**
 * Created by Prometheus on 20-Nov-16.
 */

public class Station
{
    private String latitude;
    private String longitude;
    private String stationName;
    private String stationUICCode;
    private String stationShortCode;
    private String countryCode;
    private String type; // asema vai pysähdyspaikka

    public Station(){}

    public Station(String name, String lat, String lng, String UIC, String shortCode, String stationType, String countrycode)
    {
        stationName = name;
        latitude = lat;
        longitude = lng;
        stationUICCode = UIC;
        stationShortCode = shortCode;
        type = stationType;
        countryCode = countrycode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getStationShortCode() {
        return stationShortCode;
    }

    public void setStationShortCode(String stationShortCode) {
        this.stationShortCode = stationShortCode;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStationUICCode() {
        return stationUICCode;
    }

    public void setStationUICCode(String stationUICCode) {
        this.stationUICCode = stationUICCode;
    }




}
