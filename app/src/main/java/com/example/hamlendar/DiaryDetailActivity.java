package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;

import android.widget.EditText;
import android.widget.Button;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public class DiaryDetailActivity extends AppCompatActivity {

    private static final String PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";

    private TextView txtDate;
    private EditText editContent;
    private String originalContent = "";
    private boolean isSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        txtDate = findViewById(R.id.txtDate);
        editContent = findViewById(R.id.editContent);
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView menuIcon = findViewById(R.id.menuIcon);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnDeleteDiary = findViewById(R.id.btnDeleteDiary);

        String date = getIntent().getStringExtra("date");
        String content = getIntent().getStringExtra("content");

        originalContent = content == null ? "" : content;

        // 목록이나 달력에서 넘어온 일기면 해당 날짜와 내용을 보여주고, 새 일기면 오늘 날짜로 작성한다.
        txtDate.setText((date == null || date.isEmpty()) ? getCurrentDate() : date);
        editContent.setText(originalContent);

        // 뒤로가기 버튼 클릭 -> 저장하지 않은 수정 내용이 있으면 안내한다.
        btnBack.setOnClickListener(v -> handleBack());

        // 날짜 클릭 -> 일기 목록 화면으로 이동
        txtDate.setOnClickListener(v ->
                startActivity(new Intent(DiaryDetailActivity.this, DiaryListActivity.class)));

        // 저장 버튼 클릭 -> 저장만 하고 현재 화면에 남아 있는다.
        btnSave.setOnClickListener(v -> saveOnly());

        // 삭제 버튼 클릭 -> 현재 일기 삭제
        btnDeleteDiary.setOnClickListener(v -> confirmDeleteDiary());

        // 메뉴 버튼 클릭 -> 저장/삭제 선택
        menuIcon.setOnClickListener(v -> showDiaryMenu());

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
        miniTimeTable.setEditable(false);

        // 미니 타임테이블 클릭 시 큰 팝업 열기
        openTimeTableEditor.setOnClickListener(v -> {
            showTimeTableDialog();
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
        return sdf.format(new Date());
    }

    private void handleBack() {
        if (isSaved || !hasChanged()) {
            finish();
        } else {
            Toast.makeText(this, "저장 버튼을 눌러주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasChanged() {
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
            originalContent = editContent.getText().toString().trim();
        }
    }

    private boolean saveDiary() {
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
                    Toast.makeText(this, "일기가 수정되었습니다", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            diaries.put(newDiary);
            prefs.edit().putString(KEY_DIARY_LIST, diaries.toString()).apply();
            Toast.makeText(this, "일기가 저장되었습니다", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "일기를 저장할 수 없습니다", Toast.LENGTH_SHORT).show();
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
            isSaved = true;
            Toast.makeText(this, "일기가 삭제되었습니다", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "일기를 삭제할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private TimeTable miniTimeTable;
    private View openTimeTableEditor;

    private void showTimeTableDialog() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_timetable);

        Window window = dialog.getWindow();

        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );

            // 뒤 배경 어둡게
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams params = window.getAttributes();

            // 어두워지는 정도
            // 0.0 = 안 어두움
            // 1.0 = 완전 검정
            params.dimAmount = 0.45f;

            window.setAttributes(params);
        }

        TimeTable bigTimeTable =
                dialog.findViewById(R.id.bigTimeTable);

        ImageView btnClose =
                dialog.findViewById(R.id.btnCloseTimeTable);

        View colorRed =
                dialog.findViewById(R.id.colorRed);

        View colorOrange =
                dialog.findViewById(R.id.colorOrange);

        View colorGreen =
                dialog.findViewById(R.id.colorGreen);

        View colorBlue =
                dialog.findViewById(R.id.colorBlue);

        Button btnEditLabel =
                dialog.findViewById(R.id.btnEditLabelTimeTable);

        final boolean[] isLabelEditMode = {false};

        // 큰 타임테이블은 편집 가능
        bigTimeTable.setEditable(true);

        bigTimeTable.setOnRequestClearAllListener(() -> {

            new AlertDialog.Builder(this)
                    .setTitle("전체 삭제")
                    .setMessage("타임테이블을 전체 삭제하시겠습니까?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("삭제", (dialogInterface, which) -> {
                        bigTimeTable.clearAll();
                    })
                    .show();
        });

        // 미니 타임테이블의 현재 내용을 큰 타임테이블에 복사
        bigTimeTable.setCells(miniTimeTable.getCells());

        // 색상 선택
        colorRed.setOnClickListener(v -> {
            isLabelEditMode[0] = false;
            btnEditLabel.setText("라벨 수정");
            bigTimeTable.setLabelEditMode(false);
            bigTimeTable.setDrawMode();
            bigTimeTable.setSelectedColor("#FF5252");
        });

        colorOrange.setOnClickListener(v -> {
            isLabelEditMode[0] = false;
            btnEditLabel.setText("라벨 수정");
            bigTimeTable.setLabelEditMode(false);
            bigTimeTable.setDrawMode();
            bigTimeTable.setSelectedColor("#FF9800");
        });

        colorGreen.setOnClickListener(v -> {
            isLabelEditMode[0] = false;
            btnEditLabel.setText("라벨 수정");
            bigTimeTable.setLabelEditMode(false);
            bigTimeTable.setDrawMode();
            bigTimeTable.setSelectedColor("#4CAF50");
        });

        colorBlue.setOnClickListener(v -> {
            isLabelEditMode[0] = false;
            btnEditLabel.setText("라벨 수정");
            bigTimeTable.setLabelEditMode(false);
            bigTimeTable.setDrawMode();
            bigTimeTable.setSelectedColor("#2196F3");
        });

        //색칠모드
        Button btnUndo =
                dialog.findViewById(R.id.btnUndoTimeTable);

        Button btnEraser =
                dialog.findViewById(R.id.btnClearAllTimeTable);



        //지우개
        btnUndo.setOnClickListener(v -> {
            bigTimeTable.undoLastAction();
        });


        // X 버튼 누르면 저장 후 팝업 닫기
        btnClose.setOnClickListener(v -> {

            // 큰 타임테이블 내용을 미니 타임테이블에 반영
            miniTimeTable.setCells(bigTimeTable.getCells());

            // 미니 타임테이블은 다시 보기 전용 유지
            miniTimeTable.setEditable(false);

            // TODO: 여기에 Firebase 저장 코드 추가 가능

            dialog.dismiss();
        });

        dialog.show();


        Button btnClearAll =
                dialog.findViewById(R.id.btnClearAllTimeTable);

        btnClearAll.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("전체 삭제")
                    .setMessage("타임테이블을 전체 삭제하시겠습니까?")

                    .setNegativeButton("취소", null)

                    .setPositiveButton("삭제", (dialogInterface, which) -> {
                        bigTimeTable.clearAll();
                    })

                    .show();
        });


// 라벨 수정 버튼
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



        bigTimeTable.setLabelEditMode(false);

        bigTimeTable.setOnLabelEditRequestListener((groupId, currentLabel) -> {

            EditText input = new EditText(this);
            input.setHint("라벨");
            input.setSingleLine(true);

            if (currentLabel != null) {
                input.setText(currentLabel);
                input.setSelection(currentLabel.length());
            }

            new AlertDialog.Builder(this)
                    .setTitle("라벨 수정")
                    .setView(input)

                    .setNegativeButton("삭제", (dialogInterface, which) -> {
                        bigTimeTable.setGroupLabel(groupId, "");
                    })

                    .setNeutralButton("취소", null)

                    .setPositiveButton("저장", (dialogInterface, which) -> {
                        String label = input.getText().toString().trim();
                        bigTimeTable.setGroupLabel(groupId, label);
                    })

                    .show();
        });


    }



}
