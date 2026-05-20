package com.example.hamlendar;

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

public class DiaryDetailActivity extends AppCompatActivity {

    private static final String PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";

    private TextView txtDate;
    private EditText editWeather;
    private EditText editContent;
    private String originalWeather = "";
    private String originalContent = "";
    private boolean isSaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_detail);

        txtDate = findViewById(R.id.txtDate);
        editWeather = findViewById(R.id.editWeather);
        editContent = findViewById(R.id.editContent);
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView menuIcon = findViewById(R.id.menuIcon);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnDeleteDiary = findViewById(R.id.btnDeleteDiary);

        String date = getIntent().getStringExtra("date");
        String weather = getIntent().getStringExtra("weather");
        String content = getIntent().getStringExtra("content");

        originalWeather = weather == null ? "" : weather;
        originalContent = content == null ? "" : content;

        // 목록에서 넘어온 일기 데이터가 있으면 표시하고, 없으면 오늘 날짜로 새 일기 작성
        txtDate.setText((date == null || date.isEmpty()) ? getCurrentDate() : date);
        editWeather.setText(originalWeather);
        editContent.setText(originalContent);

        // 뒤로가기 버튼 클릭 -> 수정 안 했거나 저장했다면 나가고, 수정했으면 저장 안내
        btnBack.setOnClickListener(v -> handleBack());

        // 저장 버튼 클릭 -> 저장만 하고 화면은 유지
        btnSave.setOnClickListener(v -> saveOnly());

        // 상세 화면 안의 삭제 버튼 클릭 -> 해당 일기 삭제
        btnDeleteDiary.setOnClickListener(v -> confirmDeleteDiary());

        // 메뉴 버튼 클릭 -> 저장/삭제 선택
        menuIcon.setOnClickListener(v -> showDiaryMenu());

        // 휴대폰 뒤로가기 버튼도 화면의 뒤로가기와 똑같이 처리
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private String getCurrentDate() {
        // 오늘 날짜를 일기에서 쓰는 형식으로 변환
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
        String currentWeather = editWeather.getText().toString().trim();
        String currentContent = editContent.getText().toString().trim();
        return !currentWeather.equals(originalWeather) || !currentContent.equals(originalContent);
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
            originalWeather = editWeather.getText().toString().trim();
            originalContent = editContent.getText().toString().trim();
        }
    }

    private boolean saveDiary() {
        // 화면에 입력된 날짜, 날씨, 일기 내용 가져오기
        String date = txtDate.getText().toString();
        String weather = editWeather.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        // 아무 내용도 없으면 저장하지 않음
        if (weather.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "일기 내용을 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DIARY_LIST, "[]");

        try {
            JSONArray diaries = new JSONArray(json);
            JSONObject newDiary = new JSONObject();
            newDiary.put("date", date);
            newDiary.put("weather", weather);
            newDiary.put("content", content);

            // 같은 날짜의 일기가 이미 있으면 새 내용으로 덮어쓰기
            for (int i = 0; i < diaries.length(); i++) {
                JSONObject diary = diaries.getJSONObject(i);
                if (date.equals(diary.optString("date"))) {
                    diaries.put(i, newDiary);
                    prefs.edit().putString(KEY_DIARY_LIST, diaries.toString()).apply();
                    Toast.makeText(this, "일기가 수정되었습니다", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            // 같은 날짜가 없으면 새 일기로 추가
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

            // 삭제할 날짜와 다른 일기만 새 배열에 다시 담기
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
}
