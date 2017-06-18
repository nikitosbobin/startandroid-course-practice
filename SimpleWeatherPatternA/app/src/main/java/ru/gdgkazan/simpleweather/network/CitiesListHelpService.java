package ru.gdgkazan.simpleweather.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;

/**
 * @author Nikita Bobin
 */
public interface CitiesListHelpService {
    @GET("help/city_list.txt")
    Call<List<WeatherCity>> getCitiesList();
}
