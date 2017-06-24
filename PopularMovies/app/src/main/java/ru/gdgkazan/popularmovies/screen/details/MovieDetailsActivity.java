package ru.gdgkazan.popularmovies.screen.details;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmResults;
import ru.gdgkazan.popularmovies.R;
import ru.gdgkazan.popularmovies.model.content.Movie;
import ru.gdgkazan.popularmovies.model.content.Review;
import ru.gdgkazan.popularmovies.model.content.Video;
import ru.gdgkazan.popularmovies.model.response.ReviewsResponse;
import ru.gdgkazan.popularmovies.model.response.VideosResponse;
import ru.gdgkazan.popularmovies.network.ApiFactory;
import ru.gdgkazan.popularmovies.screen.loading.LoadingDialog;
import ru.gdgkazan.popularmovies.screen.loading.LoadingView;
import ru.gdgkazan.popularmovies.utils.Images;
import ru.gdgkazan.popularmovies.utils.Videos;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MovieDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String MAXIMUM_RATING = "10";

    public static final String IMAGE = "image";
    public static final String EXTRA_MOVIE = "extraMovie";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.toolbar_layout)
    CollapsingToolbarLayout mCollapsingToolbar;

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.title)
    TextView mTitleTextView;

    @BindView(R.id.overview)
    TextView mOverviewTextView;

    @BindView(R.id.reviews)
    TextView mReviewsTextView;

    @BindView(R.id.rating)
    TextView mRatingTextView;

    @BindView(R.id.content_linear_layout)
    LinearLayout linearLayout;

    private Subscription movieReviewsSubscription;
    private Subscription movieVideosSubscription;
    private LoadingView loadingView;
    private int hideCounter;

    public static void navigate(@NonNull AppCompatActivity activity, @NonNull View transitionImage,
                                @NonNull Movie movie) {
        Intent intent = new Intent(activity, MovieDetailsActivity.class);
        intent.putExtra(EXTRA_MOVIE, movie);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage, IMAGE);
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareWindowForAnimation();
        setContentView(R.layout.activity_movie_details);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        ViewCompat.setTransitionName(findViewById(R.id.app_bar), IMAGE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Movie movie = getIntent().getParcelableExtra(EXTRA_MOVIE);
        showMovie(movie);

        loadingView = LoadingDialog.view(getSupportFragmentManager());
        movieReviewsSubscription = ApiFactory.getMoviesService()
                .movieReviews(movie.getId())
                .map(ReviewsResponse::getReviews)
                .flatMap(x -> {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        realm.delete(Review.class);
                        realm.insert(x);
                    });
                    return Observable.just(x);
                })
                .onErrorResumeNext(throwable -> {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Review> results = realm.where(Review.class).findAll();
                    return Observable.just(realm.copyFromRealm(results));
                })
                .doOnSubscribe(loadingView::showLoadingIndicator)
                .doAfterTerminate(this::hideLoadingIndicator)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showReviews, x -> Log.d("logs, errors", x.getMessage()));

        movieVideosSubscription = ApiFactory.getMoviesService()
                .movieVideos(movie.getId())
                .map(VideosResponse::getVideos)
                .flatMap(x -> {
                    Realm.getDefaultInstance().executeTransaction(realm -> {
                        realm.delete(Video.class);
                        realm.insert(x);
                    });
                    return Observable.just(x);
                })
                .onErrorResumeNext(throwable -> {
                    Realm realm = Realm.getDefaultInstance();
                    RealmResults<Video> results = realm.where(Video.class).findAll();
                    return Observable.just(realm.copyFromRealm(results));
                })
                .doOnSubscribe(loadingView::showLoadingIndicator)
                .doAfterTerminate(this::hideLoadingIndicator)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showTrailers, x -> Log.d("logs, errors", x.getMessage()));
    }

    private void hideLoadingIndicator() {
        hideCounter++;
        Log.d("hideLoadingIndicator", "" + hideCounter);
        if (hideCounter == 2 && loadingView != null)
            loadingView.hideLoadingIndicator();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void prepareWindowForAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide transition = new Slide();
            transition.excludeTarget(android.R.id.statusBarBackground, true);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setEnterTransition(transition);
            getWindow().setReturnTransition(transition);
        }
    }

    private void showMovie(@NonNull Movie movie) {
        String title = getString(R.string.movie_details);
        mCollapsingToolbar.setTitle(title);
        mCollapsingToolbar.setExpandedTitleColor(ContextCompat.getColor(this, android.R.color.transparent));

        Images.loadMovie(mImage, movie, Images.WIDTH_780);

        String year = movie.getReleasedDate().substring(0, 4);
        mTitleTextView.setText(getString(R.string.movie_title, movie.getTitle(), year));
        mOverviewTextView.setText(movie.getOverview());

        String average = String.valueOf(movie.getVoteAverage());
        average = average.length() > 3 ? average.substring(0, 3) : average;
        average = average.length() == 3 && average.charAt(2) == '0' ? average.substring(0, 1) : average;
        mRatingTextView.setText(getString(R.string.rating, average, MAXIMUM_RATING));
    }

    private void showTrailers(List<Video> videos) {
        if (videos == null || videos.size() == 0)
            return;
        for (int i = 0; i < videos.size(); ++i) {
            TextView textView = new TextView(this);
            textView.setText(videos.get(i).getName());
            textView.setTextSize(18);
            textView.setTag(videos.get(i));
            textView.setOnClickListener(this);
            linearLayout.addView(textView);
        }
    }

    private void showReviews(List<Review> reviews) {
        if (reviews == null || reviews.size() == 0)
            return;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < reviews.size(); ++i) {
            Review currentReview = reviews.get(i);
            stringBuilder.append(currentReview.getAuthor());
            stringBuilder.append("\n");
            stringBuilder.append(currentReview.getContent());
            stringBuilder.append("\n\n");
        }
        mReviewsTextView.setText(stringBuilder.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (movieReviewsSubscription != null)
            movieReviewsSubscription.unsubscribe();

        if (movieVideosSubscription != null)
            movieVideosSubscription.unsubscribe();
    }

    @Override
    public void onClick(View v) {
        Video video = (Video) v.getTag();
        Videos.browseVideo(this, video);
    }
}
