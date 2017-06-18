package ru.gdgkazan.simpleweather.screen.weatherlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.gdgkazan.simpleweather.R;
import ru.gdgkazan.simpleweather.model.City;
import ru.gdgkazan.simpleweather.screen.general.LoadingDialog;
import ru.gdgkazan.simpleweather.screen.general.LoadingView;
import ru.gdgkazan.simpleweather.screen.general.SimpleDividerItemDecoration;
import ru.gdgkazan.simpleweather.screen.weather.RetrofitWeatherLoader;
import ru.gdgkazan.simpleweather.screen.weather.WeatherActivity;

/**
 * @author Artur Vasilov
 */
public class WeatherListActivity
        extends AppCompatActivity
        implements CitiesAdapter.OnItemClick, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty)
    View mEmptyView;

    @BindView(R.id.swipe_container)
    SwipeRefreshLayout refreshLayout;

    private CitiesAdapter mAdapter;
    private LoadingView mLoadingView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_list);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, false));
        mAdapter = new CitiesAdapter(getInitialCities(), this);
        mRecyclerView.setAdapter(mAdapter);
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());
        refreshLayout.setOnRefreshListener(this);
        loadWeather(false);
    }

    @Override
    public void onItemClick(@NonNull City city) {
        refreshLayout.setRefreshing(false);
        startActivity(WeatherActivity.makeIntent(this, city));
    }

    @NonNull
    private List<City> getInitialCities() {
        List<City> cities = new ArrayList<>();
        String[] initialCities = getResources().getStringArray(R.array.initial_cities);
        for (String city : initialCities) {
            cities.add(new City(city));
        }
        return cities;
    }

    private void loadWeather(boolean restart) {
        if (!restart)
            mLoadingView.showLoadingIndicator();
        LoaderManager.LoaderCallbacks<HashMap<String, City>> callbacks =
                new WeatherCallbacks();
        if (restart) {
            getSupportLoaderManager().restartLoader(
                    R.id.weather_loader_id,
                    Bundle.EMPTY,
                    callbacks);
        } else {
            getSupportLoaderManager().initLoader(
                    R.id.weather_loader_id,
                    Bundle.EMPTY,
                    callbacks);
        }
    }

    @Override
    public void onRefresh() {
        loadWeather(true);
    }

    private class WeatherCallbacks implements LoaderManager.LoaderCallbacks<HashMap<String, City>> {

        @Override
        public Loader<HashMap<String, City>> onCreateLoader(int id, Bundle args) {
            return new RetrofitWeatherLoader(
                    WeatherListActivity.this,
                    getResources().getStringArray(R.array.initial_cities));
        }

        @Override
        public void onLoadFinished(
                Loader<HashMap<String, City>> loader,
                HashMap<String, City> data) {
            mLoadingView.hideLoadingIndicator();
            refreshLayout.setRefreshing(false);
            mAdapter.changeDataSet(toList(
                    data.values(),
                    (left, right) -> left.getName().compareTo(right.getName())));
        }

        @Override
        public void onLoaderReset(Loader<HashMap<String, City>> loader) {
            loader.stopLoading();
        }

        private <TItem> List<TItem> toList(
                Collection<TItem> items,
                @Nullable Comparator<? super TItem> comparator) {
            ArrayList<TItem> result = new ArrayList<>(items.size());
            result.addAll(items);
            if (comparator != null)
                Collections.sort(result, comparator);
            return result;
        }
    }
}
