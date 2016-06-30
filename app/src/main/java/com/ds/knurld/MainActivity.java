package com.ds.knurld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ds.knurld.service.KnurldService;

import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity {
    public KnurldService knurldService;
    private Thread knurldServiceThread;

    private static final String KNURLD_INSTRUCTIONS = "KNURLD_INSTRUCTIONS";

    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_loading);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressSpinner);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.rgb(226, 132, 59), PorterDuff.Mode.MULTIPLY);

        context = this;

        knurldServiceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                knurldService = new KnurldService();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSetup();
                    }
                });
            }
        });
        knurldServiceThread.start();
    }

    public void showSetup() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        setContentView(R.layout.knurld_setup);
        try {
            knurldServiceThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public PopupWindow showLoadingPopup(View view) {
        View spinnerView = LayoutInflater.from((Activity) context).inflate(R.layout.loading_popup, null);
        ProgressBar progressBar = (ProgressBar) spinnerView.findViewById(R.id.speakProgress);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        PopupWindow popupWindow = new PopupWindow(spinnerView, 500, 500);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        return popupWindow;
    }

    public void showInstructions(View view) {
        Activity parent = (Activity) context;
        View spinnerView = LayoutInflater.from(parent).inflate(R.layout.instructions_popup, null);


        TextView textView = (TextView) spinnerView.findViewById(R.id.phraseText);
        textView.setText("Press record to begin recording enrollment");

        PopupWindow popupWindow = new PopupWindow(spinnerView, 500, 500);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);


        final PopupWindow finalPopupWindow = popupWindow;
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        finalPopupWindow.dismiss();
                    }
                }, 3000);

    }

    public void showMessage(View view, String message) {
        Activity parent = (Activity) context;
        View spinnerView = LayoutInflater.from(parent).inflate(R.layout.instructions_popup, null);


        TextView textView = (TextView) spinnerView.findViewById(R.id.phraseText);
        textView.setText(message);

        PopupWindow popupWindow = new PopupWindow(spinnerView, 500, 500);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);


        final PopupWindow finalPopupWindow = popupWindow;
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        finalPopupWindow.dismiss();
                    }
                }, 3000);

    }


    public void recordEnrollment(View view) {
        Intent intent = new Intent(this, RecordWAVActivity.class);
        JSONArray vocabArray = knurldService.getAppModel().getVocabulary();
        String vocab = "";
        for (int i = 0; i < vocabArray.length(); i++) {
            try {
                vocab += vocabArray.getString(i) + " ";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        intent.putExtra(KNURLD_INSTRUCTIONS, vocab);

        startActivity(intent);
    }

    public void setKnurldEnrollment(View view) {
        Activity parent = (Activity) context;
        final View layoutView = LayoutInflater.from(parent).inflate(R.layout.knurld_setup, null);
        final PopupWindow loadingWindow = showLoadingPopup(layoutView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                knurldService.startEnrollment();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingWindow.dismiss();
                    }
                });

            }
        }).start();
        showInstructions(layoutView);
    }

    public void updateKnurldEnrollment(View view) {
        Activity parent = (Activity) context;
        final View layoutView = LayoutInflater.from(parent).inflate(R.layout.knurld_setup, null);
        final PopupWindow loadingWindow = showLoadingPopup(layoutView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean isEnrolled = knurldService.enroll();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isEnrolled) {
                            loadingWindow.dismiss();
                            showMessage(layoutView, "Enrollment completed!");
                        }
                        else {
                            loadingWindow.dismiss();
                            showMessage(layoutView, "Enrollment failed, please record enrollment again");
                        }
                    }
                });
            }
        }).start();
    }
}
