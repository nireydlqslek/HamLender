package com.example.hamlendar;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

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
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String DIARY_PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";
    private static final String HEALTH_PREF_NAME = "health_pref";
    private static final String KEY_HEALTH_LIST = "health_list";

    private CalendarView calendarView;
    private TextView nameTitle;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<LocalDate, List<ScheduleItem>> schedulesMap = new HashMap<>();
    private final Map<LocalDate, List<CalendarEvent>> healthEventsMap = new HashMap<>();

    static class CalendarEvent {
        final String text;
        final Integer backgroundColor;

        CalendarEvent(String text, Integer backgroundColor) {
            this.text = text;
            this.backgroundColor = backgroundColor;
        }
    }

    // 🌟 카테고리 > 일정 > 메모 구조화용 ScheduleItem 클래스
    class ScheduleItem {
        String id;
        String title;
        String categoryId;
        String category;
        String memo;

        public ScheduleItem(String id, String title, String categoryId, String category, String memo) {
            this.id = id;
            this.title = title;
            this.categoryId = categoryId;
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
        ImageView btnHealth = findViewById(R.id.img_main_health);
        ImageView btnDiary = findViewById(R.id.img_main_diary);

        menuIcon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingActivity.class)));

        btnHealth.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HealthActivity.class)));

        btnDiary.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DiaryListActivity.class)));

        setGreetingName();
        setupCalendar();
        loadSchedulesFromFirebase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHealthEvents();
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
                resetEventView(container.tvEvent1);
                resetEventView(container.tvEvent2);
                resetEventView(container.tvEvent3);
                resetEventView(container.tvEvent4);
                resetEventView(container.tvEvent5);

                if (day.getPosition() == DayPosition.MonthDate) {
                    container.tvDate.setText(String.valueOf(day.getDate().getDayOfMonth()));

                    List<CalendarEvent> dayEvents = getCalendarEvents(day.getDate());
                    TextView[] eventViews = {
                            container.tvEvent1,
                            container.tvEvent2,
                            container.tvEvent3,
                            container.tvEvent4,
                            container.tvEvent5
                    };

                    int displayCount = Math.min(dayEvents.size(), eventViews.length);
                    for (int i = 0; i < displayCount; i++) {
                        bindEventView(eventViews[i], dayEvents.get(i));
                    }
                    if (dayEvents.size() > eventViews.length) {
                        container.tvMore.setText("+" + (dayEvents.size() - eventViews.length));
                    }
                } else {
                    container.tvDate.setText("");
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
                        calendarView.smoothScrollToMonth(yearMonth.minusMonths(1))
                );

                container.btnNext.setOnClickListener(v ->
                        calendarView.smoothScrollToMonth(yearMonth.plusMonths(1))
                );
            }
        });

        calendarView.scrollToMonth(currentMonth);
    }

    private List<CalendarEvent> getCalendarEvents(LocalDate date) {
        List<CalendarEvent> events = new ArrayList<>();

        List<ScheduleItem> schedules = schedulesMap.get(date);
        if (schedules != null) {
            for (ScheduleItem schedule : schedules) {
                events.add(new CalendarEvent("• " + schedule.title, null));
            }
        }

        List<CalendarEvent> healthEvents = healthEventsMap.get(date);
        if (healthEvents != null) {
            events.addAll(healthEvents);
        }
        return events;
    }

    private void bindEventView(TextView view, CalendarEvent event) {
        view.setText(event.text);
        if (event.backgroundColor == null) {
            resetEventView(view);
            return;
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(event.backgroundColor);
        background.setCornerRadius(dpToPx(4));
        view.setBackground(background);
        view.setPadding(dpToPx(3), 0, dpToPx(3), 0);
    }

    private void resetEventView(TextView view) {
        view.setBackground(null);
        view.setPadding(0, 0, 0, 0);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void loadHealthEvents() {
        healthEventsMap.clear();

        SharedPreferences prefs = getSharedPreferences(HEALTH_PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_HEALTH_LIST, "[]");
        YearMonth currentMonth = YearMonth.now();
        LocalDate rangeStart = currentMonth.minusMonths(12).atDay(1);
        LocalDate rangeEnd = currentMonth.plusMonths(12).atEndOfMonth();

        try {
            JSONArray items = new JSONArray(json);
            for (int i = 0; i < items.length(); i++) {
                try {
                    JSONObject item = items.getJSONObject(i);
                    int cycleValue = item.optInt("cycleValue", 0);
                    if (cycleValue <= 0) continue;

                    LocalDate startDate = LocalDate.parse(item.optString("startDate", ""));
                    String cycleUnit = item.optString("cycleUnit", "일");
                    String category = item.optString("category", "건강");
                    String content = item.optString("content", "").trim();
                    String title = item.optString("title", "").trim();
                    String label = !content.isEmpty()
                            ? content
                            : !title.isEmpty() ? title : category;
                    int color = item.optInt("color", Color.rgb(239, 248, 232));

                    addHealthOccurrences(
                            startDate,
                            cycleValue,
                            cycleUnit,
                            label,
                            color,
                            rangeStart,
                            rangeEnd
                    );
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        if (calendarView != null) {
            calendarView.notifyCalendarChanged();
        }
    }

    private void addHealthOccurrences(
            LocalDate startDate,
            int cycleValue,
            String cycleUnit,
            String label,
            int color,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        LocalDate occurrence = moveOccurrenceToRange(
                startDate, cycleValue, cycleUnit, rangeStart
        );

        while (!occurrence.isAfter(rangeEnd)) {
            healthEventsMap
                    .computeIfAbsent(occurrence, ignored -> new ArrayList<>())
                    .add(new CalendarEvent(label, color));
            occurrence = nextHealthOccurrence(occurrence, cycleValue, cycleUnit);
        }
    }

    private LocalDate moveOccurrenceToRange(
            LocalDate startDate,
            int cycleValue,
            String cycleUnit,
            LocalDate rangeStart
    ) {
        if (!startDate.isBefore(rangeStart)) {
            return startDate;
        }

        if ("일".equals(cycleUnit)) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, rangeStart);
            long cycles = (days + cycleValue - 1L) / cycleValue;
            return startDate.plusDays(cycles * cycleValue);
        }

        LocalDate occurrence = startDate;
        while (occurrence.isBefore(rangeStart)) {
            occurrence = nextHealthOccurrence(occurrence, cycleValue, cycleUnit);
        }
        return occurrence;
    }

    private LocalDate nextHealthOccurrence(
            LocalDate date,
            int cycleValue,
            String cycleUnit
    ) {
        if ("년".equals(cycleUnit)) {
            return date.plusYears(cycleValue);
        }
        if ("달".equals(cycleUnit)) {
            return date.plusMonths(cycleValue);
        }
        return date.plusDays(cycleValue);
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
                        String categoryId = document.getString("categoryId");
                        String category = document.getString("category");
                        String memo = document.getString("memo");

                        if (dateStr != null && title != null) {
                            LocalDate date = LocalDate.parse(dateStr);
                            if (!schedulesMap.containsKey(date)) {
                                schedulesMap.put(date, new ArrayList<>());
                            }
                            schedulesMap.get(date).add(new ScheduleItem(id, title, categoryId, category, memo));
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
        LinearLayout layoutCategoryChips = dialog.findViewById(R.id.layoutCategoryChips);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일");
        tvDialogDate.setText(date.format(formatter));

        final String[] editDocId = {null};
        final String[] selectedCategoryId = {null};

        // 🌟 [수정 포인트 1] 다이얼로그가 처음 열릴 때는 이전 뷰들을 싹 비우고 리스트 영역을 숨깁니다 (공간까지 완전 압축)
        if (layoutScheduleList != null) {
            layoutScheduleList.removeAllViews();
            layoutScheduleList.setVisibility(View.GONE);
        }

        List<ScheduleItem> todaySchedules = schedulesMap.get(date);

        // 🌟 [수정 포인트 2] 오늘 등록된 일정이 하나라도 '있을 때만' 목록 상자를 보여줍니다 (단계별 노출)
        if (todaySchedules != null && !todaySchedules.isEmpty() && layoutScheduleList != null) {
            layoutScheduleList.setVisibility(View.VISIBLE); // 일정이 존재하므로 숨겨진 리스트 영역 활성화!

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
                                    Toast.makeText(MainActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    loadSchedulesFromFirebase();
                                });
                    }
                });

                itemView.setOnClickListener(v -> {
                    layoutInputForm.setVisibility(View.VISIBLE);
                    editDocId[0] = item.id;
                    selectedCategoryId[0] = item.categoryId;
                    etCategory.setText(item.category);
                    etTitle.setText(item.title);
                    etMemo.setText(item.memo);
                    btnSave.setText("수정하기");
                });
                layoutScheduleList.addView(itemView);
            }
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).collection("categories")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        layoutCategoryChips.removeAllViews();
                        List<CategoryItem> serverCategories = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            CategoryItem cat = doc.toObject(CategoryItem.class);
                            cat.setId(doc.getId());
                            serverCategories.add(cat);
                        }

                        int totalSize = serverCategories.size();
                        int displayCount = Math.min(totalSize, 3);

                        for (int i = 0; i < displayCount; i++) {
                            CategoryItem cat = serverCategories.get(i);

                            View chipView = getLayoutInflater().inflate(R.layout.item_category, layoutCategoryChips, false);
                            View viewColorCircle = chipView.findViewById(R.id.viewColorBar);
                            TextView txtCategoryName = chipView.findViewById(R.id.txtCategoryName);

                            txtCategoryName.setText(cat.getName());

                            Drawable bgDrawable = viewColorCircle.getBackground();
                            if (bgDrawable != null && cat.getColorCode() != null) {
                                try {
                                    Drawable wrappedDrawable = DrawableCompat.wrap(bgDrawable.mutate());
                                    DrawableCompat.setTint(wrappedDrawable, Color.parseColor(cat.getColorCode()));
                                    viewColorCircle.setBackground(wrappedDrawable);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            params.setMargins(0, 0, 16, 0);
                            chipView.setLayoutParams(params);

                            chipView.setOnClickListener(v -> {
                                etCategory.setText(cat.getName());
                                selectedCategoryId[0] = cat.getId();

                                Drawable etBg = etCategory.getBackground();
                                if (etBg != null) {
                                    try {
                                        Drawable wrappedEtBg = DrawableCompat.wrap(etBg.mutate());
                                        DrawableCompat.setTint(wrappedEtBg, Color.parseColor(cat.getColorCode()));
                                        etCategory.setBackground(wrappedEtBg);
                                    } catch (Exception e) {
                                        etCategory.setBackgroundColor(Color.parseColor(cat.getColorCode()));
                                    }
                                } else {
                                    etCategory.setBackgroundColor(Color.parseColor(cat.getColorCode()));
                                }
                                etCategory.setTextColor(Color.WHITE);
                            });
                            layoutCategoryChips.addView(chipView);
                        }

                        if (totalSize > 0) {
                            Button moreBtn = new Button(MainActivity.this);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            moreBtn.setLayoutParams(params);
                            moreBtn.setText("+ etc");
                            moreBtn.setTextSize(12);
                            moreBtn.setBackgroundColor(Color.parseColor("#7F7F7F"));
                            moreBtn.setTextColor(Color.WHITE);

                            moreBtn.setOnClickListener(v -> {
                                String[] catNames = new String[totalSize + 1];
                                for (int k = 0; k < totalSize; k++) {
                                    catNames[k] = serverCategories.get(k).getName();
                                }
                                catNames[totalSize] = "새 카테고리 추가하기";

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("카테고리 선택")
                                        .setItems(catNames, (dialogInterface, which) -> {
                                            if (which == totalSize) {
                                                Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
                                                startActivity(intent);
                                                dialog.dismiss();
                                            } else {
                                                CategoryItem selectedCat = serverCategories.get(which);
                                                etCategory.setText(selectedCat.getName());
                                                selectedCategoryId[0] = selectedCat.getId();

                                                Drawable etBg = etCategory.getBackground();
                                                if (etBg != null) {
                                                    try {
                                                        Drawable wrappedEtBg = DrawableCompat.wrap(etBg.mutate());
                                                        DrawableCompat.setTint(wrappedEtBg, Color.parseColor(selectedCat.getColorCode()));
                                                        etCategory.setBackground(wrappedEtBg);
                                                    } catch (Exception e) {
                                                        etCategory.setBackgroundColor(Color.parseColor(selectedCat.getColorCode()));
                                                    }
                                                } else {
                                                    etCategory.setBackgroundColor(Color.parseColor(selectedCat.getColorCode()));
                                                }
                                                etCategory.setTextColor(Color.WHITE);
                                            }
                                        })
                                        .show();
                            });
                            layoutCategoryChips.addView(moreBtn);
                        }
                    });
        }

        btnAddSchedule.setOnClickListener(v -> {
            if (layoutInputForm.getVisibility() == View.GONE) {
                layoutInputForm.setVisibility(View.VISIBLE);
                editDocId[0] = null;
                selectedCategoryId[0] = null;
                etCategory.setText("");
                etCategory.setBackgroundColor(Color.parseColor("#F5F5F5"));
                etCategory.setTextColor(Color.BLACK);
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
                Toast.makeText(MainActivity.this, "일정을 입력해 주세요!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (user != null) {
                Map<String, Object> scheduleData = new HashMap<>();
                scheduleData.put("date", date.toString());
                scheduleData.put("categoryId", selectedCategoryId[0]);
                scheduleData.put("category", etCategory.getText().toString().trim());
                scheduleData.put("title", title);
                scheduleData.put("memo", etMemo.getText().toString().trim());
                scheduleData.put("timestamp", System.currentTimeMillis());

                if (editDocId[0] == null) {
                    db.collection("users").document(user.getUid()).collection("schedules")
                            .add(scheduleData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(MainActivity.this, "저장 완료!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadSchedulesFromFirebase();
                            });
                } else {
                    db.collection("users").document(user.getUid()).collection("schedules").document(editDocId[0])
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
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        int nameColor = ContextCompat.getColor(this, R.color.maincolor);
        int greetingColor = ContextCompat.getColor(this, R.color.namecolor);

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

    final class DayViewContainer extends ViewContainer {
        TextView tvDate;
        TextView tvEvent1, tvEvent2, tvEvent3, tvEvent4, tvEvent5, tvMore;
        CalendarDay day;

        public DayViewContainer(View view) {
            super(view);
            tvDate = view.findViewById(R.id.tvDate);
            tvEvent1 = view.findViewById(R.id.tvEvent1);
            tvEvent2 = view.findViewById(R.id.tvEvent2);
            tvEvent3 = view.findViewById(R.id.tvEvent3);
            tvEvent4 = view.findViewById(R.id.tvEvent4);
            tvEvent5 = view.findViewById(R.id.tvEvent5);
            tvMore = view.findViewById(R.id.tvMore);

            TextView[] allTextViews = {tvDate, tvEvent1, tvEvent2, tvEvent3, tvEvent4, tvEvent5, tvMore};
            for (TextView tv : allTextViews) {
                if (tv != null) {
                    tv.setClickable(false);
                    tv.setLongClickable(false);
                }
            }

            view.setOnClickListener(v -> {
                if (day != null && day.getPosition() == DayPosition.MonthDate) {
                    showScheduleDialog(day.getDate());
                }
            });

            view.setOnLongClickListener(v -> {
                if (day != null && day.getPosition() == DayPosition.MonthDate) {
                    openDiaryForDate(day.getDate());
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

        public MonthHeaderContainer(View view) {
            super(view);
            tvMonth = view.findViewById(R.id.tvMonth);
            btnPrev = view.findViewById(R.id.btnPrev);
            btnNext = view.findViewById(R.id.btnNext);
        }
    }

    private void openDiaryForDate(LocalDate date) {
        Intent intent = new Intent(MainActivity.this, DiaryDetailActivity.class);
        String formattedDate;

        try {
            java.time.format.DateTimeFormatter diaryFormatter =
                    java.time.format.DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN);
            formattedDate = date.format(diaryFormatter);
        } catch (Exception e) {
            formattedDate = date.toString();
        }

        intent.putExtra("date", formattedDate);
        intent.putExtra("content", findDiaryContent(formattedDate));
        startActivity(intent);
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
        } catch (Exception ignored) {
        }
        return "";
    }
}
