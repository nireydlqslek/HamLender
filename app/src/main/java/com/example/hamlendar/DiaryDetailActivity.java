package com.example.hamlendar;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DiaryDetailActivity extends AppCompatActivity {

    private static final String PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";
    private static final String KEY_SUMMARY_PREFIX = "summary_";
    private static final String KEY_TIMETABLE_PREFIX = "timetable_";

    private TextView txtDate;
    private EditText editContent;
    private String originalContent = "";
    private boolean isSaved;

    private TextView tvDiarySummary;
    private ProgressBar progressBar;

    private TimeTable miniTimeTable;
    private View openTimeTableEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        txtDate = findViewById(R.id.txtDate);
        editContent = findViewById(R.id.editContent);
        tvDiarySummary = findViewById(R.id.tvDiarySummary);
        progressBar = findViewById(R.id.progressBar);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView menuIcon = findViewById(R.id.menuIcon);

        ImageButton btnSave = findViewById(R.id.btnSave);
        ImageButton btnDeleteDiary = findViewById(R.id.btnDeleteDiary);

        String date = getIntent().getStringExtra("date");
        String content = getIntent().getStringExtra("content");

        originalContent = content == null ? "" : content;

        SharedPreferences settings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isAiEnabled = settings.getBoolean("isAiEnabled", true);

        if (txtDate != null) {
            txtDate.setText((date == null || date.isEmpty()) ? getCurrentDate() : date);
        }
        if (editContent != null) {
            editContent.setText(originalContent);
        }

        if (tvDiarySummary != null && txtDate != null) {
            String savedSummary = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .getString(KEY_SUMMARY_PREFIX + txtDate.getText().toString(), "");

            if (isAiEnabled) {
                if (!savedSummary.isEmpty()) {
                    tvDiarySummary.setText(savedSummary);
                } else {
                    tvDiarySummary.setText("작성된 요약이 없습니다.");
                }
            } else {
                tvDiarySummary.setText("AI 기능이 OFF 상태입니다.");
            }
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBack());
        }

        if (txtDate != null) {
            txtDate.setOnClickListener(v ->
                    startActivity(new Intent(DiaryDetailActivity.this, DiaryListActivity.class)));
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveOnly());
        }

        if (btnDeleteDiary != null) {
            btnDeleteDiary.setOnClickListener(v -> confirmDeleteDiary());
        }

        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> showDiaryMenu());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });

        miniTimeTable = findViewById(R.id.miniTimeTable);
        openTimeTableEditor = findViewById(R.id.openTimeTableEditor);

        if (miniTimeTable != null) {
            miniTimeTable.setEditable(false);
            miniTimeTable.setLabelEditMode(false);

            miniTimeTable.setClickable(true);
            miniTimeTable.setFocusable(true);

            miniTimeTable.setOnTouchListener((v, event) -> true);

            loadTimeTable();
        }

        if (openTimeTableEditor != null) {
            openTimeTableEditor.setOnClickListener(v -> showTimeTableDialog());
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
        return sdf.format(new Date());
    }

    private void handleBack() {
        if (isSaved || !hasChanged()) {
            finish();
        } else {
            Toast.makeText(this, "저장 버튼을 눌러 주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasChanged() {
        if (editContent == null) return false;
        String currentContent = editContent.getText().toString().trim();
        return !currentContent.equals(originalContent);
    }

    private void showDiaryMenu() {
        String[] menuItems = {"저장", "삭제", "취소"};

        new AlertDialog.Builder(this)
                .setItems(menuItems, (dialog, which) -> {
                    if (which == 0) {
                        saveOnly();
                    } else if (which == 1) {
                        confirmDeleteDiary();
                    } else {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void saveOnly() {
        if (saveDiary()) {
            isSaved = true;
            if (editContent != null) {
                originalContent = editContent.getText().toString().trim();
            }
        }
    }

    private boolean saveDiary() {
        if (txtDate == null || editContent == null) return false;

        String date = txtDate.getText().toString();
        String content = editContent.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "일기 내용을 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DIARY_LIST, "[]");

        try {
            JSONArray diaries = new JSONArray(json);
            JSONObject newDiary = new JSONObject();

            newDiary.put("date", date);
            newDiary.put("content", content);

            for (int i = 0; i < diaries.length(); i++) {
                JSONObject diary = diaries.getJSONObject(i);

                if (date.equals(diary.optString("date"))) {
                    diaries.put(i, newDiary);
                    prefs.edit().putString(KEY_DIARY_LIST, diaries.toString()).apply();

                    if (isAiEnabled()) {
                        generateDiarySummary(content);
                    } else {
                        if (tvDiarySummary != null) tvDiarySummary.setText("AI 기능이 OFF 상태입니다.");
                        saveSummary(date, "AI 기능이 OFF 상태입니다.");
                    }

                    Toast.makeText(this, "일기가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            diaries.put(newDiary);
            prefs.edit().putString(KEY_DIARY_LIST, diaries.toString()).apply();

            generateDiarySummary(content);

            Toast.makeText(this, "일기가 저장되었습니다.", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "일기를 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void confirmDeleteDiary() {
        new AlertDialog.Builder(this)
                .setTitle("일기 삭제")
                .setMessage("이 일기를 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> deleteDiary())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteDiary() {
        if (txtDate == null) return;
        String date = txtDate.getText().toString();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DIARY_LIST, "[]");

        try {
            JSONArray diaries = new JSONArray(json);
            JSONArray newDiaries = new JSONArray();

            for (int i = 0; i < diaries.length(); i++) {
                JSONObject diary = diaries.getJSONObject(i);
                if (!date.equals(diary.optString("date"))) {
                    newDiaries.put(diary);
                }
            }

            prefs.edit()
                    .putString(KEY_DIARY_LIST, newDiaries.toString())
                    .remove(KEY_SUMMARY_PREFIX + date)
                    .remove(KEY_TIMETABLE_PREFIX + date)
                    .apply();

            isSaved = true;
            Toast.makeText(this, "일기가 삭제되었습니다", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "일기를 삭제할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimeTableDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_timetable);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.45f;
            window.setAttributes(params);
        }

        TimeTable bigTimeTable = dialog.findViewById(R.id.bigTimeTable);
        ImageView btnClose = dialog.findViewById(R.id.btnCloseTimeTable);
        View colorRed = dialog.findViewById(R.id.colorRed);
        View colorOrange = dialog.findViewById(R.id.colorOrange);
        View colorGreen = dialog.findViewById(R.id.colorGreen);
        View colorBlue = dialog.findViewById(R.id.colorBlue);
        ImageButton btnEditLabel = dialog.findViewById(R.id.btnEditLabelTimeTable);

        final boolean[] isLabelEditMode = {false};

        if (bigTimeTable != null && miniTimeTable != null) {
            bigTimeTable.setEditable(true);
            bigTimeTable.setOnRequestClearAllListener(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("전체 삭제")
                        .setMessage("타임테이블을 전체 삭제하시겠습니까?")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("삭제", (dialogInterface, which) -> bigTimeTable.clearAll())
                        .show();
            });

            bigTimeTable.setCells(miniTimeTable.getCells());
            bigTimeTable.setGroupIds(miniTimeTable.getGroupIds());
            bigTimeTable.setGroupLabels(miniTimeTable.getGroupLabels());
        }

        if (colorRed != null && bigTimeTable != null && btnEditLabel != null) {
            colorRed.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#FFDCD8");
            });
        }
        if (colorOrange != null && bigTimeTable != null && btnEditLabel != null) {
            colorOrange.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#FEF6D8");
            });
        }
        if (colorGreen != null && bigTimeTable != null && btnEditLabel != null) {
            colorGreen.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#F7F9E7");
            });
        }
        if (colorBlue != null && bigTimeTable != null && btnEditLabel != null) {
            colorBlue.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#EBEEF6");
            });
        }

        ImageButton btnUndo = dialog.findViewById(R.id.btnUndoTimeTable);
        if (btnUndo != null && bigTimeTable != null) {
            btnUndo.setOnClickListener(v -> bigTimeTable.undoLastAction());
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        ImageButton btnClearAll = dialog.findViewById(R.id.btnClearAllTimeTable);
        if (btnClearAll != null && bigTimeTable != null) {
            btnClearAll.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("전체 삭제")
                        .setMessage("타임테이블을 전체 삭제하시겠습니까?")
                        .setNegativeButton("취소", null)
                        .setPositiveButton("삭제", (dialogInterface, which) -> bigTimeTable.clearAll())
                        .show();
            });
        }

        if (btnEditLabel != null && bigTimeTable != null) {
            btnEditLabel.setOnClickListener(v -> {
                isLabelEditMode[0] = !isLabelEditMode[0];
                if (isLabelEditMode[0]) {
                    bigTimeTable.setLabelEditMode(true);
                    btnEditLabel.setImageResource(R.drawable.label_off);
                } else {
                    bigTimeTable.setLabelEditMode(false);
                    btnEditLabel.setImageResource(R.drawable.label_on);
                }
            });
        }

        if (bigTimeTable != null) {
            bigTimeTable.setLabelEditMode(false);
            bigTimeTable.setOnLabelEditRequestListener((groupId, currentLabel) -> {
                EditText input = new EditText(this);
                input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
                input.setHint("라벨");
                input.setSingleLine(true);

                if (currentLabel != null) {
                    input.setText(currentLabel);
                    input.setSelection(currentLabel.length());
                }

                new AlertDialog.Builder(this)
                        .setTitle("라벨 수정")
                        .setView(input)
                        .setNegativeButton("삭제", (dialogInterface, which) -> bigTimeTable.setGroupLabel(groupId, ""))
                        .setNeutralButton("취소", null)
                        .setPositiveButton("저장", (dialogInterface, which) -> {
                            String label = input.getText().toString().trim();
                            if (label.length() > 4) {
                                Toast.makeText(this, "라벨은 최대 7글자까지 가능합니다", Toast.LENGTH_SHORT).show();
                                label = label.substring(0, 4);
                            }
                            bigTimeTable.setGroupLabel(groupId, label);
                        })
                        .show();
            });
        }

        dialog.setOnDismissListener(ignored -> {
            if (miniTimeTable != null && bigTimeTable != null) {
                miniTimeTable.setCells(bigTimeTable.getCells());
                miniTimeTable.setGroupIds(bigTimeTable.getGroupIds());
                miniTimeTable.setGroupLabels(bigTimeTable.getGroupLabels());
                miniTimeTable.setEditable(false);
                saveTimeTable();
            }
        });

        dialog.show();
    }

    private void saveTimeTable() {
        if (miniTimeTable == null || txtDate == null) return;

        try {
            JSONObject root = new JSONObject();
            JSONArray cellRows = new JSONArray();
            JSONArray groupRows = new JSONArray();
            String[][] cells = miniTimeTable.getCells();
            int[][] groupIds = miniTimeTable.getGroupIds();

            for (int row = 0; row < cells.length; row++) {
                JSONArray cellRow = new JSONArray();
                JSONArray groupRow = new JSONArray();
                for (int col = 0; col < cells[row].length; col++) {
                    cellRow.put(cells[row][col] == null ? JSONObject.NULL : cells[row][col]);
                    groupRow.put(groupIds[row][col]);
                }
                cellRows.put(cellRow);
                groupRows.put(groupRow);
            }

            JSONObject labels = new JSONObject();
            for (Map.Entry<Integer, String> entry : miniTimeTable.getGroupLabels().entrySet()) {
                labels.put(String.valueOf(entry.getKey()), entry.getValue());
            }

            root.put("cells", cellRows);
            root.put("groupIds", groupRows);
            root.put("labels", labels);

            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_TIMETABLE_PREFIX + txtDate.getText(), root.toString())
                    .apply();
        } catch (Exception e) {
            Toast.makeText(this, "타임테이블을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTimeTable() {
        if (miniTimeTable == null || txtDate == null) return;

        String json = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getString(KEY_TIMETABLE_PREFIX + txtDate.getText(), "");
        if (json.isEmpty()) return;

        try {
            JSONObject root = new JSONObject(json);
            JSONArray cellRows = root.getJSONArray("cells");
            JSONArray groupRows = root.getJSONArray("groupIds");
            String[][] cells = new String[24][6];
            int[][] groupIds = new int[24][6];

            for (int row = 0; row < 24; row++) {
                JSONArray cellRow = cellRows.getJSONArray(row);
                JSONArray groupRow = groupRows.getJSONArray(row);
                for (int col = 0; col < 6; col++) {
                    cells[row][col] = cellRow.isNull(col) ? null : cellRow.getString(col);
                    groupIds[row][col] = groupRow.getInt(col);
                }
            }

            Map<Integer, String> labels = new HashMap<>();
            JSONObject labelObject = root.optJSONObject("labels");
            if (labelObject != null) {
                java.util.Iterator<String> keys = labelObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    labels.put(Integer.parseInt(key), labelObject.getString(key));
                }
            }

            miniTimeTable.setCells(cells);
            miniTimeTable.setGroupIds(groupIds);
            miniTimeTable.setGroupLabels(labels);
        } catch (Exception e) {
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .remove(KEY_TIMETABLE_PREFIX + txtDate.getText())
                    .apply();
        }
    }

    private void saveSummary(String date, String summary) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_SUMMARY_PREFIX + date, summary).apply();
    }

    private boolean isAiEnabled() {
        SharedPreferences settings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        return settings.getBoolean("isAiEnabled", true);
    }

    private void generateDiarySummary(String content) {
        if (!isAiEnabled()) {
            if (tvDiarySummary != null) tvDiarySummary.setText("AI 기능이 OFF 상태입니다.");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        GeminiManager.INSTANCE.generateSummary(content, new GeminiManager.SummaryCallback() {
            @Override
            public void onSuccess(String summary) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvDiarySummary != null) tvDiarySummary.setText(summary);

                    if (txtDate != null) {
                        String diaryDate = txtDate.getText().toString();
                        saveSummary(diaryDate, summary);

                        // 🌟 [핵심 수리] 일기 요약도 UID 대신 이메일 이름표를 사용해 업로드!
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();
                            Map<String, Object> serverDiary = new HashMap<>();
                            serverDiary.put("summary", summary);

                            FirebaseFirestore.getInstance().collection("users").document(myEmailKey)
                                    .collection("summaries").document(diaryDate).set(serverDiary);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (tvDiarySummary != null) tvDiarySummary.setText(error);
                });
            }
        });
    }
}