package ru.gdgkazan.simpleweather.screen.weather;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;

import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.gdgkazan.simpleweather.model.City;
import ru.gdgkazan.simpleweather.network.ApiFactory;

/**
 * @author Artur Vasilov
 */
public class RetrofitWeatherLoader extends Loader<HashMap<String, City>> {

    @NonNull
    private final String[] citiesNames;
    private ArrayList<Call<City>> citiesCalls;
    private HashMap<String, City> cities;

    public RetrofitWeatherLoader(Context context, @NonNull String[] citiesNames) {
        super(context);
        this.citiesNames = citiesNames;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (cities != null) {
            deliverResult(cities);
        } else {
            forceLoad();
        }
    }

    @Override
    protected void onForceLoad() {
        super.onForceLoad();
        citiesCalls = new ArrayList<>(citiesNames.length);
        for (int i = 0; i < citiesNames.length; ++i) {
            Call<City> cityLoadCall = ApiFactory.getWeatherService().getWeather(citiesNames[i]);
            citiesCalls.add(cityLoadCall);
            cityLoadCall.enqueue(new Callback<City>() {
                @Override
                public void onResponse(Call<City> call, Response<City> response) {
                    City city = response.body();
                    putToResult(city);
                    if (citiesCalls.size() == cities.size())
                        deliverResult(cities);
                }

                @Override
                public void onFailure(Call<City> call, Throwable t) {
                    deliverResult(null);
                }
            });
        }
    }

    private synchronized void putToResult(City city) {
        if (cities == null)
            cities = new HashMap<>();
        cities.put(city.getName(), city);
    }

    @Override
    protected void onStopLoading() {
        if (citiesCalls == null)
            return;
        for (int i = 0; i < citiesCalls.size(); ++i)
            citiesCalls.get(i).cancel();
        super.onStopLoading();
    }
}

