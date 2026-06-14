package com.example.hamlendar;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiaryListActivity extends AppCompatActivity {

    private static final String PREF_NAME = "diary_pref";
    private static final String KEY_DIARY_LIST = "diary_list";
    private static final String KEY_SUMMARY_PREFIX = "summary_";
    private static final String KEY_TIMETABLE_PREFIX = "timetable_";
    private static final Pattern DIARY_DATE_PATTERN =
            Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일");

    private TextView txtCurrentDate;
    private Button btnSelectDelete;
    private Button btnDeleteAll;
    private RecyclerView recyclerDiary;
    private final ArrayList<DiaryItem> diaryList = new ArrayList<>();
    private DiaryAdapter diaryAdapter;
    private boolean selectMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_list);

        recyclerDiary = findViewById(R.id.recyclerDiary);
        txtCurrentDate = findViewById(R.id.txtCurrentDate);
        btnSelectDelete = findViewById(R.id.btnSelectDelete);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        View fabAddDiary = findViewById(R.id.fabAddDiary);
        ImageView btnHealth = findViewById(R.id.img_diary_health);
        ImageView btnCal = findViewById(R.id.img_diary_cal);

        btnHealth.setOnClickListener(v ->
                startActivity(new Intent(DiaryListActivity.this, HealthActivity.class)));

        btnCal.setOnClickListener(v ->
                startActivity(new Intent(DiaryListActivity.this, MainActivity.class)));

        // 오늘 날짜 표시
        txtCurrentDate.setText(getTodayDate());
        txtCurrentDate.setOnClickListener(v -> showDiaryDatePicker());

        // RecyclerView 연결
        diaryAdapter = new DiaryAdapter();
        recyclerDiary.setLayoutManager(new LinearLayoutManager(this));
        recyclerDiary.setAdapter(diaryAdapter);

        // 위쪽 빈 공간이 크게 생기지 않도록 padding을 작게 둔다.
        recyclerDiary.setClipToPadding(false);
        recyclerDiary.setOverScrollMode(View.OVER_SCROLL_NEVER);
        recyclerDiary.setPadding(
                recyclerDiary.getPaddingLeft(),
                dpToPx(18),
                recyclerDiary.getPaddingRight(),
                dpToPx(88)
        );

        // 스크롤 가능한 양일 때만 가운데 카드 확대 효과 적용
        recyclerDiary.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateDiaryCardScale();
            }
        });

        // 새 일기 작성
        fabAddDiary.setOnClickListener(v -> openDiary(getTodayDate(), ""));

        // 선택 삭제 버튼: 처음 누르면 선택 모드, 선택 모드에서는 체크한 일기 삭제
        btnSelectDelete.setOnClickListener(v -> handleSelectDelete());

        // 평소에는 모두 삭제, 선택 모드에서는 선택 취소
        btnDeleteAll.setOnClickListener(v -> {
            if (selectMode) {
                clearSelectionMode();
            } else {
                confirmDeleteAll();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDiaryList();
        clearSelectionMode();
        recyclerDiary.post(this::updateDiaryCardScale);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private boolean canDiaryListScroll() {
        RecyclerView.LayoutManager manager = recyclerDiary.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager)) {
            return false;
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();
        int itemCount = diaryAdapter.getItemCount();

        if (itemCount == 0 || firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return false;
        }

        // 모든 일기가 화면에 보이면 두 번째 사진처럼 scale 없이 원래 크기로 둔다.
        return firstVisible > 0 || lastVisible < itemCount - 1;
    }

    private void updateDiaryCardScale() {
        if (!canDiaryListScroll()) {
            resetDiaryCardScale();
            return;
        }

        int recyclerCenterY = recyclerDiary.getHeight() / 2;
        int maxDistance = Math.max(recyclerCenterY, 1);

        for (int i = 0; i < recyclerDiary.getChildCount(); i++) {
            View child = recyclerDiary.getChildAt(i);
            int childCenterY = (child.getTop() + child.getBottom()) / 2;
            float distance = Math.abs(recyclerCenterY - childCenterY);
            float percent = Math.min(distance / maxDistance, 1f);

            float scale = 1.0f - (percent * 0.18f);
            float alpha = 1.0f - (percent * 0.35f);

            child.setScaleX(Math.max(scale, 0.82f));
            child.setScaleY(Math.max(scale, 0.82f));
            child.setAlpha(Math.max(alpha, 0.65f));
        }
    }

    private void resetDiaryCardScale() {
        for (int i = 0; i < recyclerDiary.getChildCount(); i++) {
            View child = recyclerDiary.getChildAt(i);
            child.setScaleX(1f);
            child.setScaleY(1f);
            child.setAlpha(1f);
        }
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
        return sdf.format(new Date());
    }

    private void showDiaryDatePicker() {
        Calendar selected = Calendar.getInstance();
        int currentPosition = findDiaryPosition(txtCurrentDate.getText().toString());
        if (currentPosition >= 0) {
            applyDateToCalendar(diaryList.get(currentPosition).date, selected);
        }

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    String pickedDate = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)
                            .format(picked.getTime());
                    txtCurrentDate.setText(pickedDate);

                    int position = findDiaryPosition(pickedDate);
                    if (position >= 0) {
                        scrollDiaryToCenter(position);
                    } else {
                        Toast.makeText(this, "해당 날짜에 작성된 일기가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                },
                selected.get(Calendar.YEAR),
                selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private int findDiaryPosition(String date) {
        long target = diaryDateSortValue(date);
        for (int i = 0; i < diaryList.size(); i++) {
            if (diaryDateSortValue(diaryList.get(i).date) == target) {
                return i;
            }
        }
        return -1;
    }

    private void scrollDiaryToCenter(int position) {
        RecyclerView.LayoutManager manager = recyclerDiary.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager)) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
        layoutManager.scrollToPositionWithOffset(position, recyclerDiary.getHeight() / 2);
        recyclerDiary.post(() -> {
            View itemView = layoutManager.findViewByPosition(position);
            if (itemView != null) {
                int itemCenter = (itemView.getTop() + itemView.getBottom()) / 2;
                recyclerDiary.scrollBy(0, itemCenter - recyclerDiary.getHeight() / 2);
            }
            updateDiaryCardScale();
        });
    }

    private void applyDateToCalendar(String date, Calendar calendar) {
        Matcher matcher = DIARY_DATE_PATTERN.matcher(date == null ? "" : date);
        if (matcher.find()) {
            calendar.set(Calendar.MONTH, Integer.parseInt(matcher.group(1)) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(2)));
        }
    }

    private long diaryDateSortValue(String date) {
        Matcher matcher = DIARY_DATE_PATTERN.matcher(date == null ? "" : date);
        if (!matcher.find()) return Long.MIN_VALUE;

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(
                Calendar.getInstance().get(Calendar.YEAR),
                Integer.parseInt(matcher.group(1)) - 1,
                Integer.parseInt(matcher.group(2))
        );
        return calendar.getTimeInMillis();
    }

    private void openDiary(String date, String content) {
        Intent intent = new Intent(DiaryListActivity.this, DiaryDetailActivity.class);
        intent.putExtra("date", date);
        intent.putExtra("content", content);
        startActivity(intent);
    }

    private void loadDiaryList() {
        diaryList.clear();

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_DIARY_LIST, "[]");

        try {
            JSONArray jsonArray = new JSONArray(json);

            // 최근에 쓴 일기가 위에 보이도록 역순으로 추가
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                diaryList.add(new DiaryItem(
                        obj.optString("date", ""),
                        obj.optString("content", "")
                ));
            }
            diaryList.sort(Comparator.comparingLong(
                    (DiaryItem item) -> diaryDateSortValue(item.date)
            ).reversed());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSelectDelete() {
        if (!selectMode) {
            if (diaryList.isEmpty()) {
                Toast.makeText(this, "삭제할 일기가 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            selectMode = true;
            btnSelectDelete.setText("삭제");
            btnDeleteAll.setText("취소");
            diaryAdapter.notifyDataSetChanged();
            recyclerDiary.post(this::updateDiaryCardScale);
            return;
        }

        confirmDeleteSelected();
    }

    private void confirmDeleteSelected() {
        int selectedCount = 0;
        for (DiaryItem item : diaryList) {
            if (item.selected) {
                selectedCount++;
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(this, "삭제할 일기를 체크하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("선택 삭제")
                .setMessage("체크한 일기를 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> deleteSelectedDiaries())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteSelectedDiaries() {
        Iterator<DiaryItem> iterator = diaryList.iterator();
        while (iterator.hasNext()) {
            DiaryItem item = iterator.next();
            if (item.selected) {
                removeDiaryExtras(item.date);
                iterator.remove();
            }
        }

        saveDiaryList();
        clearSelectionMode();
        Toast.makeText(this, "선택한 일기가 삭제되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteAll() {
        if (diaryList.isEmpty()) {
            Toast.makeText(this, "삭제할 일기가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("모두 삭제")
                .setMessage("모든 일기를 삭제할까요?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    for (DiaryItem item : diaryList) {
                        removeDiaryExtras(item.date);
                    }
                    diaryList.clear();
                    saveDiaryList();
                    clearSelectionMode();
                    Toast.makeText(this, "모든 일기가 삭제되었습니다", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void clearSelectionMode() {
        selectMode = false;
        btnSelectDelete.setText("선택 삭제");
        btnDeleteAll.setText("모두 삭제");
        for (DiaryItem item : diaryList) {
            item.selected = false;
        }
        diaryAdapter.notifyDataSetChanged();
        recyclerDiary.post(this::updateDiaryCardScale);
    }

    private void saveDiaryList() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();

        try {
            // SharedPreferences에는 원래 저장 순서로 다시 저장한다.
            for (DiaryItem item : diaryList) {
                JSONObject obj = new JSONObject();
                obj.put("date", item.date);
                obj.put("content", item.content);
                jsonArray.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        prefs.edit().putString(KEY_DIARY_LIST, jsonArray.toString()).apply();
    }

    private void removeDiaryExtras(String date) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .remove(KEY_SUMMARY_PREFIX + date)
                .remove(KEY_TIMETABLE_PREFIX + date)
                .apply();
    }

    static final class DiaryItem {
        private final String date;
        private final String content;
        private boolean selected;

        DiaryItem(String date, String content) {
            this.date = date;
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

            String preview = item.content;
            if (preview == null || preview.trim().isEmpty()) {
                preview = "(내용 없음)";
            }
            holder.txtDiaryPreview.setText(preview);

            holder.checkDelete.setVisibility(selectMode ? View.VISIBLE : View.GONE);
            holder.checkDelete.setOnCheckedChangeListener(null);
            holder.checkDelete.setChecked(item.selected);
            holder.checkDelete.setOnCheckedChangeListener((buttonView, isChecked) ->
                    item.selected = isChecked);

            holder.itemView.setOnClickListener(v -> {
                if (selectMode) {
                    item.selected = !item.selected;
                    notifyItemChanged(holder.getBindingAdapterPosition());
                } else {
                    openDiary(item.date, item.content);
                }
            });
        }

        @Override
        public int getItemCount() {
            return diaryList.size();
        }

        class DiaryViewHolder extends RecyclerView.ViewHolder {
            private final CheckBox checkDelete;
            private final TextView txtDiaryDate;
            private final TextView txtDiaryPreview;

            DiaryViewHolder(@NonNull View itemView) {
                super(itemView);
                checkDelete = itemView.findViewById(R.id.checkDelete);
                txtDiaryDate = itemView.findViewById(R.id.txtDiaryDate);
                txtDiaryPreview = itemView.findViewById(R.id.txtDiaryPreview);
            }
        }
    }
}
