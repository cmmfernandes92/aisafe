package eapli.aisafe.flightmanagement.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class WeatherAssignment implements Serializable {

    private int legNumber;
    private int segmentIndex;
    private String weatherDataId;

    protected WeatherAssignment() {
        // for ORM
    }

    public WeatherAssignment(int legNumber, int segmentIndex, String weatherDataId) {
        this.legNumber = legNumber;
        this.segmentIndex = segmentIndex;
        this.weatherDataId = weatherDataId;
    }

    public int legNumber() { return legNumber; }

    public int segmentIndex() { return segmentIndex; }

    public String weatherDataId() { return weatherDataId; }
}
