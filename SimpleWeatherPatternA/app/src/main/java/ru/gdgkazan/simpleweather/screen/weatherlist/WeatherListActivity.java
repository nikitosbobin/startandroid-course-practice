package ru.gdgkazan.simpleweather.screen.weatherlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.arturvasilov.sqlite.core.BasicTableObserver;
import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.sqlite.core.Where;
import ru.arturvasilov.sqlite.rx.RxSQLite;
import ru.gdgkazan.simpleweather.R;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.tables.CityTable;
import ru.gdgkazan.simpleweather.data.tables.RequestTable;
import ru.gdgkazan.simpleweather.network.NetworkService;
import ru.gdgkazan.simpleweather.network.model.NetworkRequest;
import ru.gdgkazan.simpleweather.network.model.Request;
import ru.gdgkazan.simpleweather.network.model.RequestStatus;
import ru.gdgkazan.simpleweather.screen.general.LoadingDialog;
import ru.gdgkazan.simpleweather.screen.general.LoadingView;
import ru.gdgkazan.simpleweather.screen.general.SimpleDividerItemDecoration;
import ru.gdgkazan.simpleweather.screen.weather.WeatherActivity;
import ru.gdgkazan.simpleweather.utils.RxSchedulers;
import rx.Observable;

/**
 * @author Artur Vasilov
 */
public class WeatherListActivity
        extends AppCompatActivity
        implements CitiesAdapter.OnItemClick, BasicTableObserver, SwipeRefreshLayout.OnRefreshListener {

    private static final String CITIES_FORECASTS_KEY = "citiesForecasts";
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
    private List<City> citiesForecasts;

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
        if (savedInstanceState != null && savedInstanceState.containsKey(CITIES_FORECASTS_KEY)) {
            SerializableArray<City> citiesArray = (SerializableArray<City>) savedInstanceState.getSerializable(CITIES_FORECASTS_KEY);
            citiesForecasts = citiesArray.getItems();
            mAdapter.changeDataSet(citiesForecasts);
        } else {
            loadWeather(false);
        }
    }

    @Override
    public void onItemClick(@NonNull City city) {
        startActivity(WeatherActivity.makeIntent(this, city.getId()));
    }

    @NonNull
    private List<City> getInitialCities() {
        return Arrays.stream(getResources().getStringArray(R.array.initial_cities))
                .map(City::new)
                .collect(Collectors.toList());
    }

    private void loadWeather(boolean refresh) {
        if (!refresh)
            mLoadingView.showLoadingIndicator();
        String[] citiesNames = getResources().getStringArray(R.array.initial_cities);
        SQLite.get().registerObserver(RequestTable.TABLE, this);
        Request request = new Request(NetworkRequest.CITY_WEATHER);
        NetworkService.start(this, request, citiesNames);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SQLite.get().unregisterObserver(this);
    }

    @Override
    public void onTableChanged() {
        Where where = Where.create().equalTo(RequestTable.REQUEST, NetworkRequest.CITY_WEATHER);
        RxSQLite.get().querySingle(RequestTable.TABLE, where)
                .compose(RxSchedulers.async())
                .flatMap(request -> {
                    if (request.getStatus() == RequestStatus.IN_PROGRESS) {
                        mLoadingView.showLoadingIndicator();
                        return Observable.empty();
                    } else if (request.getStatus() == RequestStatus.ERROR) {
                        return Observable.error(new IOException(request.getError()));
                    }
                    return RxSQLite.get().query(CityTable.TABLE).compose(RxSchedulers.async());
                })
                .subscribe(
                        cities -> {
                            this.citiesForecasts = cities;
                            mAdapter.changeDataSet(cities
                                    .stream()
                                    .sorted((x, y) -> x.getName().compareTo(y.getName()))
                                    .collect(Collectors.toList()));
                            mLoadingView.hideLoadingIndicator();
                            refreshLayout.setRefreshing(false);
                        },
                        throwable -> {
                            mLoadingView.hideLoadingIndicator();
                            refreshLayout.setRefreshing(false);
                            Snackbar.make(mEmptyView, "Error", Snackbar.LENGTH_SHORT).show();
                        });
    }

    @Override
    public void onRefresh() {
        loadWeather(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (citiesForecasts == null)
            return;
        outState.putSerializable(CITIES_FORECASTS_KEY, new SerializableArray<>(citiesForecasts));
    }

    private static class SerializableArray<TItem extends Serializable> implements Serializable {
        private List<TItem> items;

        public SerializableArray(List<TItem> items) {
            this.items = items;
        }

        public List<TItem> getItems() {
            return items;
        }
    }
}
