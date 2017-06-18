package ru.gdgkazan.simpleweather.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * @author Nikita Bobin
 */
public class WeatherArray implements Serializable {
    @SerializedName("list")
    private List<City> citiesForecasts;


    public List<City> getCitiesForecasts() {
        return citiesForecasts;
    }
}
