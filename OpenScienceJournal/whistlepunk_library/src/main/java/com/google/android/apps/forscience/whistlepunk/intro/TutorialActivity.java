package com.google.android.apps.forscience.whistlepunk.intro;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.apps.forscience.whistlepunk.MainActivity;
import com.google.android.apps.forscience.whistlepunk.R;

import java.util.ArrayList;
import java.util.List;

public class TutorialActivity extends AppCompatActivity {

    private static final String TAG = "Tutorial";

    private static final boolean LOCAL_LOGD = false;

    /**
     * If {@code true}, always load the tutorial.
     */
    private static final boolean DEBUG_TUTORIAL = false;

    private static final String KEY_TUTORIAL_VERSION_SEEN = "tutorial_version_seen";

    private static final int TUTORIAL_VERSION = 1;

    private ViewPager mPager;
    private Button mBtnPrev;
    private Button mBtnNext;
    private ViewGroup mDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        mPager = (ViewPager) findViewById(R.id.tutorial_pager);
        mDots = (ViewGroup) findViewById(R.id.dot_holder);
        Resources res = getResources();
        ArrayList<TutorialItem> items = new ArrayList<>();
        items.add(new TutorialItem(R.string.tutorial_welcome_header,
                R.string.tutorial_welcome_body,
                R.raw.walkthrough_welcome,
                res.getColor(R.color.tutorial_background_welcome)));
        items.add(new TutorialItem(R.string.tutorial_setup_header,
                R.string.tutorial_setup_body,
                R.raw.walkthrough_setup,
                res.getColor(R.color.tutorial_background_setup)));
        items.add(new TutorialItem(R.string.tutorial_observe_record_header,
                R.string.tutorial_observe_record_body,
                R.raw.walkthrough_observe_record,
                res.getColor(R.color.tutorial_background_observe_record)));
        items.add(new TutorialItem(R.string.tutorial_take_notes_header,
                R.string.tutorial_take_notes_body,
                R.raw.walkthrough_take_notes,
                res.getColor(R.color.tutorial_background_take_notes)));
        items.add(new EndItem(res));
        final TutorialAdapter adapter = new TutorialAdapter(getFragmentManager(), items);

        mBtnNext = (Button) findViewById(R.id.btn_next);
        mBtnPrev = (Button) findViewById(R.id.btn_prev);

        mBtnNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                advance();
            }
        });

        mBtnPrev.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mPager.getCurrentItem() > 0) {
                    mPager.setCurrentItem(mPager.getCurrentItem() - 1, true);
                }
            }
        });
        setupDots(items);
        // This prevents the video views from touching.
        mPager.setPageMargin(1);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private ArgbEvaluator mArgbEvaluator = new ArgbEvaluator();

            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                int color1 = adapter.getTutorialItem(position).backgroundColor;
                if (position < adapter.getCount() - 1) {
                    int color2 = adapter.getTutorialItem(position + 1).backgroundColor;
                    setStatusBarColorIfPossible((int) mArgbEvaluator.evaluate(positionOffset,
                            color1, color2));
                }
            }

            @Override
            public void onPageSelected(int position) {
                mBtnPrev.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
                final int count = mDots.getChildCount();
                for (int index = 0; index < count; index++) {
                    mDots.getChildAt(index).setSelected(index == position);
                }
                if (adapter.getTutorialItem(position).isEndItem()) {
                    finishTutorial();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mPager.setAdapter(adapter);
        setStatusBarColorIfPossible(res.getColor(R.color.tutorial_background_welcome));
    }

    private void setupDots(ArrayList<TutorialItem> items) {
        mDots.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        final int count = items.size() - 1; // Don't make a dot for the last item.
        for (int index = 0; index < count; index++) {
            ImageView view = (ImageView) inflater.inflate(R.layout.tutorial_dot_view, mDots, false);
            mDots.addView(view);
            view.setSelected(index == 0);
        }
    }

    private void finishTutorial() {
        // Need to mark ourselves as done and finish.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit()
                .putInt(KEY_TUTORIAL_VERSION_SEEN, TUTORIAL_VERSION)
                .apply();

        ImageView view = (ImageView) findViewById(R.id.logo);
        view.animate()
                .scaleXBy(3)
                .scaleYBy(3)
                .alpha(.1f)
                .setDuration(500)
                .setStartDelay(50)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        launchOrFinish();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                })
                .start();
    }

    private void launchOrFinish() {
        if (TextUtils.isEmpty(getIntent().getAction())) {
            // If this is true, we didn't come here from settings, so relaunch the main activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
        // This always throws an exception, sometimes with nasty results.  See b/28086979
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            overridePendingTransition(0, android.R.transition.explode);
//        }
    }

    public static boolean shouldShowTutorial(Context context) {
        if (DEBUG_TUTORIAL) {
            return true;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(KEY_TUTORIAL_VERSION_SEEN, 0) < TUTORIAL_VERSION;
    }

    private void setStatusBarColorIfPossible(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        }
    }

    private void advance() {
        if (mPager.getCurrentItem() < mPager.getAdapter().getCount()) {
            mPager.setCurrentItem(mPager.getCurrentItem() + 1, true);
        }
    }

    public static class TutorialAdapter extends FragmentStatePagerAdapter {

        private List<TutorialItem> mItems;

        TutorialAdapter(FragmentManager fm, List<TutorialItem> items) {
            super(fm);
            mItems = new ArrayList<>(items);
        }

        @Override
        public Fragment getItem(int position) {
            if (mItems.get(position).isEndItem()) {
                return new EndFragment();
            } else {
                return TutorialFragment.newInstance(mItems.get(position));
            }
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        /**
         * Gets the affiliated tutorial item.
         */
        public TutorialItem getTutorialItem(int position) {
            return mItems.get(position);
        }
    }

    public static class TutorialItem {
        int headerTextResource;
        int bodyTextResource;
        int movieResource;
        int backgroundColor;

        TutorialItem(int headerTextResource, int bodyTextResource, int movieResource,
                     int backgroundColor) {
            this.headerTextResource = headerTextResource;
            this.bodyTextResource = bodyTextResource;
            this.movieResource = movieResource;
            this.backgroundColor = backgroundColor;
        }

        public boolean isEndItem() {
            return false;
        }
    }

    public static class EndItem extends TutorialItem {

        EndItem(Resources res) {
            super(0, 0, 0, res.getColor(R.color.color_primary_dark));
        }

        public boolean isEndItem() {
            return true;
        }
    }

    public static class TutorialFragment extends Fragment {
        private static final String KEY_HEADER_TEXT = "header_text";
        private static final String KEY_BODY_TEXT = "body_text";
        private static final String KEY_MOVIE = "movie";
        private static final String KEY_BACKGROUND = "background";

        private VideoView mVideo;

        static TutorialFragment newInstance(TutorialItem item) {
            TutorialFragment fragment = new TutorialFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_HEADER_TEXT, item.headerTextResource);
            args.putInt(KEY_BODY_TEXT, item.bodyTextResource);
            args.putInt(KEY_MOVIE, item.movieResource);
            args.putInt(KEY_BACKGROUND, item.backgroundColor);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_tutorial_page, container, false);

            mVideo = new VideoView(getActivity().getApplicationContext());
            mVideo.setContentDescription(null);
            // Set an explicit error listener so that crashes don't kill the app.
            mVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.w(TAG, "Error: " + what + " " + extra);
                    return true;
                }
            });
            TextView headerText = (TextView) view.findViewById(R.id.tutorial_header);
            TextView bodyText = (TextView) view.findViewById(R.id.tutorial_body);
            ViewGroup videoContainer = (ViewGroup) view.findViewById(R.id.video_container);
            videoContainer.addView(mVideo, ViewPager.LayoutParams.MATCH_PARENT,
                    ViewPager.LayoutParams.WRAP_CONTENT);
            // Would like to use Uri.Builder, but it inserts an extra slash which confuses video
            // view.
            Uri video = Uri.parse("android.resource://" + view.getContext().getPackageName() + "/" +
                    getArguments().getInt(KEY_MOVIE));
            mVideo.setVideoURI(video);
            mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                }
            });


            int backgroundColor = getArguments().getInt(KEY_BACKGROUND);
            view.setBackgroundColor(backgroundColor);
            headerText.setText(getArguments().getInt(KEY_HEADER_TEXT));
            bodyText.setText(getArguments().getInt(KEY_BODY_TEXT));
            return view;
        }

        @Override
        public void onDestroyView() {
            mVideo.stopPlayback();
            super.onDestroyView();
        }

        @Override
        public void onResume() {
            super.onResume();
            mVideo.start();
        }

        @Override
        public void onPause() {
            if (mVideo.canPause()) {
                mVideo.pause();
            }
            mVideo.suspend();
            super.onPause();
        }
    }

    public static class EndFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_tutorial_done, container, false);
            return view;
        }
    }
}
