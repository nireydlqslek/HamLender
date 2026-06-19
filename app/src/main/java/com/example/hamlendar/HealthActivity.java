package com.example.hamlendar;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class HealthActivity extends AppCompatActivity {

    private static final String PREF_NAME = "health_pref";
    private static final String KEY_HEALTH_LIST = "health_list";
    private static final int MAX_SECTION_ITEMS = 6;

    private enum DeleteTarget {
        NONE, HEALTH, ROUTINE
    }

    private final ArrayList<HealthItem> healthItems = new ArrayList<>();
    private final ArrayList<HealthItem> routineItems = new ArrayList<>();
    private final String[] categoryNames = {
            "병원", "관리", "검진", "교체", "월경", "복용"
    };
    private final int[] categoryColors = {
            Color.rgb(239, 248, 232),
            Color.rgb(252, 246, 237),
            Color.rgb(232, 246, 255),
            Color.rgb(253, 253, 216),
            Color.rgb(255, 238, 238),
            Color.rgb(244, 241, 250)
    };

    private HealthAdapter healthAdapter;
    private HealthAdapter routineAdapter;
    private DeleteTarget deleteTarget = DeleteTarget.NONE;
    private Button btnDeleteHealth;
    private Button btnConfirmHealth;
    private Button btnCancelHealth;
    private Button btnDeleteRoutine;
    private Button btnConfirmRoutine;
    private Button btnCancelRoutine;
    private LinearLayout sectionHealth;
    private LinearLayout sectionRoutine;
    private RecyclerView recyclerHealth;
    private RecyclerView recyclerRoutine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnHealth = findViewById(R.id.img_health_health);
        ImageView btnCalendar = findViewById(R.id.img_health_cal);
        ImageView btnDiary = findViewById(R.id.img_health_diary);
        btnDeleteHealth = findViewById(R.id.btnDeleteHealth);
        btnConfirmHealth = findViewById(R.id.btnConfirmHealth);
        btnCancelHealth = findViewById(R.id.btnCancelHealth);
        btnDeleteRoutine = findViewById(R.id.btnDeleteRoutine);
        btnConfirmRoutine = findViewById(R.id.btnConfirmRoutine);
        btnCancelRoutine = findViewById(R.id.btnCancelRoutine);
        sectionHealth = findViewById(R.id.sectionHealth);
        sectionRoutine = findViewById(R.id.sectionRoutine);
        recyclerHealth = findViewById(R.id.recyclerHealth);
        recyclerRoutine = findViewById(R.id.recyclerRoutine);

        btnBack.setOnClickListener(v -> finish());
        btnHealth.setOnClickListener(v -> recyclerHealth.smoothScrollToPosition(0));
        btnCalendar.setOnClickListener(v ->
                startActivity(new Intent(HealthActivity.this, MainActivity.class)));
        btnDiary.setOnClickListener(v ->
                startActivity(new Intent(HealthActivity.this, DiaryListActivity.class)));
        btnDeleteHealth.setOnClickListener(v -> enterDeleteMode(DeleteTarget.HEALTH));
        btnConfirmHealth.setOnClickListener(v -> confirmDeleteSelected(DeleteTarget.HEALTH));
        btnCancelHealth.setOnClickListener(v -> exitDeleteMode());
        btnDeleteRoutine.setOnClickListener(v -> enterDeleteMode(DeleteTarget.ROUTINE));
        btnConfirmRoutine.setOnClickListener(v -> confirmDeleteSelected(DeleteTarget.ROUTINE));
        btnCancelRoutine.setOnClickListener(v -> exitDeleteMode());

        healthAdapter = new HealthAdapter(healthItems, DeleteTarget.HEALTH);
        routineAdapter = new HealthAdapter(routineItems, DeleteTarget.ROUTINE);

        recyclerHealth.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerRoutine.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerHealth.setAdapter(healthAdapter);
        recyclerRoutine.setAdapter(routineAdapter);

        loadItems();
    }

    private void loadItems() {
        healthItems.clear();
        routineItems.clear();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_HEALTH_LIST, "[]");
        boolean migratedCategory = false;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                HealthItem item = HealthItem.fromJson(array.getJSONObject(i));
                if ("영양제".equals(item.category)) {
                    item.category = "복용";
                    item.color = categoryColors[5];
                    migratedCategory = true;
                }
                if (item.isRoutine()) {
                    routineItems.add(item);
                } else {
                    healthItems.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (migratedCategory) {
            saveItems();
        }
        refreshLists();
    }

    private void saveItems() {
        JSONArray array = new JSONArray();
        try {
            for (HealthItem item : healthItems) {
                array.put(item.toJson());
            }
            for (HealthItem item : routineItems) {
                array.put(item.toJson());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_HEALTH_LIST, array.toString())
                .apply();
    }

    private void refreshLists() {
        updateDeleteButtons();
        healthAdapter.notifyDataSetChanged();
        routineAdapter.notifyDataSetChanged();
        updateSectionHeights();
    }

    private void updateSectionHeights() {
        updateSectionHeight(
                sectionHealth,
                recyclerHealth,
                healthAdapter.getItemCount()
        );
        updateSectionHeight(
                sectionRoutine,
                recyclerRoutine,
                routineAdapter.getItemCount()
        );
    }

    private void updateSectionHeight(
            LinearLayout sectionView,
            RecyclerView recyclerView,
            int visibleSlots
    ) {
        int rowCount = Math.max(1, (visibleSlots + 1) / 2);
        int recyclerHeight = rowCount * 100;
        int sectionHeight = recyclerHeight + 28;

        ViewGroup.LayoutParams recyclerParams = recyclerView.getLayoutParams();
        recyclerParams.height = dp(recyclerHeight);
        recyclerView.setLayoutParams(recyclerParams);

        sectionView.setMinimumHeight(dp(sectionHeight));
        recyclerView.requestLayout();
        sectionView.requestLayout();
    }

    private void updateDeleteButtons() {
        boolean deletingHealth = deleteTarget == DeleteTarget.HEALTH;
        boolean deletingRoutine = deleteTarget == DeleteTarget.ROUTINE;
        btnDeleteHealth.setVisibility(deletingHealth ? View.GONE : View.VISIBLE);
        btnConfirmHealth.setVisibility(deletingHealth ? View.VISIBLE : View.GONE);
        btnCancelHealth.setVisibility(deletingHealth ? View.VISIBLE : View.GONE);
        btnDeleteRoutine.setVisibility(deletingRoutine ? View.GONE : View.VISIBLE);
        btnConfirmRoutine.setVisibility(deletingRoutine ? View.VISIBLE : View.GONE);
        btnCancelRoutine.setVisibility(deletingRoutine ? View.VISIBLE : View.GONE);
    }

    private void enterDeleteMode(DeleteTarget target) {
        ArrayList<HealthItem> targetItems =
                target == DeleteTarget.HEALTH ? healthItems : routineItems;
        if (targetItems.isEmpty()) {
            Toast.makeText(this, "삭제할 일정이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        clearAllSelections();
        deleteTarget = target;
        refreshLists();
    }

    private void confirmDeleteSelected(DeleteTarget target) {
        if (deleteTarget != target) {
            return;
        }

        ArrayList<HealthItem> targetItems =
                target == DeleteTarget.HEALTH ? healthItems : routineItems;
        int selectedCount = 0;
        for (HealthItem item : targetItems) {
            if (item.selected) {
                selectedCount++;
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(this, "삭제할 항목을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Iterator<HealthItem> iterator = targetItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().selected) {
                iterator.remove();
            }
        }

        saveItems();
        exitDeleteMode();
        Toast.makeText(this, "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void exitDeleteMode() {
        deleteTarget = DeleteTarget.NONE;
        clearAllSelections();
        refreshLists();
    }

    private void clearAllSelections() {
        for (HealthItem item : healthItems) {
            item.selected = false;
        }
        for (HealthItem item : routineItems) {
            item.selected = false;
        }
    }

    private void showEditDialog(HealthItem editingItem) {
        boolean isEdit = editingItem != null;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(8), dp(22), dp(6));

        TextView dialogTitle = new TextView(this);
        dialogTitle.setText(isEdit ? "수정" : "추가");
        dialogTitle.setTextSize(18);
        dialogTitle.setTextColor(Color.BLACK);
        dialogTitle.setTypeface(null, Typeface.BOLD);
        root.addView(dialogTitle);

        LinearLayout categoryGrid = new LinearLayout(this);
        categoryGrid.setOrientation(LinearLayout.VERTICAL);
        categoryGrid.setPadding(0, dp(16), 0, dp(8));
        root.addView(categoryGrid);

        final String[] selectedCategory = {
                isEdit ? editingItem.category : categoryNames[0]
        };
        final int[] selectedColor = {
                isEdit ? editingItem.color : categoryColors[0]
        };
        ArrayList<CheckBox> categoryChecks = new ArrayList<>();

        EditText editContent = makeEditText("내용");
        editContent.setText(
                isEdit && !TextUtils.isEmpty(editingItem.content)
                        ? editingItem.content
                        : isEdit ? editingItem.title : ""
        );

        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            categoryGrid.addView(line);

            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (index >= categoryNames.length) {
                    break;
                }

                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(categoryNames[index]);
                checkBox.setTextSize(12);
                checkBox.setBackground(
                        makeRound(categoryColors[index], 8, Color.TRANSPARENT)
                );
                checkBox.setPadding(dp(8), 0, dp(8), 0);
                checkBox.setChecked(categoryNames[index].equals(selectedCategory[0]));

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(0, dp(38), 1);
                params.setMargins(dp(4), dp(4), dp(4), dp(4));
                line.addView(checkBox, params);
                categoryChecks.add(checkBox);

                final int categoryIndex = index;
                checkBox.setOnClickListener(v -> {
                    selectedCategory[0] = categoryNames[categoryIndex];
                    selectedColor[0] = categoryColors[categoryIndex];
                    for (CheckBox box : categoryChecks) {
                        box.setChecked(box == checkBox);
                    }
                    updateMenstruationFields(
                            selectedCategory[0], editContent
                    );
                });
            }
        }

        root.addView(editContent);
        updateMenstruationFields(selectedCategory[0], editContent);

        LinearLayout cycleRow = new LinearLayout(this);
        cycleRow.setOrientation(LinearLayout.HORIZONTAL);
        cycleRow.setGravity(Gravity.CENTER_VERTICAL);
        cycleRow.setPadding(0, dp(8), 0, dp(8));
        root.addView(cycleRow);

        EditText editCycle = makeEditText("주기");
        editCycle.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editCycle.addTextChangedListener(new TextWatcher() {
            private boolean correcting;

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (correcting) {
                    return;
                }

                String value = editable.toString();
                if (!value.matches("\\d*")) {
                    showOneSecondToast("주기는 숫자만 입력해 주세요.");
                    String digitsOnly = value.replaceAll("\\D", "");
                    correcting = true;
                    editable.replace(0, editable.length(), digitsOnly);
                    correcting = false;
                }
            }
        });
        editCycle.setText(isEdit ? String.valueOf(editingItem.cycleValue) : "");
        cycleRow.addView(editCycle, new LinearLayout.LayoutParams(0, dp(48), 1));

        Spinner unitSpinner = new Spinner(this);
        String[] units = {"일", "달", "년"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, units
        );
        unitSpinner.setAdapter(spinnerAdapter);
        if (isEdit) {
            for (int i = 0; i < units.length; i++) {
                if (units[i].equals(editingItem.cycleUnit)) {
                    unitSpinner.setSelection(i);
                    break;
                }
            }
        }
        cycleRow.addView(unitSpinner, new LinearLayout.LayoutParams(dp(92), dp(48)));

        TextView startDate = new TextView(this);
        final LocalDate[] selectedStartDate = {
                isEdit ? editingItem.startDate : LocalDate.now()
        };
        startDate.setText("시작일: " + formatFullDate(selectedStartDate[0]));
        startDate.setTextSize(14);
        startDate.setGravity(Gravity.CENTER_VERTICAL);
        startDate.setPadding(0, dp(8), 0, dp(8));
        root.addView(startDate);

        startDate.setOnClickListener(v -> {
            LocalDate current = selectedStartDate[0];
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedStartDate[0] =
                                LocalDate.of(year, month + 1, dayOfMonth);
                        startDate.setText(
                                "시작일: " + formatFullDate(selectedStartDate[0])
                        );
                    },
                    current.getYear(),
                    current.getMonthValue() - 1,
                    current.getDayOfMonth()
            );
            picker.show();
        });

        Button btnDone = new Button(this);
        btnDone.setText("완료");
        btnDone.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(getColor(R.color.green_w))
        );
        root.addView(
                btnDone,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(54)
                )
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .create();

        btnDone.setOnClickListener(v -> {
            boolean isMenstruation = "월경".equals(selectedCategory[0]);
            String content = isMenstruation
                    ? ""
                    : editContent.getText().toString().trim();
            String cycleText = editCycle.getText().toString().trim();

            if ((!isMenstruation && TextUtils.isEmpty(content))
                    || TextUtils.isEmpty(cycleText)) {
                Toast.makeText(this, "빈칸을 모두 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            int cycleValue;
            try {
                cycleValue = Integer.parseInt(cycleText);
            } catch (NumberFormatException e) {
                showOneSecondToast("주기는 숫자만 입력해 주세요.");
                return;
            }

            if (cycleValue <= 0) {
                Toast.makeText(this, "주기는 1 이상으로 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            HealthItem item = isEdit ? editingItem : new HealthItem();
            boolean wasHealthItem = isEdit && healthItems.contains(editingItem);
            boolean wasRoutineItem = isEdit && routineItems.contains(editingItem);
            item.id = isEdit
                    ? editingItem.id
                    : String.valueOf(System.currentTimeMillis());
            item.category = selectedCategory[0];
            item.title = "";
            item.content = content;
            item.cycleValue = cycleValue;
            item.cycleUnit = unitSpinner.getSelectedItem().toString();
            item.startDate = selectedStartDate[0];
            item.color = selectedColor[0];
            item.selected = false;

            boolean willBeHealthItem = !item.isRoutine();
            if (willBeHealthItem
                    && !wasHealthItem
                    && healthItems.size() >= MAX_SECTION_ITEMS) {
                Toast.makeText(
                        this, "최대 주기 개수에 도달했습니다.", Toast.LENGTH_SHORT
                ).show();
                return;
            }
            if (!willBeHealthItem
                    && !wasRoutineItem
                    && routineItems.size() >= MAX_SECTION_ITEMS) {
                Toast.makeText(
                        this, "최대 루틴 개수에 도달했습니다.", Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (isEdit) {
                moveItemToCorrectSection(item);
            } else if (item.isRoutine()) {
                routineItems.add(item);
                if (routineItems.size() == MAX_SECTION_ITEMS) {
                    Toast.makeText(
                            this, "최대 루틴 개수에 도달했습니다.", Toast.LENGTH_SHORT
                    ).show();
                }
            } else {
                healthItems.add(item);
                if (healthItems.size() == MAX_SECTION_ITEMS) {
                    Toast.makeText(
                            this, "최대 주기 개수에 도달했습니다.", Toast.LENGTH_SHORT
                    ).show();
                }
            }

            saveItems();
            refreshLists();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateMenstruationFields(
            String category, EditText editContent
    ) {
        boolean isMenstruation = "월경".equals(category);
        editContent.setVisibility(isMenstruation ? View.GONE : View.VISIBLE);
        if (isMenstruation) {
            editContent.setText("");
        }
    }

    private void moveItemToCorrectSection(HealthItem item) {
        healthItems.remove(item);
        routineItems.remove(item);
        if (item.isRoutine()) {
            routineItems.add(item);
        } else {
            healthItems.add(item);
        }
    }

    private EditText makeEditText(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextSize(14);
        editText.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)
        );
        params.setMargins(0, dp(4), 0, dp(4));
        editText.setLayoutParams(params);
        return editText;
    }

    private GradientDrawable makeRound(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private String formatFullDate(LocalDate date) {
        return date.format(
                DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.KOREAN)
        );
    }

    private String formatShortDate(LocalDate date) {
        return date.format(
                DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)
        );
    }

    private String makeCycleText(HealthItem item) {
        LocalDate nextDate = getNextDate(item);
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), nextDate);
        String cycleLabel = makeCycleLabel(item);

        if (daysLeft <= 0) {
            return cycleLabel + " : 오늘";
        }
        if (daysLeft <= 10) {
            return cycleLabel + " : " + daysLeft + "일 뒤";
        }
        if (daysLeft < 30) {
            long weeksLeft = (long) Math.ceil(daysLeft / 7.0);
            return cycleLabel + " : " + weeksLeft + "주 뒤";
        }
        return cycleLabel + " : " + formatShortDate(nextDate);
    }

    private String makeCycleLabel(HealthItem item) {
        if ("년".equals(item.cycleUnit)) {
            return item.cycleValue + "년마다";
        }
        if ("달".equals(item.cycleUnit)) {
            return item.cycleValue + "달마다";
        }
        return item.cycleValue + "일마다";
    }

    private LocalDate getNextDate(HealthItem item) {
        LocalDate today = LocalDate.now();
        LocalDate nextDate = item.startDate;
        if (!nextDate.isBefore(today)) {
            return nextDate;
        }

        if ("일".equals(item.cycleUnit)) {
            long passedDays = ChronoUnit.DAYS.between(nextDate, today);
            long cycleCount =
                    (long) Math.ceil(passedDays / (double) item.cycleValue);
            return nextDate.plusDays(cycleCount * item.cycleValue);
        }
        if ("달".equals(item.cycleUnit)) {
            while (nextDate.isBefore(today)) {
                nextDate = nextDate.plusMonths(item.cycleValue);
            }
            return nextDate;
        }
        while (nextDate.isBefore(today)) {
            nextDate = nextDate.plusYears(item.cycleValue);
        }
        return nextDate;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showOneSecondToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
        new Handler(Looper.getMainLooper()).postDelayed(toast::cancel, 1000);
    }

    private class HealthAdapter
            extends RecyclerView.Adapter<HealthAdapter.HealthViewHolder> {

        private final ArrayList<HealthItem> items;
        private final DeleteTarget section;

        HealthAdapter(ArrayList<HealthItem> items, DeleteTarget section) {
            this.items = items;
            this.section = section;
        }

        @NonNull
        @Override
        public HealthViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType
        ) {
            View view = LayoutInflater.from(HealthActivity.this)
                    .inflate(R.layout.item_health, parent, false);
            return new HealthViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull HealthViewHolder holder, int position
        ) {
            if (isAddPosition(position)) {
                bindAddButton(holder);
                return;
            }

            HealthItem item = items.get(position);
            boolean sectionDeleteMode = deleteTarget == section;
            holder.addHealthFab.setVisibility(View.GONE);
            holder.card.setVisibility(View.VISIBLE);
            holder.txtTitle.setText(
                    "월경".equals(item.category)
                            ? "월경"
                            : item.category + " : " + (
                                    TextUtils.isEmpty(item.content)
                                            ? item.title
                                            : item.content
                            )
            );
            holder.txtCycle.setText(makeCycleText(item));

            // 재사용된 체크박스가 다른 항목의 값을 바꾸지 않도록 리스너를 먼저 해제한다.
            holder.checkDelete.setOnCheckedChangeListener(null);
            holder.checkDelete.setVisibility(
                    sectionDeleteMode ? View.VISIBLE : View.GONE
            );
            holder.checkDelete.setChecked(item.selected);
            holder.checkDelete.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> item.selected = isChecked
            );

            holder.card.setBackground(
                    makeRound(item.color, 10, Color.TRANSPARENT)
            );
            holder.itemView.setOnClickListener(v -> {
                if (sectionDeleteMode) {
                    item.selected = !item.selected;
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        notifyItemChanged(adapterPosition);
                    }
                } else if (deleteTarget == DeleteTarget.NONE) {
                    showEditDialog(item);
                }
            });
        }

        private void bindAddButton(HealthViewHolder holder) {
            holder.card.setVisibility(View.INVISIBLE);
            holder.checkDelete.setVisibility(View.GONE);
            holder.addHealthFab.setVisibility(View.VISIBLE);
            holder.addHealthFab.setOnClickListener(v -> showEditDialog(null));
            holder.itemView.setOnClickListener(v -> showEditDialog(null));
        }

        private boolean isAddPosition(int position) {
            return shouldShowAddButton() && position == items.size();
        }

        private boolean shouldShowAddButton() {
            if (deleteTarget == section) {
                return false;
            }
            return items.size() < MAX_SECTION_ITEMS;
        }

//        @Override
//        public int getItemCount() {
//            return items.size() + (shouldShowAddButton() ? 1 : 0);
//        }

        @Override
        public int getItemCount() {
            if (items == null) return 0;

            // 🌟 커스텀 카테고리가 3개보다 많으면 딱 3개만 그리도록 제한!
            if (items.size() > 3) {
                return 3;
            }
            return items.size();
        }

        class HealthViewHolder extends RecyclerView.ViewHolder {
            final LinearLayout card;
            final TextView txtTitle;
            final TextView txtCycle;
            final CheckBox checkDelete;
            final FloatingActionButton addHealthFab;

            HealthViewHolder(@NonNull View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.healthCard);
                txtTitle = itemView.findViewById(R.id.txtHealthTitle);
                txtCycle = itemView.findViewById(R.id.txtHealthCycle);
                checkDelete = itemView.findViewById(R.id.checkDelete);
                addHealthFab = itemView.findViewById(R.id.addHealthFab);
            }
        }
    }

    private static class HealthItem {
        String id;
        String category;
        String title;
        String content;
        int cycleValue;
        String cycleUnit;
        LocalDate startDate;
        int color;
        boolean selected;

        boolean isRoutine() {
            return "일".equals(cycleUnit) && cycleValue <= 7;
        }

        JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("category", category);
            obj.put("title", title);
            obj.put("content", content);
            obj.put("cycleValue", cycleValue);
            obj.put("cycleUnit", cycleUnit);
            obj.put("startDate", startDate.toString());
            obj.put("color", color);
            return obj;
        }

        static HealthItem fromJson(JSONObject obj) {
            HealthItem item = new HealthItem();
            item.id = obj.optString(
                    "id", String.valueOf(System.currentTimeMillis())
            );
            item.category = obj.optString("category", "병원");
            item.title = obj.optString("title", "");
            item.content = obj.optString("content", "");
            item.cycleValue = obj.optInt("cycleValue", 1);
            item.cycleUnit = obj.optString("cycleUnit", "일");
            item.startDate = LocalDate.parse(
                    obj.optString("startDate", LocalDate.now().toString())
            );
            item.color = obj.optInt(
                    "color", Color.rgb(239, 248, 232)
            );
            return item;
        }
    }
}
