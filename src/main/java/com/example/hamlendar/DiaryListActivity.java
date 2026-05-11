package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    private ImageView btnCal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        RecyclerView recyclerDiary = findViewById(R.id.recyclerDiary);

        txtCurrentDate = findViewById(R.id.txtCurrentDate);

        View fabAddDiary = findViewById(R.id.fabAddDiary);

        btnCal = findViewById(R.id.img_diary_cal);

        btnCal.setOnClickListener(v ->
                startActivity(new Intent(
                        DiaryListActivity.this,
                        MainActivity.class
                )));

        // 오늘 날짜 표시
        txtCurrentDate.setText(getTodayDate());

        // RecyclerView 연결
        diaryAdapter = new DiaryAdapter();

        recyclerDiary.setLayoutManager(
                new LinearLayoutManager(this));

        recyclerDiary.setAdapter(diaryAdapter);

        // 새 일기 작성
        fabAddDiary.setOnClickListener(v ->
                openDiary(getTodayDate(), "", ""));
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadDiaryList();

        diaryAdapter.notifyDataSetChanged();
    }

    private String getTodayDate() {

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "M월 d일 (E)",
                        Locale.KOREAN
                );

        return sdf.format(new Date());
    }

    private void openDiary(
            String date,
            String weather,
            String content
    ) {

        Intent intent =
                new Intent(
                        DiaryListActivity.this,
                        DiaryDetailActivity.class
                );

        intent.putExtra("date", date);
        intent.putExtra("weather", weather);
        intent.putExtra("content", content);

        startActivity(intent);
    }

    // 저장된 일기 목록 불러오기
    private void loadDiaryList() {

        diaryList.clear();

        SharedPreferences prefs =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String json =
                prefs.getString(
                        KEY_DIARY_LIST,
                        "[]"
                );

        try {

            JSONArray jsonArray =
                    new JSONArray(json);

            // 최신 일기가 위에 보이도록 역순
            for (int i = jsonArray.length() - 1; i >= 0; i--) {

                JSONObject obj =
                        jsonArray.getJSONObject(i);

                diaryList.add(
                        new DiaryItem(
                                obj.optString("date", ""),
                                obj.optString("weather", ""),
                                obj.optString("content", "")
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 수정/삭제 후 다시 저장
    private void saveDiaryList() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        SharedPreferences.Editor editor =
                prefs.edit();

        JSONArray jsonArray = new JSONArray();

        try {

            // 원래 저장 순서 유지 위해 역순 저장
            for (int i = diaryList.size() - 1; i >= 0; i--) {

                DiaryItem item = diaryList.get(i);

                JSONObject obj = new JSONObject();

                obj.put("date", item.date);
                obj.put("weather", item.weather);
                obj.put("content", item.content);

                jsonArray.put(obj);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        editor.putString(
                KEY_DIARY_LIST,
                jsonArray.toString()
        );

        editor.apply();
    }

    // 일기 데이터 클래스
    private static class DiaryItem {

        private final String date;
        private final String weather;
        private final String content;

        DiaryItem(
                String date,
                String weather,
                String content
        ) {

            this.date = date;
            this.weather = weather;
            this.content = content;
        }
    }

    // RecyclerView Adapter
    private class DiaryAdapter
            extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

        @NonNull
        @Override
        public DiaryViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType
        ) {

            View view =
                    LayoutInflater.from(
                                    DiaryListActivity.this
                            )
                            .inflate(
                                    R.layout.item_diary,
                                    parent,
                                    false
                            );

            return new DiaryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull DiaryViewHolder holder,
                int position
        ) {

            DiaryItem item = diaryList.get(position);

            holder.txtDiaryDate.setText(item.date);

            String preview = item.content;

            if (preview == null ||
                    preview.trim().isEmpty()) {

                preview = "(내용 없음)";
            }

            holder.txtDiaryPreview.setText(preview);

            // 카드 클릭 -> 상세보기
            holder.itemView.setOnClickListener(v ->
                    openDiary(
                            item.date,
                            item.weather,
                            item.content
                    ));

            // 수정 버튼
            holder.btnEdit.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                DiaryListActivity.this,
                                DiaryDetailActivity.class
                        );

                intent.putExtra("date", item.date);
                intent.putExtra("weather", item.weather);
                intent.putExtra("content", item.content);

                startActivity(intent);
            });

            // 삭제 버튼
            holder.btnDelete.setOnClickListener(v -> {

                new AlertDialog.Builder(
                        DiaryListActivity.this
                )

                        .setTitle("일기 삭제")

                        .setMessage(
                                "정말 삭제하시겠습니까?"
                        )

                        .setPositiveButton(
                                "삭제",
                                (dialog, which) -> {

                                    diaryList.remove(position);

                                    saveDiaryList();

                                    notifyItemRemoved(position);

                                    notifyItemRangeChanged(
                                            position,
                                            diaryList.size()
                                    );
                                })

                        .setNegativeButton(
                                "취소",
                                null
                        )

                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return diaryList.size();
        }

        class DiaryViewHolder
                extends RecyclerView.ViewHolder {

            private final TextView txtDiaryDate;

            private final TextView txtDiaryPreview;

            private final ImageView btnEdit;

            private final ImageView btnDelete;

            DiaryViewHolder(@NonNull View itemView) {

                super(itemView);

                txtDiaryDate =
                        itemView.findViewById(
                                R.id.txtDiaryDate
                        );

                txtDiaryPreview =
                        itemView.findViewById(
                                R.id.txtDiaryPreview
                        );

                btnEdit =
                        itemView.findViewById(
                                R.id.btnEdit
                        );

                btnDelete =
                        itemView.findViewById(
                                R.id.btnDelete
                        );
            }
        }
    }
}

