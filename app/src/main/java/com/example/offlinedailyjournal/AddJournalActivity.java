package com.example.offlinedailyjournal;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AddJournalActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 1001;
    private static final String COLUMN_JOURNAL_MOOD = "mood";

    private TextView journalPageTitle;
    private EditText journalTitleInput, journalContentInput;
    private TextView dateAddedText, dateModifiedText;
    private Button saveButton, speakButton, recordButton;
    private Spinner moodSpinner;

    private DatabaseHelper dbHelper;
    private long folderId;
    private long journalId = -1;
    private boolean isEditMode = false;

    private TextToSpeech tts;

    // Spinner data
    private final String[] moods = new String[]{
            "— Select mood —",
            "😀 Happy", "🙂 Calm", "😐 Neutral", "🙁 Sad",
            "😡 Angry", "😴 Tired", "✨ Excited"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_journal);

        // Bind views
        journalPageTitle    = findViewById(R.id.journalPageTitle);
        journalTitleInput   = findViewById(R.id.journalTitleInput);
        journalContentInput = findViewById(R.id.journalContentInput);
        dateAddedText       = findViewById(R.id.dateAddedText);
        dateModifiedText    = findViewById(R.id.dateModifiedText);
        saveButton          = findViewById(R.id.saveButton);
        speakButton         = findViewById(R.id.speakButton);
        recordButton        = findViewById(R.id.recordButton);
        moodSpinner         = findViewById(R.id.moodSpinner);

        // Fonts
        Typeface poppinsRegular = ResourcesCompat.getFont(this, R.font.poppins);
        Typeface poppinsLight   = ResourcesCompat.getFont(this, R.font.poppins_light);
        if (poppinsRegular != null && poppinsLight != null) {
            journalTitleInput.setTypeface(poppinsRegular);
            journalContentInput.setTypeface(poppinsLight);
            dateAddedText.setTypeface(poppinsLight);
            dateModifiedText.setTypeface(poppinsLight);
            saveButton.setTypeface(poppinsRegular);
            speakButton.setTypeface(poppinsRegular);
            recordButton.setTypeface(poppinsRegular);
        }

        // Spinner adapter
        ArrayAdapter<String> moodAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, moods
        );
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(moodAdapter);

        dbHelper = new DatabaseHelper(this);
        ensureMoodColumn(); // add the column once if it doesn't exist

        Intent intent = getIntent();
        journalId = intent.getLongExtra("journalId", -1);

        if (journalId != -1) {
            // EDIT mode
            isEditMode = true;
            journalPageTitle.setText("Edit Journal Entry");
            saveButton.setText("Update Entry");
            loadJournalForEdit(journalId);
        } else {
            // NEW mode
            journalPageTitle.setText("New Journal Entry");
            saveButton.setText("Save Entry");
            folderId = intent.getLongExtra("folderId", -1);
            if (folderId == -1) {
                Toast.makeText(this, "Invalid folder.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            String now = new SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(new Date());
            dateAddedText.setText("Date Added: " + now);
            dateModifiedText.setText("Last Modified: " + now);
        }

        // Save
        saveButton.setOnClickListener(v -> {
            if (isEditMode) updateJournal();
            else insertJournal();
        });

        // TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        speakButton.setOnClickListener(v -> {
            String text = journalContentInput.getText().toString();
            if (!TextUtils.isEmpty(text)) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(this, "Nothing to read.", Toast.LENGTH_SHORT).show();
            }
        });

        // STT
        recordButton.setOnClickListener(v -> startSpeechToText());
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your journal content...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on your device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                journalContentInput.append(result.get(0) + " ");
            }
        }
    }

    private void loadJournalForEdit(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(
                DatabaseHelper.TABLE_JOURNALS,
                null,
                DatabaseHelper.COLUMN_JOURNAL_ID + " = ?",
                new String[]{ String.valueOf(id) },
                null, null, null
        );
        if (c != null && c.moveToFirst()) {
            String title   = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_TITLE));
            String content = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_CONTENT));
            String added   = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_DATE_ADDED));
            String modified= c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_DATE_MODIFIED));
            folderId       = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_FOLDER_ID));

            journalTitleInput.setText(title);
            journalContentInput.setText(content);
            dateAddedText.setText("Date Added: " + added);
            dateModifiedText.setText("Last Modified: " + modified);

            // Set spinner selection from stored mood (if column exists)
            int moodIdx = c.getColumnIndex(COLUMN_JOURNAL_MOOD);
            if (moodIdx != -1) {
                String mood = c.getString(moodIdx);
                selectMoodInSpinner(mood);
            }
        }
        if (c != null) c.close();
    }

    private void insertJournal() {
        String title   = journalTitleInput.getText().toString().trim();
        String content = journalContentInput.getText().toString().trim();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Please enter both title and content.", Toast.LENGTH_SHORT).show();
            return;
        }

        long newId = dbHelper.insertJournal(title, content, folderId);
        if (newId != -1) {
            String mood = getSelectedMood();
            if (mood != null) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues v = new ContentValues();
                v.put(COLUMN_JOURNAL_MOOD, mood);
                db.update(DatabaseHelper.TABLE_JOURNALS, v,
                        DatabaseHelper.COLUMN_JOURNAL_ID + "=?",
                        new String[]{ String.valueOf(newId) });
            }
            Toast.makeText(this, "Journal saved!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to save journal.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateJournal() {
        String title   = journalTitleInput.getText().toString().trim();
        String content = journalContentInput.getText().toString().trim();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Please enter both title and content.", Toast.LENGTH_SHORT).show();
            return;
        }
        String now = new SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault())
                .format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_JOURNAL_TITLE, title);
        values.put(DatabaseHelper.COLUMN_JOURNAL_CONTENT, content);
        values.put(DatabaseHelper.COLUMN_JOURNAL_DATE_MODIFIED, now);

        String mood = getSelectedMood();
        if (mood != null) values.put(COLUMN_JOURNAL_MOOD, mood);

        int rows = db.update(
                DatabaseHelper.TABLE_JOURNALS,
                values,
                DatabaseHelper.COLUMN_JOURNAL_ID + " = ?",
                new String[]{ String.valueOf(journalId) }
        );
        if (rows > 0) {
            Toast.makeText(this, "Journal updated!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getSelectedMood() {
        int pos = moodSpinner.getSelectedItemPosition();
        if (pos <= 0) return null; // "— Select mood —"
        return moods[pos];
    }

    private void selectMoodInSpinner(String moodText) {
        if (TextUtils.isEmpty(moodText)) return;
        for (int i = 0; i < moods.length; i++) {
            if (moods[i].equals(moodText)) {
                moodSpinner.setSelection(i);
                return;
            }
        }
        // not found: leave as default
    }

    /** Adds the 'mood' column once if it doesn't exist. Safe to call repeatedly. */
    private void ensureMoodColumn() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.execSQL("ALTER TABLE " + DatabaseHelper.TABLE_JOURNALS +
                    " ADD COLUMN " + COLUMN_JOURNAL_MOOD + " TEXT");
        } catch (Exception ignored) {
            // Column likely exists; ignore.
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
