package ru.gdgkazan.simpleweather.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.sqlite.core.Where;
import ru.gdgkazan.simpleweather.data.GsonHolder;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.model.WeatherArray;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;
import ru.gdgkazan.simpleweather.data.tables.CityTable;
import ru.gdgkazan.simpleweather.data.tables.RequestTable;
import ru.gdgkazan.simpleweather.data.tables.WeatherCityTable;
import ru.gdgkazan.simpleweather.network.model.NetworkRequest;
import ru.gdgkazan.simpleweather.network.model.Request;
import ru.gdgkazan.simpleweather.network.model.RequestStatus;

/**
 * @author Artur Vasilov
 */
public class NetworkService extends IntentService {

    private static final String REQUEST_KEY = "request";
    private static final String CITY_NAME_KEY = "city_name";

    public static void start(
            @NonNull Context context,
            @NonNull Request request,
            @NonNull String[] citiesNames) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(REQUEST_KEY, GsonHolder.getGson().toJson(request));
        intent.putExtra(CITY_NAME_KEY, citiesNames);
        context.startService(intent);
    }

    @SuppressWarnings("unused")
    public NetworkService() {
        super(NetworkService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Request request = GsonHolder.getGson().fromJson(
                intent.getStringExtra(REQUEST_KEY),
                Request.class);
        Request savedRequest = SQLite.get().querySingle(RequestTable.TABLE,
                Where.create().equalTo(RequestTable.REQUEST, request.getRequest()));

        if (savedRequest != null && request.getStatus() == RequestStatus.IN_PROGRESS) {
            return;
        }
        request.setStatus(RequestStatus.IN_PROGRESS);
        SQLite.get().insert(RequestTable.TABLE, request);
        SQLite.get().notifyTableChanged(RequestTable.TABLE);

        if (TextUtils.equals(NetworkRequest.CITY_WEATHER, request.getRequest())) {
            String[] citiesNames = intent.getStringArrayExtra(CITY_NAME_KEY);
            executeCityRequest(request, citiesNames);
        }
    }

    private void executeCityRequest(@NonNull Request request, @NonNull String[] cityNames) {
        try {
            ensureSupportedCitiesLoaded();
            String citiesIds = getCitiesIds(cityNames);
            WeatherArray citiesForecasts = ApiFactory.getWeatherService()
                    .getWeatherList(citiesIds)
                    .execute()
                    .body();
            SQLite.get().delete(CityTable.TABLE);
            SQLite.get().insert(CityTable.TABLE, citiesForecasts.getCitiesForecasts());
            request.setStatus(RequestStatus.SUCCESS);
        } catch (IOException e) {
            request.setStatus(RequestStatus.ERROR);
            request.setError(e.getMessage());
        } finally {
            SQLite.get().insert(RequestTable.TABLE, request);
            SQLite.get().notifyTableChanged(RequestTable.TABLE);
        }
    }

    private String getCitiesIds(String[] cityNames) {
        Where where = Where.create();
        for (int i = 0; i < cityNames.length; ++i) {
            if (i != 0)
                where = where.or();
            where = where.equalTo(WeatherCityTable.CITY_NAME, cityNames[i]);
        }
        List<Integer> ids = SQLite.get().query(WeatherCityTable.TABLE, where)
                .stream()
                .collect(Collectors.groupingBy(WeatherCity::getCityName))
                .entrySet()
                .stream()
                .map(x -> x.getValue().get(0).getCityId())
                .collect(Collectors.toList());
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < ids.size(); ++i) {
            stringBuilder.append(ids.get(i));
            if (i != ids.size() - 1)
                stringBuilder.append(',');
        }
        return stringBuilder.toString();
    }

    private void ensureSupportedCitiesLoaded() throws IOException {
        WeatherCity weatherCity = SQLite.get().querySingle(WeatherCityTable.TABLE);
        if (weatherCity == null) {
            List<WeatherCity> cities = ApiFactoryCitiesList.getCitiesListHelpService()
                    .getCitiesList()
                    .execute()
                    .body();
            SQLite.get().insert(WeatherCityTable.TABLE, cities);
        }
    }
}

