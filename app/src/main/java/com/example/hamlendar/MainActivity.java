package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView nameTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        nameTitle = findViewById(R.id.nameTitle);
        ImageView settingIcon = findViewById(R.id.setting_icon);
        ImageView btnDiary = findViewById(R.id.img_main_diary);
        calendarView = findViewById(R.id.calendarView);

        // 회원가입 때 저장한 이름으로 인사말 표시
        setGreetingName();

        // 설정 아이콘 클릭 -> 설정 화면으로 이동
        settingIcon.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingActivity.class)));

        // 하단 일기 아이콘 클릭 -> 일기 목록 화면으로 이동
        btnDiary.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DiaryListActivity.class)));

        // 달력 모양과 요일 색상 설정
        setupCalendar();

        // 날짜 클릭 -> 해당 날짜 일정 추가 다이얼로그 표시
        calendarView.setOnDateChangedListener((widget, date, selected) ->
                showAddScheduleDialog(formatDate(date)));

        // 상태바/내비게이션바 영역과 화면 내용이 겹치지 않도록 여백 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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

        // 이름 부분만 다른 색으로 표시
        spannableGreeting.setSpan(
                new ForegroundColorSpan(nameColor),
                0,
                userName.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // 이름 뒤의 인사말은 기존 메인 컬러로 표시
        spannableGreeting.setSpan(
                new ForegroundColorSpan(greetingColor),
                userName.length(),
                greeting.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        nameTitle.setText(spannableGreeting);
    }

    private void setupCalendar() {
        // 달력 날짜 칸 높이와 글자 스타일 설정
        calendarView.setTileHeightDp(56);
        calendarView.setDateTextAppearance(R.style.CalendarDateText);
        calendarView.setWeekDayTextAppearance(R.style.CalendarWeekText);

        // 요일 이름을 한글로 표시
        calendarView.setWeekDayFormatter(dayOfWeek -> {
            String[] week = {"일", "월", "화", "수", "목", "금", "토"};
            return week[dayOfWeek - 1];
        });

        // 모든 날짜 칸에 흰색 배경 적용
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return true;
            }

            @Override
            public void decorate(DayViewFacade view) {
                GradientDrawable drawable = new GradientDrawable();
                drawable.setColor(Color.WHITE);
                drawable.setCornerRadius(8);
                view.setBackgroundDrawable(drawable);
                view.addSpan(new android.text.style.RelativeSizeSpan(0.85f));
            }
        });

        // 일요일은 빨간색, 토요일은 파란색으로 표시
        calendarView.addDecorator(new DayOfWeekDecorator(Calendar.SUNDAY, Color.RED));
        calendarView.addDecorator(new DayOfWeekDecorator(Calendar.SATURDAY, Color.BLUE));
    }

    private String formatDate(CalendarDay date) {
        // 선택한 날짜를 저장하기 쉬운 yyyy-MM-dd 형태로 변환
        return String.format(Locale.US, "%04d-%02d-%02d",
                date.getYear(), date.getMonth() + 1, date.getDay());
    }

    private void showAddScheduleDialog(String date) {
        // 일정 내용을 입력받는 다이얼로그 생성
        EditText editText = new EditText(this);
        editText.setHint("일정을 입력하세요");

        new AlertDialog.Builder(this)
                .setTitle(date + " 일정 추가")
                .setView(editText)
                .setPositiveButton("저장", (dialog, which) -> {
                    String schedule = editText.getText().toString().trim();
                    if (TextUtils.isEmpty(schedule)) {
                        Toast.makeText(this, "일정이 비어 있습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveSchedule(date, schedule);
                    Toast.makeText(this, "일정 저장 완료", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void saveSchedule(String date, String schedule) {
        // 날짜를 key로 사용해서 일정 내용 저장
        SharedPreferences prefs = getSharedPreferences("schedule", MODE_PRIVATE);
        prefs.edit().putString(date, schedule).apply();
    }

    private static class DayOfWeekDecorator implements DayViewDecorator {
        private final int dayOfWeek;
        private final int color;

        DayOfWeekDecorator(int dayOfWeek, int color) {
            this.dayOfWeek = dayOfWeek;
            this.color = color;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            // 날짜가 지정한 요일인지 확인
            Calendar calendar = Calendar.getInstance();
            calendar.set(day.getYear(), day.getMonth(), day.getDay());
            return calendar.get(Calendar.DAY_OF_WEEK) == dayOfWeek;
        }

        @Override
        public void decorate(DayViewFacade view) {
            // 해당 요일 날짜 글자색 변경
            view.addSpan(new android.text.style.ForegroundColorSpan(color));
        }
    }
}
