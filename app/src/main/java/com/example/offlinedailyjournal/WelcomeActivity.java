package com.example.offlinedailyjournal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if userName is already saved
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedName = prefs.getString("userName", null);

        if (savedName != null && !savedName.isEmpty()) {
            // Skip welcome and go straight to home
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Otherwise, show welcome screen
        setContentView(R.layout.activity_welcome);

        Guideline topGuideline = findViewById(R.id.topGuideline);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) topGuideline.getLayoutParams();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.guidePercent = 0.1f;
        } else {
            params.guidePercent = 0.3f;
        }

        topGuideline.setLayoutParams(params);

        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rootLayout.setBackgroundResource(R.drawable.bg_land);
        } else {
            rootLayout.setBackgroundResource(R.drawable.bg_port);
        }

        Button btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, EnterNameActivity.class));
            }
        });
    }
}
