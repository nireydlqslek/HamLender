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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView nameTitle;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 🌟 일정을 통째로 보관할 클래스 (아이디, 제목, 카테고리, 메모)
    class ScheduleItem {
        String id;
        String title;
        String category; ///카테고리수정
        String memo;

        public ScheduleItem(String id, String title, String category, String memo) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.memo = memo;
        }
    }

    // 🔥 단순 String이 아니라, ScheduleItem 전체를 담아두는 맵으로 업그레이드!
    private Map<LocalDate, List<ScheduleItem>> schedulesMap = new HashMap<>();

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

        btnDiary.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DiaryListActivity.class)));

        setGreetingName();
        loadSchedulesFromFirebase();

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

                        // 객체 안의 'title'을 꺼내서 넣어줘요
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
                        String id = document.getId(); // 파이어베이스 고유 번호
                        String dateStr = document.getString("date");
                        String title = document.getString("title");
                        String category = document.getString("category");
                        String memo = document.getString("memo");

                        if (dateStr != null && title != null) {
                            LocalDate date = LocalDate.parse(dateStr);
                            if (!schedulesMap.containsKey(date)) {
                                schedulesMap.put(date, new ArrayList<>());
                            }
                            // 상세 정보를 전부 담아서 저장!
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일");
        tvDialogDate.setText(date.format(formatter));

        // 🌟 현재 수정 중인 문서의 ID를 기억하는 변수 (null이면 '새로 추가', 값이 있으면 '수정')
        final String[] editDocId = {null};

        // 1️⃣ 기존 일정 목록 띄우기 및 수정/삭제 기능
        layoutScheduleList.removeAllViews();
        List<ScheduleItem> todaySchedules = schedulesMap.get(date);

        if (todaySchedules != null) {
            for (ScheduleItem item : todaySchedules) {
                // 아까 만드신 item_schedule.xml을 한 줄씩 불러와서 채워요
                View itemView = getLayoutInflater().inflate(R.layout.item_schedule, null);
                TextView tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
                ImageView ivDelete = itemView.findViewById(R.id.ivDelete);

                tvItemTitle.setText(item.title);

                // 🔥 삭제 버튼을 눌렀을 때
                ivDelete.setOnClickListener(v -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        db.collection("users").document(user.getUid())
                                .collection("schedules").document(item.id)
                                .delete() // 파이어베이스에서 완전 삭제!
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MainActivity.this, "삭제되었습니다! 🗑️", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss(); // 닫고 새로고침
                                    loadSchedulesFromFirebase();
                                });
                    }
                });

                // 🔥 일정을 (글씨 부분을) 클릭했을 때 -> 수정 모드 돌입!
                itemView.setOnClickListener(v -> {
                    layoutInputForm.setVisibility(View.VISIBLE);
                    editDocId[0] = item.id; // 수정할 문서 번호 기억하기
                    etCategory.setText(item.category);
                    etTitle.setText(item.title);
                    etMemo.setText(item.memo);
                    btnSave.setText("수정하기");
                });

                layoutScheduleList.addView(itemView);
            }
        }

        // 2️⃣ '새로운 일정 추가하기' 버튼 클릭 시
        btnAddSchedule.setOnClickListener(v -> {
            if (layoutInputForm.getVisibility() == View.GONE) {
                layoutInputForm.setVisibility(View.VISIBLE);
                // 새로 추가할 땐 폼 비워주기
                editDocId[0] = null;
                etCategory.setText("");
                etTitle.setText("");
                etMemo.setText("");
                btnSave.setText("저장하기");
            } else {
                layoutInputForm.setVisibility(View.GONE);
            }
        });

        // 3️⃣ 저장 or 수정 버튼 클릭 시
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
                    // 새로 저장하는 경우 (Add)
                    db.collection("users").document(user.getUid())
                            .collection("schedules")
                            .add(scheduleData)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(MainActivity.this, "저장 성공! 🎉", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadSchedulesFromFirebase(); // 캘린더 새로고침
                            });
                } else {
                    // 기존 일정을 수정하는 경우 (Update)
                    db.collection("users").document(user.getUid())
                            .collection("schedules").document(editDocId[0])
                            .update(scheduleData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "수정 완료! ✨", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                loadSchedulesFromFirebase(); // 캘린더 새로고침
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

    class DayViewContainer extends ViewContainer {
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
                    showScheduleDialog(day.getDate());
                }
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
}