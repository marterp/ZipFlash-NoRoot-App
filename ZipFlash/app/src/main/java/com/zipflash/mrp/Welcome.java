package com.zipflash.mrp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Welcome extends BaseActivity {

    private TextView tvTitle, tvMessage;
    private Button btnContinue;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        // Check if welcome is already done
        boolean welcomeDone = prefs.getBoolean("welcome_done", false);
        if (welcomeDone) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.welcome_page);

        tvTitle = findViewById(R.id.tvTitle);
        tvMessage = findViewById(R.id.tvMessage);
        btnContinue = findViewById(R.id.btnContinue);

        tvMessage.setVisibility(View.INVISIBLE);
        btnContinue.setVisibility(View.GONE);

        String titleText = "Welcome to ZipFlash!";
        final String messageText = "Thanks for installing ZipFlash!\n\nReady to unleash your phone’s full potential?";

        // Start typing title first
        typeText(tvTitle, titleText, 100, new Runnable() {
                @Override
                public void run() {
                    // Reveal message after title is done
                    tvMessage.setVisibility(View.VISIBLE);
                    typeText(tvMessage, messageText, 50, new Runnable() {
                            @Override
                            public void run() {
                                fadeInButton(btnContinue);
                            }
                        });
                }
            });

        btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Welcome.this, LaunchPerm.class));
                    finish();
                }
            });
    }

    private void typeText(final TextView textView, final String text, final long delay, final Runnable onComplete) {
        textView.setText("");
        final Handler handler = new Handler(Looper.getMainLooper());
        final int[] index = {0};

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index[0] < text.length()) {
                    textView.append(String.valueOf(text.charAt(index[0])));
                    index[0]++;
                    handler.postDelayed(this, delay);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        };

        handler.post(runnable);
    }

    private void fadeInButton(final Button button) {
        button.setVisibility(View.VISIBLE);
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(500);
        fadeIn.setFillAfter(true);
        button.startAnimation(fadeIn);
    }

    @Override
    public void onBackPressed() {
        // Prevent back navigation from welcome screen
    }
}
