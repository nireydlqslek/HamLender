package com.example.hamlendar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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

        String date = getIntent().getStringExtra("date");
        String weather = getIntent().getStringExtra("weather");
        String content = getIntent().getStringExtra("content");

        // 목록에서 넘어온 일기 데이터가 있으면 표시하고, 없으면 오늘 날짜로 새 일기 작성
        txtDate.setText((date == null || date.isEmpty()) ? getCurrentDate() : date);
        editWeather.setText(weather == null ? "" : weather);
        editContent.setText(content == null ? "" : content);

        // 뒤로가기/메뉴 버튼 클릭 시 일기를 저장하고 화면 닫기
        btnBack.setOnClickListener(v -> saveAndClose());
        menuIcon.setOnClickListener(v -> saveAndClose());

        // 휴대폰 뒤로가기 버튼을 눌러도 저장 후 닫히도록 처리
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                saveAndClose();
            }
        });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
        return sdf.format(new Date());
    }

    private void saveAndClose() {
        // 중복 저장을 막고 한 번만 저장
        if (!isSaved) {
            saveDiary();
            isSaved = true;
        }
        finish();
    }

    private void saveDiary() {
        // 화면에 입력된 날짜, 날씨, 일기 내용 가져오기
        String date = txtDate.getText().toString();
        String weather = editWeather.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        // 아무 내용도 없으면 저장하지 않음
        if (weather.isEmpty() && content.isEmpty()) {
            return;
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
                    Toast.makeText(this, "일기가 저장되었습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 같은 날짜가 없으면 새 일기로 추가
            diaries.put(newDiary);
            prefs.edit().putString(KEY_DIARY_LIST, diaries.toString()).apply();
            Toast.makeText(this, "일기가 저장되었습니다", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "일기를 저장할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }
}
