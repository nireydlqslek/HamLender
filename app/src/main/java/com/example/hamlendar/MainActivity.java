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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        boolean isCompleted;
        String categoryColor;

        public ScheduleItem(String id, String title, String categoryId, String category, String memo, boolean isCompleted, String categoryColor) {
            this.id = id;
            this.title = title;
            this.categoryId = categoryId;
            this.category = category;
            this.memo = memo;
            this.isCompleted = isCompleted;
            this.categoryColor = categoryColor;
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

        TextView aiMessage = findViewById(R.id.aiMessage);

        // 3. "햄햄" 텍스트뷰를 클릭했을 때의 동작
        aiMessage.setOnClickListener(v -> {
            // 로딩 문구 표시 (클릭하자마자 반응 주기)
            aiMessage.setText("들어 있는 게 많다햄! 기다려 보라햄...");

            // 실제 회원 이름 가져오기 (nameTitle 텍스트에서 추출하거나 기존 이름 변수 사용)
            // 만약 "사용자님, 안녕하세요!" 형태라면 이름 부분만 추출합니다.
            String actualName = nameTitle.getText().toString().replace(", 안녕하세요!", "");

            // 다른 화면으로 이동하지 않고, 이 자리에서 바로 Gemini 호출!
            GeminiManager.INSTANCE.generateEmpathyMessage(actualName, new GeminiManager.SummaryCallback() {
                @Override
                public void onSuccess(@NonNull String summary) {
                    // 대답을 성공적으로 받아오면 메인 스레드에서 글자만 쏙 바꿉니다.
                    runOnUiThread(() -> aiMessage.setText(summary));
                }

                @Override
                public void onError(@NonNull String error) {
                    // 에러 발생 시 처리
                    runOnUiThread(() -> aiMessage.setText("다시 한 번 누르자햄!"));
                }
            });
        });

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

        YearMonth startMonth = currentMonth.minusMonths(100);
        YearMonth endMonth = currentMonth.plusMonths(100);
        DayOfWeek firstDayOfWeek = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();

        calendarView.setup(startMonth, endMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);


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

                        // 🌟 1. 파이어베이스에서 완료 여부 읽어오기 (기본값 false)
                        boolean isCompleted = document.getBoolean("isCompleted") != null
                                && document.getBoolean("isCompleted");

                        // 🌟 2. 파이어베이스에서 카테고리 색상 코드 읽어오기 (기본값 연회색)
                        String categoryColor = document.getString("categoryColor");
                        if (categoryColor == null || categoryColor.isEmpty()) {
                            categoryColor = "#D3D3D3";
                        }

                        if (dateStr != null && title != null) {
                            LocalDate date = LocalDate.parse(dateStr);
                            if (!schedulesMap.containsKey(date)) {
                                schedulesMap.put(date, new ArrayList<>());
                            }

                            // 🌟 3. 생성자 파라미터 순서에 정확히 맞춰서 주입 (id, title, categoryId, category, memo, 완료여부, 색상)
                            schedulesMap.get(date).add(new ScheduleItem(id, title, categoryId, category, memo, isCompleted, categoryColor));
                        }
                    }
                    if (calendarView != null) {
                        calendarView.notifyCalendarChanged();
                    }
                });
    }


    // 🌟 칩 토글 상태와 자동 저장을 유기적으로 바인딩하기 위해 showScheduleDialog 메서드를 전면 수정합니다.
    private void showScheduleDialog(LocalDate date) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_schedule);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 1. 뷰 바인딩 진행
        TextView tvDialogDate = dialog.findViewById(R.id.tvDialogDate);
        ImageView btnDialogClose = dialog.findViewById(R.id.btnDialogClose);
        LinearLayout layoutScheduleList = dialog.findViewById(R.id.layoutScheduleList);
        Button btnAddSchedule = dialog.findViewById(R.id.btnAddSchedule);

        LinearLayout layoutInputForm = dialog.findViewById(R.id.layoutInputForm);
        ImageView btnCategoryMenu = dialog.findViewById(R.id.btnCategoryMenu);
        LinearLayout layoutCategoryChips = dialog.findViewById(R.id.layoutCategoryChips);
        EditText etCategory = dialog.findViewById(R.id.etCategory);
        EditText etTitle = dialog.findViewById(R.id.etTitle);
        ImageView btnDeleteMemo = dialog.findViewById(R.id.btnDeleteMemo);
        EditText etMemo = dialog.findViewById(R.id.etMemo);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        // 상단 날짜 포맷 세팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일");
        tvDialogDate.setText(date.format(formatter));

        // 우측 상단 X 버튼 누르면 다이얼로그 닫기
        if (btnDialogClose != null) {
            btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        }

        // 🌟 추적 변수 선언 (순서 및 중복 정리 완료)
        final String[] editDocId = {null};
        final String[] selectedCategoryId = {null};
        final String[] selectedCategoryColor = {"#D3D3D3"}; // 기본 컬러 값 세팅
        final View[] currentSelectedChip = {null}; // 칩 단일 선택 상태 추적용

        // 2. 초기 상태 정의: 이전 항목 뷰를 비우고 기본적으로 리스트 공간을 숨깁니다.
        if (layoutScheduleList != null) {
            layoutScheduleList.removeAllViews();
            layoutScheduleList.setVisibility(View.GONE);
        }

        List<ScheduleItem> todaySchedules = schedulesMap.get(date);

        // 3. 오늘 등록된 일정이 '있을 때만' 목록 레이아웃을 활성화하여 보여줍니다.
        if (todaySchedules != null && !todaySchedules.isEmpty() && layoutScheduleList != null) {
            layoutScheduleList.setVisibility(View.VISIBLE);

            for (ScheduleItem item : todaySchedules) {
                View itemView = getLayoutInflater().inflate(R.layout.item_schedule, null);
                View rootScheduleItem = itemView.findViewById(R.id.rootScheduleItem);
                TextView tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
                ImageView ivComplete = itemView.findViewById(R.id.ivComplete);
                View viewCategoryBar = itemView.findViewById(R.id.viewCategoryBar);

                if (tvItemTitle != null) tvItemTitle.setText(item.title);

                // 🌟 카테고리 색상 동적 바인딩
                if (viewCategoryBar != null && item.categoryColor != null) {
                    String colorUpper = item.categoryColor.toUpperCase();
                    int colorResId;

                    switch (colorUpper) {
                        case "RED": colorResId = R.color.cat_red_dark; break;
                        case "ORANGE": colorResId = R.color.cat_orange_dark; break;
                        case "YELLOW": colorResId = R.color.cat_yellow_dark; break;
                        case "GREEN": colorResId = R.color.cat_green_dark; break;
                        case "BLUE": colorResId = R.color.cat_blue_dark; break;
                        case "PURPLE": colorResId = R.color.cat_purple_dark; break;
                        case "PINK": colorResId = R.color.cat_pink_dark; break;
                        default: colorResId = R.color.cat_grey_dark; break;
                    }

                    viewCategoryBar.setBackgroundColor(ContextCompat.getColor(MainActivity.this, colorResId));
                }

                // 초기 완료 상태 표시 및 투명도 20% 흐림 처리
                if (item.isCompleted) {
                    if (ivComplete != null) ivComplete.setImageResource(R.drawable.circle_white_filled);
                    if (rootScheduleItem != null) rootScheduleItem.setAlpha(0.2f);
                } else {
                    if (ivComplete != null) ivComplete.setImageResource(R.drawable.circle_white);
                    if (rootScheduleItem != null) rootScheduleItem.setAlpha(1.0f);
                }

                // 원형 완료 버튼 클릭 리스너 (투명도 토글 및 파이어베이스 상태 저장)
                if (ivComplete != null) {
                    ivComplete.setOnClickListener(v -> {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            boolean nextState = !item.isCompleted;

                            db.collection("users").document(currentUser.getUid())
                                    .collection("schedules").document(item.id)
                                    .update("isCompleted", nextState)
                                    .addOnSuccessListener(aVoid -> {
                                        item.isCompleted = nextState;
                                        if (nextState) {
                                            ivComplete.setImageResource(R.drawable.circle_white_filled);
                                            if (rootScheduleItem != null) rootScheduleItem.setAlpha(0.2f);
                                        } else {
                                            ivComplete.setImageResource(R.drawable.circle_white);
                                            if (rootScheduleItem != null) rootScheduleItem.setAlpha(1.0f);
                                        }
                                        loadSchedulesFromFirebase();
                                    });
                        }
                    });
                }

                // 리스트 아이템 클릭 시 수정 폼 열기
                itemView.setOnClickListener(v -> {
                    if (layoutInputForm != null) layoutInputForm.setVisibility(View.VISIBLE);
                    if (layoutScheduleList != null) layoutScheduleList.setVisibility(View.GONE);
                    if (btnAddSchedule != null) btnAddSchedule.setVisibility(View.GONE);

                    editDocId[0] = item.id;
                    selectedCategoryId[0] = item.categoryId;
                    selectedCategoryColor[0] = item.categoryColor; // 수정 모드 진입 시 컬러 기존값 백업
                    if (etCategory != null) etCategory.setText(item.category);
                    if (etTitle != null) etTitle.setText(item.title);
                    if (etMemo != null) etMemo.setText(item.memo);
                    if (btnSave != null) btnSave.setText("수정하기");
                });

                layoutScheduleList.addView(itemView);
            }
        }

        // 4. 파이어베이스에서 카테고리 로드 후 가로 칩 목록 구성하기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && layoutCategoryChips != null) {
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

                        Collections.sort(serverCategories, (c1, c2) -> {
                            return Integer.compare(c1.getIndex(), c2.getIndex());
                        });

                        for (CategoryItem cat : serverCategories) {
                            View chipView = getLayoutInflater().inflate(R.layout.item_dialog_category_chip, layoutCategoryChips, false);
                            LinearLayout layoutChipRoot = chipView.findViewById(R.id.layoutChipRoot);
                            View viewChipColorBar = chipView.findViewById(R.id.viewChipColorBar);
                            TextView txtChipName = chipView.findViewById(R.id.txtChipName);
                            ImageView imgChipCheck = chipView.findViewById(R.id.imgChipCheck);

                            if (txtChipName != null) txtChipName.setText(cat.getName());

                            String colorType = cat.getColorCode() != null ? cat.getColorCode().toUpperCase() : "GREY";
                            int darkRes, lightRes;
                            switch (colorType) {
                                case "RED": darkRes = R.color.cat_red_dark; lightRes = R.color.cat_red_light; break;
                                case "ORANGE": darkRes = R.color.cat_orange_dark; lightRes = R.color.cat_orange_light; break;
                                case "YELLOW": darkRes = R.color.cat_yellow_dark; lightRes = R.color.cat_yellow_light; break;
                                case "GREEN": darkRes = R.color.cat_green_dark; lightRes = R.color.cat_green_light; break;
                                case "BLUE": darkRes = R.color.cat_blue_dark; lightRes = R.color.cat_blue_light; break;
                                case "PURPLE": darkRes = R.color.cat_purple_dark; lightRes = R.color.cat_purple_light; break;
                                case "PINK": darkRes = R.color.cat_pink_dark; lightRes = R.color.cat_pink_light; break;
                                default: darkRes = R.color.cat_grey_dark; lightRes = R.color.cat_grey_light; break;
                            }

                            if (viewChipColorBar != null) {
                                viewChipColorBar.setBackgroundColor(ContextCompat.getColor(this, darkRes));
                            }
                            if (layoutChipRoot != null && layoutChipRoot.getBackground() != null) {
                                Drawable wrapped = DrawableCompat.wrap(layoutChipRoot.getBackground().mutate());
                                DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, lightRes));
                                layoutChipRoot.setBackground(wrapped);
                            }

                            if (imgChipCheck != null) {
                                imgChipCheck.setImageResource(R.drawable.circle_white);
                            }

                            // 🌟 칩 클릭 시 데이터 매핑 및 컬러 저장 로직 탑재
                            if (layoutChipRoot != null) {
                                layoutChipRoot.setOnClickListener(v -> {
                                    if (currentSelectedChip[0] != null && currentSelectedChip[0] != layoutChipRoot) {
                                        ImageView prevCheck = currentSelectedChip[0].findViewById(R.id.imgChipCheck);
                                        if (prevCheck != null) prevCheck.setImageResource(R.drawable.circle_white);
                                    }

                                    if (cat.getId().equals(selectedCategoryId[0])) {
                                        if (imgChipCheck != null) imgChipCheck.setImageResource(R.drawable.circle_white);
                                        selectedCategoryId[0] = null;
                                        selectedCategoryColor[0] = "#D3D3D3"; // 해제 시 기본 색상으로 롤백
                                        if (etCategory != null) etCategory.setText("");
                                        currentSelectedChip[0] = null;
                                    } else {
                                        if (imgChipCheck != null) imgChipCheck.setImageResource(R.drawable.circle_white_filled);
                                        selectedCategoryId[0] = cat.getId();
                                        selectedCategoryColor[0] = cat.getColorCode(); // 🌟 선택된 카테고리의 헥사 색상 코드 실시간 저장
                                        if (etCategory != null) etCategory.setText(cat.getName());
                                        currentSelectedChip[0] = layoutChipRoot;
                                    }
                                });
                            }
                            layoutCategoryChips.addView(chipView);
                        }
                    }).addOnFailureListener(e -> {
                        // 🌟 혹시 정렬(orderBy) 필드가 파이어베이스 색인(Index) 문제로 에러가 날 때를 대비한 예외 처리
                        Log.e("Firestore", "카테고리 정렬 로드 실패: " + e.getMessage());
                    });

        }

        // 5. 카테고리 설정 이동
        if (btnCategoryMenu != null) {
            btnCategoryMenu.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
                startActivity(intent);
                dialog.dismiss();
            });
        }

        // 6. 메모 비우기
        if (btnDeleteMemo != null && etMemo != null) {
            btnDeleteMemo.setOnClickListener(v -> {
                etMemo.setText("");
                Toast.makeText(MainActivity.this, "메모가 초기화되었습니다.", Toast.LENGTH_SHORT).show();
            });
        }

        // 메모 변경 체커
        if (etMemo != null) {
            etMemo.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        // 7. '+ 일정 추가' 버튼 동선 연결
        if (btnAddSchedule != null) {
            btnAddSchedule.setOnClickListener(v -> {
                if (layoutInputForm != null && layoutInputForm.getVisibility() == View.GONE) {
                    layoutInputForm.setVisibility(View.VISIBLE);
                    if (layoutScheduleList != null) layoutScheduleList.setVisibility(View.GONE);
                    btnAddSchedule.setVisibility(View.GONE);

                    editDocId[0] = null;
                    selectedCategoryId[0] = null;
                    selectedCategoryColor[0] = "#D3D3D3";
                    currentSelectedChip[0] = null;
                    if (etCategory != null) etCategory.setText("");
                    if (etTitle != null) etTitle.setText("");
                    if (etMemo != null) etMemo.setText("");
                    if (btnSave != null) btnSave.setText("저장하기");
                }
            });
        }

        // 8. 최종 완료 저장하기 버튼 리스너 (선택된 카테고리 색상 데이터 함께 전달)
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String title = etTitle != null ? etTitle.getText().toString().trim() : "";
                if (title.isEmpty()) {
                    Toast.makeText(MainActivity.this, "일정을 입력해 주세요!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (user != null) {
                    Map<String, Object> scheduleData = new HashMap<>();
                    scheduleData.put("date", date.toString());
                    scheduleData.put("categoryId", selectedCategoryId[0]);
                    scheduleData.put("category", etCategory != null ? etCategory.getText().toString().trim() : "");
                    scheduleData.put("categoryColor", selectedCategoryColor[0]); // 🌟 검증된 색상 코드 업로드 추가!
                    scheduleData.put("title", title);
                    scheduleData.put("memo", etMemo != null ? etMemo.getText().toString().trim() : "");
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
        }

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
