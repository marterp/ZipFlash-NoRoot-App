package com.zipflash.mrp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

public class About extends BaseActivity {

    private Button btnGithub, btnYouTube, btnTikTok, btnFacebook, btnDonate, btnTermsCond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("About");
        }

        // Set the back button (up indicator) color to white
        if (toolbar.getNavigationIcon() != null) {
            toolbar.getNavigationIcon().setTint(Color.WHITE);
        }

        // Description with clickable links
        TextView descView = findViewById(R.id.tvDescription);
        descView.setText(Html.fromHtml(getString(R.string.app_description)));
        descView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());

        // Buttons
        btnGithub = findViewById(R.id.btnGithub);
        btnYouTube = findViewById(R.id.btnYouTube);
        btnTikTok = findViewById(R.id.btnTikTok);
        btnFacebook = findViewById(R.id.btnFacebook);
        btnDonate = findViewById(R.id.btnDonate);
        btnTermsCond = findViewById(R.id.btnTermsCond);

        // Open main website
        Button btnZipflashWebsite = findViewById(R.id.btnZipflashWebsite);
        btnZipflashWebsite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW,
											   Uri.parse("https://zip-flash-modules.vercel.app/"));
					startActivity(intent);
				}
			});

        // Socials
        btnGithub.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl("https://github.com/marterp/ZipFlash-NoRoot");
				}
			});

        btnYouTube.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl("https://youtube.com/@mister_p27official?si=cfCk3QK3_xDPL72N");
				}
			});

        btnTikTok.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl("https://www.tiktok.com/@yourusername");
				}
			});

        btnFacebook.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openUrl("https://www.facebook.com/yourpage");
				}
			});

        // Donate (no ads, just Ko-fi)
        btnDonate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW,
											   Uri.parse("https://ko-fi.com/mister_p0427"));
					startActivity(intent);
				}
			});

        // Terms & Privacy dialog
        btnTermsCond.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					View dialogView = getLayoutInflater().inflate(R.layout.dialog_terms_privacy, null);

					final android.app.AlertDialog dialog =
                        new android.app.AlertDialog.Builder(About.this, R.style.CustomDialogTheme)
						.setView(dialogView)
						.create();

					Button btnClose = dialogView.findViewById(R.id.btnClose);
					btnClose.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						});

					dialog.show();

					if (dialog.getWindow() != null) {
						dialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(
								android.graphics.Color.TRANSPARENT
                            )
						);
					}
				}
			});
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
