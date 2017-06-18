package ru.gdgkazan.simpleweather.network;

import android.support.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import ru.gdgkazan.simpleweather.BuildConfig;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;

/**
 * @author Nikita Bobin
 */
public final class ApiFactoryCitiesList {
    private static CitiesListHelpService service;
    private static OkHttpClient httpClient;

    @NonNull
    public static CitiesListHelpService getCitiesListHelpService() {
        if (service == null) {
            synchronized (ApiFactory.class) {
                service = buildRetrofit().create(CitiesListHelpService.class);
            }
        }
        return service;
    }

    @NonNull
    private static Retrofit buildRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.HELP_ENDPOINT)
                .client(getClient())
                .addConverterFactory(new CitiesListConverterFactory())
                .build();
    }

    @NonNull
    private static OkHttpClient getClient() {
        if (httpClient == null) {
            synchronized (ApiFactory.class) {
                httpClient = buildClient();
            }
        }
        return httpClient;
    }

    @NonNull
    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BASIC))
                .build();
    }

    private static class CitiesListConverterFactory extends Converter.Factory {
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(
                Type type,
                Annotation[] annotations,
                Retrofit retrofit) {
            return new CitiesListConverter();
        }
    }

    private static class CitiesListConverter
            implements Converter<ResponseBody, List<WeatherCity>> {

        @Override
        public List<WeatherCity> convert(ResponseBody value) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(value.byteStream()));
            try {
                return bufferedReader.lines()
                        .skip(1)
                        .map(x -> {
                            String[] segments = x.split("\t");
                            return new WeatherCity(Integer.parseInt(segments[0]), segments[1]);
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return new ArrayList<>(0);
            }
        }
    }
}
