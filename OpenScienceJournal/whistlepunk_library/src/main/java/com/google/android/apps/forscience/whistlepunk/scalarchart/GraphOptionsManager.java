package com.google.android.apps.forscience.whistlepunk.scalarchart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.google.android.apps.forscience.whistlepunk.ActiveSeekBarListeners;
import com.google.android.apps.forscience.whistlepunk.ActiveSettingsController;
import com.google.android.apps.forscience.whistlepunk.R;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ActiveBundle;
import com.google.android.apps.forscience.whistlepunk.sensorapi.ReadableSensorOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * The OptionsManager for graph settings which should apply to all graphs across the app.
 */
public class GraphOptionsManager {
    private final ScalarDisplayOptions mScalarDisplayOptions;

    private SeekBar mSmoothnessBar;
    private SeekBar mWindowBar;
    private Spinner mBlurSpinner;
    private SeekBar mGaussianSigmaBar;

    public GraphOptionsManager(ScalarDisplayOptions scalarDisplayOptions) {
        mScalarDisplayOptions = scalarDisplayOptions;
    }

    public View buildOptionsView(final ActiveBundle activeBundle, Context context) {
        @SuppressLint("InflateParams") final View inflated =
                LayoutInflater.from(context).inflate(R.layout.graph_options, null);

        final SeekBar smoothnessBar = getSmoothnessSeekBar(inflated);
        smoothnessBar.setProgress(getProgressFromSmoothness(mScalarDisplayOptions.getSmoothness(),
                smoothnessBar.getMax()));
        smoothnessBar.setOnSeekBarChangeListener(
                new ActiveSeekBarListeners.FloatSeekBarListener(activeBundle,
                        ScalarDisplayOptions.PREFS_KEY_SMOOTHNESS) {
                    @Override
                    protected float computeValueFromProgress(int progress, int max) {
                        return getSmoothnessFromProgress(progress, max);
                    }
                });

        final SeekBar windowBar = getWindowSeekBar(inflated);
        windowBar.setMax(ScalarDisplayOptions.WINDOW_MAX - ScalarDisplayOptions.WINDOW_MIN);
        windowBar.setProgress(mScalarDisplayOptions.getWindow() - ScalarDisplayOptions.WINDOW_MIN);
        windowBar.setOnSeekBarChangeListener(
                new ActiveSeekBarListeners.IntSeekBarListener(activeBundle,
                        ScalarDisplayOptions.PREFS_KEY_WINDOW) {
                    @Override
                    protected int computeValueFromProgress(int progress, int max) {
                        return windowBar.getProgress() + ScalarDisplayOptions.WINDOW_MIN;
                    }
                });

        final Spinner blurSpinner = getBlurSpinner(inflated);
        List<String> blurOptions = new ArrayList<>();
        blurOptions.add(ScalarDisplayOptions.BLUR_TYPE_GAUSSIAN,
                context.getResources().getString(R.string.blur_type_gaussian));
        blurOptions.add(ScalarDisplayOptions.BLUR_TYPE_AVERAGE,
                context.getResources().getString(R.string.blur_type_average));
        ArrayAdapter blurAdapter =
                new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                        blurOptions);
        blurAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        blurSpinner.setAdapter(blurAdapter);
        blurSpinner.setSelection(mScalarDisplayOptions.getBlurType());
        blurSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeBundle.changeInt(ScalarDisplayOptions.PREFS_KEY_BLUR_TYPE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final SeekBar sigmaBar = getGaussianSigmaBar(inflated);
        sigmaBar.setProgress(getProgressFromGaussianSigma(mScalarDisplayOptions.getGaussianSigma(),
                sigmaBar.getMax()));
        sigmaBar.setOnSeekBarChangeListener(
                new ActiveSeekBarListeners.FloatSeekBarListener(activeBundle,
                        ScalarDisplayOptions.PREFS_KEY_GAUSSIAN_SIGMA) {
                    @Override
                    protected float computeValueFromProgress(int progress, int max) {
                        return getGaussianSigmaFromProgress(progress, max);
                    }
                });

        return inflated;
    }

    public void loadOptions(ReadableSensorOptions bundle) {
        float smoothness = getSmoothness(bundle);
        int window = getWindow(bundle);
        @ScalarDisplayOptions.BlurType int blurType = getBlurType(bundle);
        float sigma = getGaussianSigma(bundle);
        mScalarDisplayOptions.updateLineSettings(smoothness, window, blurType, sigma);
    }

    private float getSmoothness(ReadableSensorOptions bundle) {
        return bundle.getFloat(ScalarDisplayOptions.PREFS_KEY_SMOOTHNESS,
                ScalarDisplayOptions.DEFAULT_SMOOTHNESS);
    }

    private int getWindow(ReadableSensorOptions bundle) {
        return bundle.getInt(ScalarDisplayOptions.PREFS_KEY_WINDOW,
                ScalarDisplayOptions.DEFAULT_WINDOW);
    }

    private int getBlurType(ReadableSensorOptions bundle) {
        return bundle.getInt(ScalarDisplayOptions.PREFS_KEY_BLUR_TYPE,
                ScalarDisplayOptions.DEFAULT_BLUR_TYPE);
    }

    private float getGaussianSigma(ReadableSensorOptions bundle) {
        return bundle.getFloat(ScalarDisplayOptions.PREFS_KEY_GAUSSIAN_SIGMA,
                ScalarDisplayOptions.DEFAULT_GAUSSIAN_SIGMA);
    }

    private SeekBar getSmoothnessSeekBar(View inflated) {
        if (mSmoothnessBar == null) {
            mSmoothnessBar = (SeekBar) inflated.findViewById(R.id.graph_options_smoothness_edit);
        }
        return mSmoothnessBar;
    }

    private SeekBar getWindowSeekBar(View inflated) {
        if (mWindowBar == null) {
            mWindowBar = (SeekBar) inflated.findViewById(R.id.graph_options_window_edit);
        }
        return mWindowBar;
    }

    private Spinner getBlurSpinner(View inflated) {
        if (mBlurSpinner == null) {
            mBlurSpinner = (Spinner) inflated.findViewById(R.id.graph_options_blur_edit);
        }
        return mBlurSpinner;
    }

    private SeekBar getGaussianSigmaBar(View inflated) {
        if (mGaussianSigmaBar == null) {
            mGaussianSigmaBar =
                    (SeekBar) inflated.findViewById(R.id.graph_options_gaussian_sigma_edit);
        }
        return mGaussianSigmaBar;
    }

    @VisibleForTesting
    public static int getProgressFromSmoothness(float smoothness, int maxProgress) {
        return (int) ((smoothness - ScalarDisplayOptions.SMOOTHNESS_MIN) /
                (ScalarDisplayOptions.SMOOTHNESS_MAX - ScalarDisplayOptions.SMOOTHNESS_MIN) * maxProgress);
    }

    @VisibleForTesting
    public static float getSmoothnessFromProgress(int progress, int maxProgress) {
        // The progress bar starts at 0, so we don't need to worry about a minProgress.
        return (float) progress / (float) maxProgress *
                (ScalarDisplayOptions.SMOOTHNESS_MAX - ScalarDisplayOptions.SMOOTHNESS_MIN) +
                ScalarDisplayOptions.SMOOTHNESS_MIN;
    }

    @VisibleForTesting
    public static int getProgressFromGaussianSigma(float sigma, int maxProgress) {
        return (int) ((sigma - ScalarDisplayOptions.GAUSSIAN_SIGMA_MIN) /
                (ScalarDisplayOptions.GAUSSIAN_SIGMA_MAX - ScalarDisplayOptions.GAUSSIAN_SIGMA_MIN) *
                maxProgress);
    }

    @VisibleForTesting
    public static float getGaussianSigmaFromProgress(int progress, int maxProgress) {
        // The progress bar starts at 0, so we don't need to worry about a minProgress.
        return (float) progress / (float) maxProgress *
                (ScalarDisplayOptions.GAUSSIAN_SIGMA_MAX - ScalarDisplayOptions.GAUSSIAN_SIGMA_MIN) +
                ScalarDisplayOptions.GAUSSIAN_SIGMA_MIN;
    }

    public ActiveSettingsController.OptionsCallbacks makeCallbacks(final Context context) {
        return new ActiveSettingsController.OptionsCallbacks() {
            @Override
            public View buildOptionsView(ActiveBundle activeBundle) {
                loadOptions(activeBundle.getReadOnly());
                return GraphOptionsManager.this.buildOptionsView(activeBundle, context);
            }

            @Override
            public void previewOptions(ReadableSensorOptions newOptions) {
                loadOptions(newOptions);
            }

            @Override
            public void commitOptions(ReadableSensorOptions newOptions) {

            }
        };
    }
}
