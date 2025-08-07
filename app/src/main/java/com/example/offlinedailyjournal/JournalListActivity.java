package com.example.offlinedailyjournal;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalListActivity extends AppCompatActivity {

    private TextView folderTitle, btnBack, emptyJournalText;
    private Spinner spinnerYear, spinnerMonth;
    private LinearLayout journalListContainer;
    private DatabaseHelper dbHelper;
    private String folderColor;
    private long folderId;

    // Parsers for both human-readable and ISO timestamps
    private static final SimpleDateFormat[] PARSE_FORMATS = {
            new SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()),    // e.g. "July 29, 2025 22:40"
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())     // e.g. "2025-07-29 22:40:44"
    };
    private static final SimpleDateFormat HEADER_FMT  =
            new SimpleDateFormat("MMMM d", Locale.getDefault());                // "July 29"
    private static final SimpleDateFormat DISPLAY_FMT =
            new SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.getDefault());    // "July 29, 2025 22:40"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);

        // Bind views
        folderTitle          = findViewById(R.id.folderTitle);
        spinnerYear          = findViewById(R.id.spinnerYear);
        spinnerMonth         = findViewById(R.id.spinnerMonth);
        emptyJournalText     = findViewById(R.id.emptyJournalText);
        journalListContainer = findViewById(R.id.journalListContainer);
        btnBack              = findViewById(R.id.btnBack);

        findViewById(R.id.btnAddJournal).setOnClickListener(v -> {
            Intent i = new Intent(this, AddJournalActivity.class);
            i.putExtra("folderId", folderId);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Receive folder info
        Intent intent = getIntent();
        folderId    = intent.getLongExtra("folderId", -1);
        folderColor = intent.getStringExtra("folderColor");
        String name = intent.getStringExtra("folderName");
        folderTitle.setText(name + " Journal");

        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });


        dbHelper = new DatabaseHelper(this);
        setupFilters();
    }

    private void setupFilters() {
        // Year spinner
        List<String> years = new ArrayList<>();
        years.add("All");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cy = db.rawQuery(
                "SELECT DISTINCT strftime('%Y', date_modified) FROM " +
                        DatabaseHelper.TABLE_JOURNALS +
                        " WHERE folder_id = ? ORDER BY 1 DESC",
                new String[]{ String.valueOf(folderId) }
        );
        while (cy != null && cy.moveToNext()) {
            years.add(cy.getString(0));
        }
        if (cy != null) cy.close();
        spinnerYear.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                years
        ));

        // Month spinner (words)
        String[] months = {
                "All",
                "January","February","March",
                "April","May","June",
                "July","August","September",
                "October","November","December"
        };
        spinnerMonth.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                months
        ));

        AdapterView.OnItemSelectedListener filterListener =
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        loadJournalsFromDatabase();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                };
        spinnerYear.setOnItemSelectedListener(filterListener);
        spinnerMonth.setOnItemSelectedListener(filterListener);

        // Initial load
        loadJournalsFromDatabase();
    }

    private void loadJournalsFromDatabase() {
        journalListContainer.removeAllViews();
        boolean foundAny = false;

        String yearFilter  = spinnerYear .getSelectedItem().toString();
        String monthFilter = spinnerMonth.getSelectedItem().toString();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id,title,content,date_modified FROM " + DatabaseHelper.TABLE_JOURNALS +
                        " WHERE " + DatabaseHelper.COLUMN_JOURNAL_FOLDER_ID + " = ?" +
                        " ORDER BY date_modified DESC",
                new String[]{ String.valueOf(folderId) }
        );

        if (c != null && c.moveToFirst()) {
            String lastHeader = "";
            do {
                long   journalId = c.getLong(0);
                String title     = c.getString(1);
                String rawDate   = c.getString(3);

                // 1) Parse into Date
                Date dt = null;
                for (SimpleDateFormat pf : PARSE_FORMATS) {
                    try { dt = pf.parse(rawDate); break; }
                    catch (ParseException ignored) {}
                }
                if (dt == null) continue;

                // 2) Extract year + month name
                Calendar cal = Calendar.getInstance();
                cal.setTime(dt);
                String entryYear  = String.valueOf(cal.get(Calendar.YEAR));
                String entryMonth = new DateFormatSymbols()
                        .getMonths()[cal.get(Calendar.MONTH)];

                // 3) Apply filters
                if ((!yearFilter.equals("All")  && !yearFilter.equals(entryYear)) ||
                        (!monthFilter.equals("All") && !monthFilter.equals(entryMonth))) {
                    continue;
                }
                foundAny = true;

                // 4) Section header
                String headerDate = HEADER_FMT.format(dt); // "July 29"
                if (!headerDate.equals(lastHeader)) {
                    lastHeader = headerDate;
                    TextView h = new TextView(this);
                    h.setText(headerDate);
                    h.setTextSize(18);
                    h.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_medium));
                    h.setTextColor(Color.parseColor(folderColor));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    lp.setMargins(0, 24, 0, 8);
                    h.setLayoutParams(lp);
                    journalListContainer.addView(h);
                }

                // 5) Journal card
                View item = LayoutInflater.from(this)
                        .inflate(R.layout.item_journal, journalListContainer, false);
                CardView card        = item.findViewById(R.id.journalCard);
                TextView tvTitle     = item.findViewById(R.id.journalTitle);
                TextView tvTimestamp = item.findViewById(R.id.journalTimestamp);

                card.setCardBackgroundColor(Color.parseColor(folderColor));
                tvTitle.setText(title);
                tvTimestamp.setText(DISPLAY_FMT.format(dt));

                card.setOnClickListener(v -> {
                    Intent edit = new Intent(this, AddJournalActivity.class);
                    edit.putExtra("journalId", journalId);
                    startActivity(edit);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });
                card.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Entry")
                            .setMessage("Are you sure you want to delete this journal?")
                            .setPositiveButton("Delete", (d,w) -> {
                                db.delete(
                                        DatabaseHelper.TABLE_JOURNALS,
                                        DatabaseHelper.COLUMN_JOURNAL_ID + "=?",
                                        new String[]{ String.valueOf(journalId) }
                                );
                                loadJournalsFromDatabase();
                                Toast.makeText(this,"Entry deleted",Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                });

                journalListContainer.addView(item);

            } while (c.moveToNext());
        }
        if (c != null) c.close();

        // Empty message if none
        if (!foundAny) {
            emptyJournalText.setText(
                    "No journal entries in " + spinnerMonth.getSelectedItem());
            emptyJournalText.setVisibility(View.VISIBLE);
        } else {
            emptyJournalText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJournalsFromDatabase(); // Refresh the journal list when coming back
    }

}
