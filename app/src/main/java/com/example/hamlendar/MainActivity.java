package com.example.hamlendar;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import androidx.core.graphics.drawable.DrawableCompat; // 🌟 필수 추가: 안전하게 색상을 입혀 크래시를 방지하는 도구

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

    // 🌟 일정을 통째로 보관할 클래스 (아이디, 제목, 카테고리, 메모)
    class ScheduleItem {
        String id;
        String title;
        String category;
        String memo;

        public ScheduleItem(String id, String title, String category, String memo) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.memo = memo;
        }
    }

    // 🔥 단순 String이 아니라, ScheduleItem 전체를 담아두는 맵

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

                if (day.getPosition() == DayPosition.MonthDate) {
                    container.tvDate.setText(String.valueOf(day.getDate().getDayOfMonth()));

                    List<ScheduleItem> todaySchedules = schedulesMap.get(day.getDate());

                    if (todaySchedules != null && !todaySchedules.isEmpty()) {
                        int size = todaySchedules.size();

                        if (size >= 1) container.tvEvent1.setText("• " + todaySchedules.get(0).title);
                        if (size >= 2) container.tvEvent2.setText("• " + todaySchedules.get(1).title);
                        if (size >= 3) container.tvEvent3.setText("• " + todaySchedules.get(2).title);
                        if (size >= 4) container.tvEvent4.setText("• " + todaySchedules.get(3).title);
                        if (size >= 5) container.tvEvent5.setText("• " + todaySchedules.get(4).title);

                        if (size > 5) {
                            container.tvMore.setText("+" + (size - 5));
                        }
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

    // =========================
    // 🔥 파이어베이스에서 내 일정 불러오기
    // =========================
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

    // =========================
    // 🔥 일정 다이얼로그 띄우기 (목록, 추가, 수정, 삭제)
    // =========================
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

        // [기존 일정 세팅 로직]
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
                                    Toast.makeText(MainActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
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

        // 🌟 [안전화 완료] 파이어베이스 카테고리 불러오기 및 실시간 칩 연동 로직
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).collection("categories")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        layoutCategoryChips.removeAllViews();
                        List<CategoryItem> serverCategories = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            serverCategories.add(doc.toObject(CategoryItem.class));
                        }

                        int totalSize = serverCategories.size();
                        int displayCount = Math.min(totalSize, 3);

                        // 상위 3개 카테고리 자동 칩 생성 (오류 우회형 동적 생성)
                        for (int i = 0; i < displayCount; i++) {
                            CategoryItem cat = serverCategories.get(i);

                            // 💡 버튼 대신 item_category.xml 도면 레이아웃을 인플레이트하여 일관성 보장 및 튕김 해결!
                            View chipView = getLayoutInflater().inflate(R.layout.item_category, layoutCategoryChips, false);
                            View viewColorCircle = chipView.findViewById(R.id.viewColorCircle);
                            TextView txtCategoryName = chipView.findViewById(R.id.txtCategoryName);

                            txtCategoryName.setText(cat.getName());

                            // DrawableCompat을 통해 안전하게 백그라운드 원형에 파이어베이스 색상 주입 🌟
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

                            // 칩 터치 시 타겟 입력창 데이터 동기화
                            chipView.setOnClickListener(v -> {
                                etCategory.setText(cat.getName());

                                // 입력 폼의 etCategory 배경색도 안전하게 변경하기 위한 로직
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

                        // 3개 이상일 때 혹은 목록 관리를 위한 [+ 더보기(etc)] 버튼 생성
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

        // [추가하기 및 저장 처리 버튼 로직]
        btnAddSchedule.setOnClickListener(v -> {
            if (layoutInputForm.getVisibility() == View.GONE) {
                layoutInputForm.setVisibility(View.VISIBLE);
                editDocId[0] = null;
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

    // =========================
    // ViewContainer 클래스들
    // =========================

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

        public MonthHeaderContainer(View view) {
            super(view);
            tvMonth = view.findViewById(R.id.tvMonth);
            btnPrev = view.findViewById(R.id.btnPrev);
            btnNext = view.findViewById(R.id.btnNext);
        }
    }

    // =========================
// 일기 화면 열기
// =========================
    private void openDiaryForDate(LocalDate date) {

        Intent intent =
                new Intent(MainActivity.this,
                        DiaryDetailActivity.class);

        // 선택한 날짜 전달
        intent.putExtra("selectedDate",
                date.toString());

        startActivity(intent);
    }
}