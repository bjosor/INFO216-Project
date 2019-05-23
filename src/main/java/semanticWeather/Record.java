package semanticWeather;

public class Record {
    private String sensorId;
    private String name;
    private String county;
    private String municipality;
    private String element;
    private String date;
    private String value;

    public Record(String sensorId, String name, String county, String municipality, String element, String date, String value){

        this.sensorId = sensorId;
        this.name = name;
        this.county = county;
        this.municipality = municipality;
        this.element = element;
        this.date = date;
        this.value = value;

    }

    public String getSensorId() {
        return sensorId;
    }

    public String getName() {
        return name;
    }

    public String getCounty() {
        return county;
    }

    public String getMunicipality() {
        return municipality;
    }

    public String getElement() {
        return element;
    }

    public String getValue() {
        return value;
    }

    public String getDate() {
        return date;
    }
}
