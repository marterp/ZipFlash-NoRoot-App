package com.zipflash.mrp.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.graphics.Color;
import android.view.ViewGroup;
import android.util.TypedValue;

import com.zipflash.mrp.helper.CheckPermHelper;

public class LimitedModeBanner {

    private Activity activity;
    private TextView bannerView;

    public LimitedModeBanner(Activity act) {
        this.activity = act;
        initBanner();
    }

    private void initBanner() {
        bannerView = new TextView(activity);
        bannerView.setText("Limited mode — Shizuku not granted. Some features disabled.");
        bannerView.setTextColor(Color.WHITE);
        bannerView.setBackgroundColor(Color.parseColor("#D32F2F")); // red
        int pad = (int) (8 * activity.getResources().getDisplayMetrics().density);
        bannerView.setPadding(pad, pad, pad, pad);
        bannerView.setGravity(Gravity.CENTER);
        bannerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        bannerView.setVisibility(View.GONE);

        FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			Gravity.TOP);
        decor.addView(bannerView, lp);
    }

    public void updateVisibility() {
        boolean limited = !CheckPermHelper.hasShizukuPermission()
			|| CheckPermHelper.isSkipShizukuPersisted(activity);
        bannerView.setVisibility(limited ? View.VISIBLE : View.GONE);
    }

    public void show() { bannerView.setVisibility(View.VISIBLE); }
    public void hide() { bannerView.setVisibility(View.GONE); }
}
