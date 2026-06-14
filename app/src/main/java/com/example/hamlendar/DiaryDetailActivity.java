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
import android.widget.ImageButton; // 🌟 필수 추가: XML 도면의 ImageButton과 연결하기 위한 도구
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

        // 🌟 [버그 해체] XML 도면과 매칭되도록 일반 Button에서 ImageButton으로 완벽 정정!
        ImageButton btnSave = findViewById(R.id.btnSave);
        ImageButton btnDeleteDiary = findViewById(R.id.btnDeleteDiary);

        String date = getIntent().getStringExtra("date");
        String content = getIntent().getStringExtra("content");

        originalContent = content == null ? "" : content;

        SharedPreferences settings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isAiEnabled = settings.getBoolean("isAiEnabled", true);

        // 목록이나 달력에서 넘어온 일기면 해당 날짜와 내용을 보여주고, 새 일기면 오늘 날짜로 작성한다.
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

        // 뒤로가기 버튼 클릭 -> 저장하지 않은 수정 내용이 있으면 안내한다.
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBack());
        }

        // 날짜 클릭 -> 일기 목록 화면으로 이동
        if (txtDate != null) {
            txtDate.setOnClickListener(v ->
                    startActivity(new Intent(DiaryDetailActivity.this, DiaryListActivity.class)));
        }

        // 저장 버튼 클릭 -> 저장만 하고 현재 화면에 남아 있는다.
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveOnly());
        }

        // 삭제 버튼 클릭 -> 현재 일기 삭제
        if (btnDeleteDiary != null) {
            btnDeleteDiary.setOnClickListener(v -> confirmDeleteDiary());
        }

        // 메뉴 버튼 클릭 -> 저장/삭제 선택
        if (menuIcon != null) {
            menuIcon.setOnClickListener(v -> showDiaryMenu());
        }

        // 휴대폰 기본 뒤로가기도 화면의 뒤로가기 버튼과 똑같이 처리한다.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });

        miniTimeTable = findViewById(R.id.miniTimeTable);
        openTimeTableEditor = findViewById(R.id.openTimeTableEditor);

        // 미니 타임테이블은 보기 전용
        if (miniTimeTable != null) {
            miniTimeTable.setEditable(false);
        }

        // 미니 타임테이블 클릭 시 큰 팝업 열기
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

            prefs.edit().putString(KEY_DIARY_LIST, newDiaries.toString()).apply();
            prefs.edit().remove(KEY_SUMMARY_PREFIX + date).apply();

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
        Button btnEditLabel = dialog.findViewById(R.id.btnEditLabelTimeTable);

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
                btnEditLabel.setText("라벨 수정");
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#FF5252");
            });
        }
        if (colorOrange != null && bigTimeTable != null && btnEditLabel != null) {
            colorOrange.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                btnEditLabel.setText("라벨 수정");
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#FF9800");
            });
        }
        if (colorGreen != null && bigTimeTable != null && btnEditLabel != null) {
            colorGreen.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                btnEditLabel.setText("라벨 수정");
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#4CAF50");
            });
        }
        if (colorBlue != null && bigTimeTable != null && btnEditLabel != null) {
            colorBlue.setOnClickListener(v -> {
                isLabelEditMode[0] = false;
                btnEditLabel.setText("라벨 수정");
                bigTimeTable.setLabelEditMode(false);
                bigTimeTable.setDrawMode();
                bigTimeTable.setSelectedColor("#2196F3");
            });
        }

        Button btnUndo = dialog.findViewById(R.id.btnUndoTimeTable);
        if (btnUndo != null && bigTimeTable != null) {
            btnUndo.setOnClickListener(v -> bigTimeTable.undoLastAction());
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                if (miniTimeTable != null && bigTimeTable != null) {
                    miniTimeTable.setCells(bigTimeTable.getCells());
                    miniTimeTable.setGroupIds(bigTimeTable.getGroupIds());
                    miniTimeTable.setGroupLabels(bigTimeTable.getGroupLabels());
                    miniTimeTable.setEditable(false);
                }
                dialog.dismiss();
            });
        }

        Button btnClearAll = dialog.findViewById(R.id.btnClearAllTimeTable);
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
                    btnEditLabel.setText("수정 완료");
                } else {
                    bigTimeTable.setLabelEditMode(false);
                    btnEditLabel.setText("라벨 수정");
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

        dialog.show();
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
                        String diaryDate = txtDate.getText().toString(); // "6월 14일 (일)" 같은 한글 날짜 이름표 추출
                        saveSummary(diaryDate, summary); // 1. 기존 로컬 캐시 저장

                        // 🌟 [서버 백업 연동] 친구들이 실시간으로 읽어갈 수 있게 파이어베이스 클라우드에도 동일 이름표로 업로드!
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            Map<String, Object> serverDiary = new HashMap<>();
                            serverDiary.put("summary", summary);
                            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
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