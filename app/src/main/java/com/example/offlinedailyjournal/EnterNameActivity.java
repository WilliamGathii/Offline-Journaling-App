package com.example.offlinedailyjournal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;

public class EnterNameActivity extends AppCompatActivity {

    private EditText nameInput;
    private Button welcomeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_name);

        // Background changes based on orientation
        ConstraintLayout rootLayout = findViewById(R.id.rootLayout);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rootLayout.setBackgroundResource(R.drawable.bg_land);
        } else {
            rootLayout.setBackgroundResource(R.drawable.bg_port);
        }

        // Adjust guideline
        Guideline topGuide = findViewById(R.id.topGuide);
        ConstraintLayout.LayoutParams guideParams = (ConstraintLayout.LayoutParams) topGuide.getLayoutParams();
        guideParams.guidePercent = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                ? 0.05f : 0.30f;
        topGuide.setLayoutParams(guideParams);

        nameInput = findViewById(R.id.nameInput);
        welcomeButton = findViewById(R.id.btnWelcome);

        welcomeButton.setOnClickListener(view -> {
            String enteredName = nameInput.getText().toString().trim();
            if (enteredName.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putString("userName", enteredName).apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
