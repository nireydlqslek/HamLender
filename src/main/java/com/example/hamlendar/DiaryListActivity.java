package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DiaryListActivity extends AppCompatActivity {

    private static final String PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";

    private TextView txtCurrentDate;
    private final ArrayList<DiaryItem> diaryList = new ArrayList<>();
    private DiaryAdapter diaryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        RecyclerView recyclerDiary = findViewById(R.id.recyclerDiary);
        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        View fabAddDiary = findViewById(R.id.fabAddDiary);

        // 화면 상단에 오늘 날짜 표시
        txtCurrentDate.setText(getTodayDate());

        // RecyclerView에 일기 목록 어댑터 연결
        diaryAdapter = new DiaryAdapter();
        recyclerDiary.setLayoutManager(new LinearLayoutManager(this));
        recyclerDiary.setAdapter(diaryAdapter);

        // 오른쪽 아래 추가 버튼 클릭 -> 오늘 날짜의 새 일기 작성
        fabAddDiary.setOnClickListener(v -> openDiary(getTodayDate(), "", ""));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 상세 화면에서 돌아오면 저장된 일기 목록을 다시 불러오기
        loadDiaryList();
        diaryAdapter.notifyDataSetChanged();
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
        return sdf.format(new Date());
    }

    private void openDiary(String date, String weather, String content) {
        // 선택한 일기 정보를 상세 화면으로 전달
        Intent intent = new Intent(DiaryListActivity.this, DiaryDetailActivity.class);
        intent.putExtra("date", date);
        intent.putExtra("weather", weather);
        intent.putExtra("content", content);
        startActivity(intent);
    }

    private void loadDiaryList() {
        // SharedPreferences에 저장된 일기 JSON 목록 불러오기
        diaryList.clear();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DIARY_LIST, "[]");

        try {
            JSONArray jsonArray = new JSONArray(json);

            // 최근에 저장한 일기가 위에 보이도록 역순으로 추가
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                JSONObject obj = jsonArray.getJSONObject(i);
                diaryList.add(new DiaryItem(
                        obj.optString("date", ""),
                        obj.optString("weather", ""),
                        obj.optString("content", "")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class DiaryItem {
        private final String date;
        private final String weather;
        private final String content;

        DiaryItem(String date, String weather, String content) {
            this.date = date;
            this.weather = weather;
            this.content = content;
        }
    }

    private class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

        @NonNull
        @Override
        public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(DiaryListActivity.this)
                    .inflate(R.layout.item_diary, parent, false);
            return new DiaryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
            DiaryItem item = diaryList.get(position);
            holder.txtDiaryDate.setText(item.date);

            // 일기 내용은 카드에서 미리보기로 표시
            String preview = item.content;
            if (preview == null || preview.trim().isEmpty()) {
                preview = "(내용 없음)";
            }
            holder.txtDiaryPreview.setText(preview);

            // 일기 카드 클릭 -> 해당 일기 상세 화면으로 이동
            holder.itemView.setOnClickListener(v -> openDiary(item.date, item.weather, item.content));
        }

        @Override
        public int getItemCount() {
            return diaryList.size();
        }

        class DiaryViewHolder extends RecyclerView.ViewHolder {
            private final TextView txtDiaryDate;
            private final TextView txtDiaryPreview;

            DiaryViewHolder(@NonNull View itemView) {
                super(itemView);
                txtDiaryDate = itemView.findViewById(R.id.txtDiaryDate);
                txtDiaryPreview = itemView.findViewById(R.id.txtDiaryPreview);
            }
        }
    }
}
