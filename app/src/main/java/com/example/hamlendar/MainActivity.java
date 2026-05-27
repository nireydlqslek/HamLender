package com.example.hamlendar;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String DIARY_PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";

    private CalendarView calendarView;
    private TextView nameTitle;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<LocalDate, List<ScheduleItem>> schedulesMap = new HashMap<>();

    class ScheduleItem {
        String id;
        String title;
        String category;
        String memo;

        ScheduleItem(String id, String title, String category, String memo) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.memo = memo;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendarView = findViewById(R.id.calendarView);
        nameTitle = findViewById(R.id.nameTitle);

        ImageView menuIcon = findViewById(R.id.menu_icon);
        ImageView btnDiary = findViewById(R.id.img_main_diary);

        menuIcon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingActivity.class)));

        // 하단 일기 버튼은 오늘 날짜 일기를 연다.
        btnDiary.setOnClickListener(v -> openDiaryForDate(LocalDate.now()));

        setGreetingName();
        loadSchedulesFromFirebase();
        setupCalendar();
    }

    private void setupCalendar() {
        YearMonth currentMonth = YearMonth.now();

        calendarView.setup(
                currentMonth.minusMonths(12),
                currentMonth.plusMonths(12),
                DayOfWeek.SUNDAY
        );

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                container.day = day;

                container.tvEvent1.setText("");
                container.tvEvent2.setText("");
                container.tvEvent3.setText("");
                container.tvEvent4.setText("");
                container.tvEvent5.setText("");
                container.tvMore.setText("");

                if (day.getPosition() != DayPosition.MonthDate) {
                    container.tvDate.setText(String.valueOf(day.getDate().getDayOfMonth()));

                    List<ScheduleItem> todaySchedules = schedulesMap.get(day.getDate());
                    if (todaySchedules == null || todaySchedules.isEmpty()) {
                        int size = todaySchedules.size();
                        if (size >= 1)
                            container.tvEvent1.setText("• " + todaySchedules.get(0).title);
                        if (size >= 2)
                            container.tvEvent2.setText("• " + todaySchedules.get(1).title);
                        if (size >= 3)
                            container.tvEvent3.setText("• " + todaySchedules.get(2).title);
                        if (size >= 4)
                            container.tvEvent4.setText("• " + todaySchedules.get(3).title);
                        if (size >= 5)
                            container.tvEvent5.setText("• " + todaySchedules.get(4).title);
                        if (size > 5) container.tvMore.setText("+" + (size - 5));
                    }
                }
            }
        });

        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<MonthHeaderContainer>() {
            @Override
            public MonthHeaderContainer create(View view) {
                return new MonthHeaderContainer(view);
            }

            @Override
            public void bind(MonthHeaderContainer container, CalendarMonth month) {
                YearMonth yearMonth = month.getYearMonth();
                container.tvMonth.setText(yearMonth.getYear() + "년 " + yearMonth.getMonthValue() + "월");

                container.btnPrev.setOnClickListener(v ->
                        calendarView.smoothScrollToMonth(yearMonth.minusMonths(1)));

                container.btnNext.setOnClickListener(v ->
                        calendarView.smoothScrollToMonth(yearMonth.plusMonths(1)));
            }
        });

        calendarView.scrollToMonth(currentMonth);
    }

    private void openDiaryForDate(LocalDate date) {
        String diaryDate = formatDiaryDate(date);
        Intent intent = new Intent(MainActivity.this, DiaryDetailActivity.class);
        intent.putExtra("date", diaryDate);
        intent.putExtra("content", findDiaryContent(diaryDate));
        startActivity(intent);
    }

    private String formatDiaryDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
        return date.format(formatter);
    }

    private String findDiaryContent(String date) {
        SharedPreferences prefs = getSharedPreferences(DIARY_PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DIARY_LIST, "[]");

        try {
            JSONArray diaries = new JSONArray(json);
            for (int i = 0; i < diaries.length(); i++) {
                JSONObject diary = diaries.getJSONObject(i);
                if (date.equals(diary.optString("date"))) {
                    return diary.optString("content", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private void loadSchedulesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("schedules")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    schedulesMap.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String dateStr = document.getString("date");
                        String title = document.getString("title");
                        String category = document.getString("category");
                        String memo = document.getString("memo");

                        if (dateStr != null && title != null) {
                            LocalDate date = LocalDate.parse(dateStr);
                            if (!schedulesMap.containsKey(date)) {
                                schedulesMap.put(date, new ArrayList<>());
                            }
                            schedulesMap.get(date).add(new ScheduleItem(id, title, category, memo));
                        }
                    }
                    calendarView.notifyCalendarChanged();
                });
    }

    private void showScheduleDialog(LocalDate date) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_schedule);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
        LinearLayout layoutScheduleList = dialog.findViewById(R.id.layoutScheduleList);
        Button btnAddSchedule = dialog.findViewById(R.id.btnAddSchedule);
        LinearLayout layoutInputForm = dialog.findViewById(R.id.layoutInputForm);

        EditText etCategory = dialog.findViewById(R.id.etCategory);
        EditText etTitle = dialog.findViewById(R.id.etTitle);
        EditText etMemo = dialog.findViewById(R.id.etMemo);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN);
        tvDialogDate.setText(date.format(formatter));

        final String[] editDocId = {null};

        layoutScheduleList.removeAllViews();
        List<ScheduleItem> todaySchedules = schedulesMap.get(date);

        if (todaySchedules != null) {
            for (ScheduleItem item : todaySchedules) {
                View itemView = getLayoutInflater().inflate(R.layout.item_schedule, null);
                TextView tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
                ImageView ivDelete = itemView.findViewById(R.id.ivDelete);

                tvItemTitle.setText(item.title);

                ivDelete.setOnClickListener(v -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        db.collection("users").document(user.getUid())
                                .collection("schedules").document(item.id)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MainActivity.this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadSchedulesFromFirebase();
                                });
                    }
                });

                itemView.setOnClickListener(v -> {
                    layoutInputForm.setVisibility(View.VISIBLE);
                    editDocId[0] = item.id;
                    etCategory.setText(item.category);
                    etTitle.setText(item.title);
                    etMemo.setText(item.memo);
                    btnSave.setText("수정하기");
                });

                layoutScheduleList.addView(itemView);
            }
        }

        btnAddSchedule.setOnClickListener(v -> {
            if (layoutInputForm.getVisibility() == View.GONE) {
                layoutInputForm.setVisibility(View.VISIBLE);
                editDocId[0] = null;
                etCategory.setText("");
                etTitle.setText("");
                etMemo.setText("");
                btnSave.setText("저장하기");
            } else {
                layoutInputForm.setVisibility(View.GONE);
            }
        });

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(MainActivity.this, "일정을 입력해주세요!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Map<String, Object> scheduleData = new HashMap<>();
                scheduleData.put("date", date.toString());
                scheduleData.put("category", etCategory.getText().toString().trim());
                scheduleData.put("title", title);
                scheduleData.put("memo", etMemo.getText().toString().trim());
                scheduleData.put("timestamp", System.currentTimeMillis());

                if (editDocId[0] == null) {
                    db.collection("users").document(user.getUid())
                            .collection("schedules")
                            .add(scheduleData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(MainActivity.this, "저장 성공!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadSchedulesFromFirebase();
                            });
                } else {
                    db.collection("users").document(user.getUid())
                            .collection("schedules").document(editDocId[0])
                            .update(scheduleData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "수정 완료!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadSchedulesFromFirebase();
                            });
                }
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void setGreetingName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userName = user == null ? "" : user.getDisplayName();

        if (TextUtils.isEmpty(userName)) {
            SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
            userName = prefs.getString("user_name", "");
        }

        if (TextUtils.isEmpty(userName)) {
            userName = "사용자";
        }

        String greeting = userName + "님, 안녕하세요!";
        SpannableString spannableGreeting = new SpannableString(greeting);

        int nameColor = ContextCompat.getColor(this, R.color.namecolor);
        int greetingColor = ContextCompat.getColor(this, R.color.maincolor);

        spannableGreeting.setSpan(
                new ForegroundColorSpan(nameColor),
                0,
                userName.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannableGreeting.setSpan(
                new ForegroundColorSpan(greetingColor),
                userName.length(),
                greeting.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        nameTitle.setText(spannableGreeting);
    }

    class DayViewContainer extends ViewContainer {
        TextView tvDate;
        TextView tvEvent1;
        TextView tvEvent2;
        TextView tvEvent3;
        TextView tvEvent4;
        TextView tvEvent5;
        TextView tvMore;
        CalendarDay day;

        DayViewContainer(View view) {
            super(view);
            tvDate = view.findViewById(R.id.tvDate);
            tvEvent1 = view.findViewById(R.id.tvEvent1);
            tvEvent2 = view.findViewById(R.id.tvEvent2);
            tvEvent3 = view.findViewById(R.id.tvEvent3);
            tvEvent4 = view.findViewById(R.id.tvEvent4);
            tvEvent5 = view.findViewById(R.id.tvEvent5);
            tvMore = view.findViewById(R.id.tvMore);

            view.setOnClickListener(v -> {
                if (day != null && day.getPosition() == DayPosition.MonthDate) {
                    openDiaryForDate(day.getDate());
                }
            });

            view.setOnLongClickListener(v -> {
                if (day != null && day.getPosition() == DayPosition.MonthDate) {
                    showScheduleDialog(day.getDate());
                    return true;
                }
                return false;
            });
        }
    }

    class MonthHeaderContainer extends ViewContainer {
        TextView tvMonth;
        ImageView btnPrev;
        ImageView btnNext;

        MonthHeaderContainer(View view) {
            super(view);
            tvMonth = view.findViewById(R.id.tvMonth);
            btnPrev = view.findViewById(R.id.btnPrev);
            btnNext = view.findViewById(R.id.btnNext);
        }
    }
}
