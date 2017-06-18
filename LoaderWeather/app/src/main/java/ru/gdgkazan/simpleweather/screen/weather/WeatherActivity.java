package ru.gdgkazan.simpleweather.screen.weather;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.gdgkazan.simpleweather.R;
import ru.gdgkazan.simpleweather.model.City;
import ru.gdgkazan.simpleweather.screen.general.LoadingDialog;
import ru.gdgkazan.simpleweather.screen.general.LoadingView;

public class WeatherActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String CITY_NAME_KEY = "city_name";
    private City mCity;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_title)
    TextView mToolbarTitle;

    @BindView(R.id.weather_layout)
    View mWeatherLayout;

    @BindView(R.id.weather_main)
    TextView mWeatherMain;

    @BindView(R.id.temperature)
    TextView mTemperature;

    @BindView(R.id.pressure)
    TextView mPressure;

    @BindView(R.id.humidity)
    TextView mHumidity;

    @BindView(R.id.wind_speed)
    TextView mWindSpeed;

    @BindView(R.id.error_layout)
    TextView mErrorLayout;

    @BindView(R.id.swipe_container)
    SwipeRefreshLayout refreshLayout;

    private LoadingView mLoadingView;
    private String mCityName;

    @NonNull
    public static Intent makeIntent(@NonNull Activity activity, @NonNull City city) {
        Intent intent = new Intent(activity, WeatherActivity.class);
        intent.putExtra(CITY_NAME_KEY, city);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        mCityName = getIntent().getStringExtra(CITY_NAME_KEY);
        mToolbarTitle.setText(mCityName);
        refreshLayout.setOnRefreshListener(this);
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());
        if (savedInstanceState != null && savedInstanceState.containsKey(CITY_NAME_KEY)) {
            Log.d("logs", "restoring weather");
            mCity = (City) savedInstanceState.getSerializable(CITY_NAME_KEY);
        } else {
            Intent intent = getIntent();
            if (!intent.hasExtra(CITY_NAME_KEY)) {
                showError();
            } else {
                Log.d("logs", "extracting weather from intent");
                mCity = (City) intent.getSerializableExtra(CITY_NAME_KEY);
            }
        }
        showWeather(mCity);
    }

    private void showWeather(@Nullable City city) {
        if (city == null || city.getMain() == null || city.getWeather() == null
                || city.getWind() == null) {
            showError();
            return;
        }
        mLoadingView.hideLoadingIndicator();

        mWeatherLayout.setVisibility(View.VISIBLE);
        mErrorLayout.setVisibility(View.GONE);

        mToolbarTitle.setText(city.getName());
        mWeatherMain.setText(city.getWeather().getMain());
        mTemperature.setText(getString(R.string.f_temperature, city.getMain().getTemp()));
        mPressure.setText(getString(R.string.f_pressure, city.getMain().getPressure()));
        mHumidity.setText(getString(R.string.f_humidity, city.getMain().getHumidity()));
        mWindSpeed.setText(getString(R.string.f_wind_speed, city.getWind().getSpeed()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(CITY_NAME_KEY, mCity);
    }

    private void showError() {
        mLoadingView.hideLoadingIndicator();
        mWeatherLayout.setVisibility(View.INVISIBLE);
        mErrorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        LoaderManager.LoaderCallbacks<HashMap<String, City>> callbacks =
                new WeatherCallbacks();
        getSupportLoaderManager().restartLoader(R.id.weather_loader_id, Bundle.EMPTY, callbacks);
    }

    private class WeatherCallbacks implements LoaderManager.LoaderCallbacks<HashMap<String, City>> {

        @Override
        public Loader<HashMap<String, City>> onCreateLoader(int id, Bundle args) {
            return new RetrofitWeatherLoader(
                    WeatherActivity.this,
                    getResources().getStringArray(R.array.initial_cities));
        }

        @Override
        public void onLoadFinished(
                Loader<HashMap<String, City>> loader,
                HashMap<String, City> data) {
            if (data.containsKey(mCityName))
                showWeather(data.get(mCityName));
            mCity = data.get(mCityName);
            refreshLayout.setRefreshing(false);
        }

        @Override
        public void onLoaderReset(Loader<HashMap<String, City>> loader) {
            loader.forceLoad();
        }
    }
}
