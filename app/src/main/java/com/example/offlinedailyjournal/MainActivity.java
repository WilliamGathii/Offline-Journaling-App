package com.example.offlinedailyjournal;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    private TextView greetingText, btnLogout;
    private TextView tabFolders, tabJournals;
    private NestedScrollView scrollViewFolders, scrollViewJournals;
    private LinearLayout journalListContainer;
    private FlexboxLayout folderGrid;
    private FloatingActionButton btnNewFolder;
    private TextView emptyFolderText, emptyJournalMainText;
    private LinearLayout filterContainer;
    private Spinner spinnerYearMain, spinnerMonthMain;

    // DB
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    // For folders
    private final String[] categories = { "Work", "Personal", "Creative", "Finance", "Fitness", "School", "Travel", "Others" };
    private final String[] colorNames = {
            "Rose", "Peach", "Lavender", "Sky", "Mint", "Lime", "Coral", "Beige",
            "Teal", "Indigo", "Sunset", "Sage", "Clay", "Blush"
    };
    private final String[] colorHex = {
            "#F28BA8", "#FFD1A4", "#D3BCE3", "#B2D7F3", "#BFF0D6", "#DFF28A", "#FFAB9B", "#F3E8D9",
            "#80CBC4", "#7986CB", "#FFB74D", "#C5E1A5", "#A1887F", "#F8BBD0"
    };


    // Date formats for journals
    private static final SimpleDateFormat[] PARSE_FORMATS = {
            new SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    };
    private static final SimpleDateFormat HEADER_FMT  = new SimpleDateFormat("MMMM d", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_FMT = new SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View binding
        greetingText         = findViewById(R.id.greetingText);
        btnLogout            = findViewById(R.id.btnLogout);
        tabFolders           = findViewById(R.id.tabFolders);
        tabJournals          = findViewById(R.id.tabJournals);
        scrollViewFolders    = findViewById(R.id.scrollViewFolders);
        scrollViewJournals   = findViewById(R.id.scrollViewJournals);
        emptyFolderText      = findViewById(R.id.emptyFolderText);
        emptyJournalMainText = findViewById(R.id.emptyJournalMainText);
        folderGrid           = findViewById(R.id.folderGrid);
        journalListContainer = findViewById(R.id.journalListContainer);
        btnNewFolder         = findViewById(R.id.btnNewFolder);
        filterContainer      = findViewById(R.id.filterContainer);
        spinnerYearMain      = findViewById(R.id.spinnerYearMain);
        spinnerMonthMain     = findViewById(R.id.spinnerMonthMain);

        // DB
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();

        // Greeting
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String name = prefs.getString("userName", "User");
        SpannableString styled = new SpannableString("Welcome back,\n" + name + "!");
        Typeface light = ResourcesCompat.getFont(this, R.font.poppins_light);
        Typeface med   = ResourcesCompat.getFont(this, R.font.poppins_medium);
        styled.setSpan(new CustomTypefaceSpan(light), 0, 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new CustomTypefaceSpan(med), 15, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        greetingText.setText(styled);

        // Tabs
        tabFolders.setOnClickListener(v -> showFoldersTab());
        tabJournals.setOnClickListener(v -> showJournalsTab());

        // Logout
        btnLogout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes", (d, w) -> {
                            prefs.edit().clear().apply();
                            startActivity(new Intent(this, WelcomeActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
        );

        // Add Folder
        btnNewFolder.setOnClickListener(v -> showAddFolderDialog());

        // Default: Folders tab
        showFoldersTab();
    }

    // ========== FOLDERS TAB ==========
    private void showFoldersTab() {
        // Tab styling
        tabFolders .setBackgroundResource(R.drawable.tab_active_bg);
        tabFolders .setTextColor(Color.WHITE);
        tabJournals.setBackgroundResource(R.drawable.tab_inactive_bg);
        tabJournals.setTextColor(Color.parseColor("#6D4C41"));
        scrollViewFolders .setVisibility(View.VISIBLE);
        scrollViewJournals.setVisibility(View.GONE);
        filterContainer    .setVisibility(View.GONE);
        btnNewFolder       .show();
        loadFoldersFromDatabase();
    }

    private void loadFoldersFromDatabase() {
        folderGrid.removeAllViews();

        Cursor c = db.query(
                DatabaseHelper.TABLE_FOLDERS,
                null, null, null, null, null, null
        );

        if (c.moveToFirst()) {
            emptyFolderText.setVisibility(View.GONE);
            do {
                int id = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FOLDER_ID));
                String name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FOLDER_NAME));
                String color = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FOLDER_ICON));

                View card = LayoutInflater.from(this)
                        .inflate(R.layout.item_folder, folderGrid, false);

                // Set background color
                ((LinearLayout) card).setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor(color))
                );

                // Set icon
                ImageView icon = card.findViewById(R.id.folderIcon);
                int res = getResources().getIdentifier(
                        name.toLowerCase(Locale.ROOT),
                        "drawable",
                        getPackageName()
                );
                icon.setImageResource(res != 0 ? res : R.drawable.others);

                // Set title
                TextView title = card.findViewById(R.id.folderName);
                title.setText(name);

                // 📂 Click: open JournalListActivity
                card.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, JournalListActivity.class);
                    intent.putExtra("folderId", (long) id); // cast to long
                    intent.putExtra("folderName", name);
                    intent.putExtra("folderColor", color);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                });

                // 🗑️ Long press to delete folder
                card.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Delete Folder")
                            .setMessage("Are you sure you want to delete this folder and all its journals?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                // Delete journals inside this folder
                                db.delete(
                                        DatabaseHelper.TABLE_JOURNALS,
                                        DatabaseHelper.COLUMN_JOURNAL_FOLDER_ID + "=?",
                                        new String[]{String.valueOf(id)}
                                );
                                // Delete the folder itself
                                db.delete(
                                        DatabaseHelper.TABLE_FOLDERS,
                                        DatabaseHelper.COLUMN_FOLDER_ID + "=?",
                                        new String[]{String.valueOf(id)}
                                );
                                loadFoldersFromDatabase();
                                Toast.makeText(MainActivity.this, "Folder deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    return true;
                });

                folderGrid.addView(card);
            } while (c.moveToNext());
        } else {
            emptyFolderText.setVisibility(View.VISIBLE);
        }

        c.close();
    }


    // ========== JOURNALS TAB ==========
    private void showJournalsTab() {
        tabFolders .setBackgroundResource(R.drawable.tab_inactive_bg);
        tabFolders .setTextColor(Color.parseColor("#6D4C41"));
        tabJournals.setBackgroundResource(R.drawable.tab_active_bg);
        tabJournals.setTextColor(Color.WHITE);
        scrollViewFolders .setVisibility(View.GONE);
        scrollViewJournals.setVisibility(View.VISIBLE);
        filterContainer    .setVisibility(View.VISIBLE);
        btnNewFolder       .hide();
        setupJournalFilters();
    }

    private void setupJournalFilters() {
        // -- YEAR Spinner
        List<String> years = new ArrayList<>();
        years.add("All");
        Cursor cy = db.rawQuery(
                "SELECT DISTINCT strftime('%Y', date_modified) FROM " +
                        DatabaseHelper.TABLE_JOURNALS + " ORDER BY 1 DESC",
                null
        );
        while (cy != null && cy.moveToNext()) {
            years.add(cy.getString(0));
        }
        if (cy != null) cy.close();
        spinnerYearMain.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, years
        ));

        // -- MONTH Spinner
        String[] months = {
                "All","January","February","March","April","May","June",
                "July","August","September","October","November","December"
        };
        spinnerMonthMain.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, months
        ));

        // Listeners
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                loadAllJournalsWithHeaders();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerYearMain.setOnItemSelectedListener(filterListener);
        spinnerMonthMain.setOnItemSelectedListener(filterListener);

        loadAllJournalsWithHeaders();
    }

    private void loadAllJournalsWithHeaders() {
        journalListContainer.removeAllViews();
        boolean foundAny = false;

        String yearFilter  = spinnerYearMain.getSelectedItem().toString();
        String monthFilter = spinnerMonthMain.getSelectedItem().toString();

        Cursor c = db.query(
                DatabaseHelper.TABLE_JOURNALS,
                null, null, null, null, null,
                DatabaseHelper.COLUMN_JOURNAL_DATE_MODIFIED + " DESC"
        );

        String lastHeader = "";
        if (c.moveToFirst()) {
            emptyJournalMainText.setVisibility(View.GONE);
            do {
                long   jid   = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_ID));
                String title = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_TITLE));
                String ts    = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_DATE_MODIFIED));
                long   fId   = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_JOURNAL_FOLDER_ID));

                // Parse date
                Date parsedDate = null;
                for (SimpleDateFormat pf : PARSE_FORMATS) {
                    try { parsedDate = pf.parse(ts); break; }
                    catch (ParseException ignored) {}
                }
                if (parsedDate == null) continue;

                // Extract year + month name
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsedDate);
                String entryYear  = String.valueOf(cal.get(Calendar.YEAR));
                String entryMonth = new DateFormatSymbols().getMonths()[cal.get(Calendar.MONTH)];

                // Apply filters
                if ((!yearFilter.equals("All")  && !yearFilter.equals(entryYear)) ||
                        (!monthFilter.equals("All") && !monthFilter.equals(entryMonth))) {
                    continue;
                }
                foundAny = true;

                // Section header
                String headerDate = HEADER_FMT.format(parsedDate);
                if (!headerDate.equals(lastHeader)) {
                    lastHeader = headerDate;
                    TextView h = new TextView(this);
                    h.setText(headerDate);
                    h.setTextSize(18);
                    h.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_medium));
                    h.setTextColor(Color.parseColor("#5C2E2E"));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    lp.setMargins(0, 24, 0, 8);
                    h.setLayoutParams(lp);
                    journalListContainer.addView(h);
                }

                // Get folder color AND name for icon
                Cursor f = db.query(
                        DatabaseHelper.TABLE_FOLDERS,
                        null,
                        DatabaseHelper.COLUMN_FOLDER_ID + "=?",
                        new String[]{String.valueOf(fId)},
                        null, null, null
                );
                String fcol = "#CCCCCC";
                String fname = "others";
                if (f.moveToFirst()) {
                    fcol  = f.getString(f.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FOLDER_ICON));
                    fname = f.getString(f.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FOLDER_NAME));
                }
                f.close();

                // Inflate journal card
                View item = LayoutInflater.from(this)
                        .inflate(R.layout.item_journal_main, journalListContainer, false);
                CardView  card     = item.findViewById(R.id.journalCard);
                ImageView folderIv = item.findViewById(R.id.journalFolderIcon);
                TextView  tvTitle  = item.findViewById(R.id.journalTitle);
                TextView  tvTs     = item.findViewById(R.id.journalDate);

                card.setCardBackgroundColor(Color.parseColor(fcol));

                // Set the correct icon for this folder
                int iconRes = getResources().getIdentifier(
                        fname.toLowerCase(Locale.ROOT), "drawable", getPackageName());
                folderIv.setImageResource(iconRes != 0 ? iconRes : R.drawable.others);

                tvTitle.setText(title);
                tvTs.setText(DISPLAY_FMT.format(parsedDate));

                card.setOnClickListener(v -> {
                    Intent i = new Intent(this, AddJournalActivity.class);
                    i.putExtra("journalId", jid);
                    startActivity(i);
                });

                journalListContainer.addView(item);
            } while (c.moveToNext());
        }
        c.close();

        if (!foundAny) {
            emptyJournalMainText.setText("No journal entries in " + monthFilter);
            emptyJournalMainText.setVisibility(View.VISIBLE);
        } else {
            emptyJournalMainText.setVisibility(View.GONE);
        }
    }


    // ========== FOLDER CREATION ==========
    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_folder, null);
        builder.setView(dialogView);

        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        Spinner spinnerColor    = dialogView.findViewById(R.id.spinnerColor);
        ImageView iconPreview   = dialogView.findViewById(R.id.iconPreview);
        FrameLayout iconBg      = dialogView.findViewById(R.id.iconBackgroundWrapper);
        TextView btnDone        = dialogView.findViewById(R.id.btnDone);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_dropdown_item, categories
        );
        categoryAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
                this, R.layout.spinner_dropdown_item, colorNames
        );
        colorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerColor.setAdapter(colorAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String nm = categories[pos].toLowerCase(Locale.ROOT);
                int resId = getResources().getIdentifier(nm, "drawable", getPackageName());
                iconPreview.setImageResource(resId != 0 ? resId : R.drawable.others);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                iconBg.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor(colorHex[pos]))
                );
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog dialog = builder.create();
        btnDone.setOnClickListener(v -> {
            String nm  = spinnerCategory.getSelectedItem().toString();
            int    idx = spinnerColor.getSelectedItemPosition();
            String clr = colorHex[idx];

            // 🔒 Check if folder with same category already exists
            Cursor check = db.query(
                    DatabaseHelper.TABLE_FOLDERS,
                    null,
                    DatabaseHelper.COLUMN_FOLDER_NAME + "=?",
                    new String[]{nm},
                    null, null, null
            );
            if (check.moveToFirst()) {
                Toast.makeText(this, "Folder with this category already exists.", Toast.LENGTH_SHORT).show();
                check.close();
                return;
            }
            check.close();

            long res = dbHelper.insertFolder(nm, clr);
            if (res > 0) {
                dialog.dismiss();
                loadFoldersFromDatabase();
                Toast.makeText(this, "Folder created: " + nm, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create folder.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    // Refresh on return
    @Override
    protected void onResume() {
        super.onResume();
        if (scrollViewFolders.getVisibility() == View.VISIBLE) {
            loadFoldersFromDatabase();
        } else {
            loadAllJournalsWithHeaders();
        }
    }
}
