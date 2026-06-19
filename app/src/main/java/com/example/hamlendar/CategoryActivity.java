package com.example.hamlendar;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    ViewFlipper viewFlipper;

    // [1번째 화면] 카테고리 추가 폼
    ImageView closeBtn, menuBtn;
    EditText editCategory;
    LinearLayout layoutInputContainer;
    android.view.View viewColorBar;
    Button completeBtn, cancelBtn, btnTaskListAdd;
    ImageButton redBtn, orangeBtn, yellowBtn, greenBtn, blueBtn, purpleBtn, pinkBtn, greyBtn;

    String selectedColorType = "transparent";

    // [2번째 화면] 카테고리 목록 리스트
    ImageView btnBackToInput;
    RecyclerView rvCategoryList;
    ImageView imgMoreVert;

    LinearLayout layoutFixedImportant, layoutFixedTodo;

    CategoryAdapter categoryAdapter;
    List<CategoryItem> categoryDataList = new ArrayList<>();

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        db = FirebaseFirestore.getInstance();

        // 뷰 컴포넌트 연결
        viewFlipper = findViewById(R.id.viewFlipper);
        closeBtn = findViewById(R.id.imageView2);
        menuBtn = findViewById(R.id.btnDetail);

        layoutInputContainer = findViewById(R.id.layoutInputContainer);
        viewColorBar = findViewById(R.id.viewColorBar);
        editCategory = findViewById(R.id.edit03);

        completeBtn = findViewById(R.id.button);
        cancelBtn = findViewById(R.id.button2);
        btnTaskListAdd = findViewById(R.id.btnTaskListAdd);

        redBtn = findViewById(R.id.imageButton);
        orangeBtn = findViewById(R.id.imageButton2);
        yellowBtn = findViewById(R.id.imageButton3);
        greenBtn = findViewById(R.id.imageButton4);
        blueBtn = findViewById(R.id.imageButton5);
        purpleBtn = findViewById(R.id.imageButton6);
        pinkBtn = findViewById(R.id.imageButton7);
        greyBtn = findViewById(R.id.imageButton8);

        btnBackToInput = findViewById(R.id.btnBackToInput);
        rvCategoryList = findViewById(R.id.rvCategoryList);
        imgMoreVert = findViewById(R.id.imgMoreVert);

        layoutFixedImportant = findViewById(R.id.layoutFixedImportant);
        layoutFixedTodo = findViewById(R.id.layoutFixedTodo);

        // 상단 고정 뷰 색상 지정
        if (layoutFixedImportant != null && layoutFixedImportant.getBackground() != null) {
            Drawable wrapped = DrawableCompat.wrap(layoutFixedImportant.getBackground().mutate());
            DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, R.color.cat_red_light));
            layoutFixedImportant.setBackground(wrapped);
        }

        if (layoutFixedTodo != null && layoutFixedTodo.getBackground() != null) {
            Drawable wrapped = DrawableCompat.wrap(layoutFixedTodo.getBackground().mutate());
            DrawableCompat.setTint(wrapped, ContextCompat.getColor(this, R.color.cat_blue_light));
            layoutFixedTodo.setBackground(wrapped);
        }

        if (btnTaskListAdd != null) {
            btnTaskListAdd.setOnClickListener(v -> {
                editCategory.setError(null);
                editCategory.clearFocus();
                editCategory.setText("");
                selectColor("transparent");
                viewFlipper.setDisplayedChild(0);
            });
        }

        // 리사이클러뷰 세팅
        categoryAdapter = new CategoryAdapter(categoryDataList);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryList.setAdapter(categoryAdapter);

        rvCategoryList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // 순수 커스텀 카테고리만 카운트하므로 기존 기준(> 3) 유지
                if (categoryDataList.size() > 3 && imgMoreVert != null && btnTaskListAdd != null) {
                    if (!recyclerView.canScrollVertically(-1)) {
                        imgMoreVert.setVisibility(android.view.View.VISIBLE);
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) btnTaskListAdd.getLayoutParams();
                        params.topToBottom = R.id.imgMoreVert;
                        btnTaskListAdd.setLayoutParams(params);
                    } else if (dy > 0) {
                        imgMoreVert.setVisibility(android.view.View.GONE);
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) btnTaskListAdd.getLayoutParams();
                        params.topToBottom = R.id.rvCategoryList;
                        btnTaskListAdd.setLayoutParams(params);
                    }
                }
            }
        });

        loadCategoriesFromFirebase();

        viewFlipper.setDisplayedChild(1);

        redBtn.setOnClickListener(v -> selectColor("RED"));
        orangeBtn.setOnClickListener(v -> selectColor("ORANGE"));
        yellowBtn.setOnClickListener(v -> selectColor("YELLOW"));
        greenBtn.setOnClickListener(v -> selectColor("GREEN"));
        blueBtn.setOnClickListener(v -> selectColor("BLUE"));
        purpleBtn.setOnClickListener(v -> selectColor("PURPLE"));
        pinkBtn.setOnClickListener(v -> selectColor("PINK"));
        greyBtn.setOnClickListener(v -> selectColor("GREY"));

        btnBackToInput.setOnClickListener(v -> finish());
        menuBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));
        closeBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));
        cancelBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));

        completeBtn.setOnClickListener(v -> {
            String categoryName = editCategory.getText().toString().trim();

            if(categoryName.isEmpty()){
                editCategory.setError("카테고리 이름을 입력하세요");
                return;
            }

            if (categoryDataList != null && categoryDataList.size() >= 8) {
                Toast.makeText(this, "추가 카테고리는 최대 8개까지만 생성 가능합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            CategoryItem newItem = new CategoryItem(categoryName, selectedColorType);
            // 순수하게 리스트 맨 뒤의 인덱스로 지정
            newItem.setIndex(categoryDataList.size());

            db.collection("users").document(user.getUid()).collection("categories")
                    .add(newItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(CategoryActivity.this, "카테고리가 저장되었습니다!", Toast.LENGTH_SHORT).show();

                        newItem.setId(documentReference.getId());
                        categoryDataList.add(newItem);
                        categoryAdapter.notifyDataSetChanged();

                        updateMoreVertLayout();

                        editCategory.setError(null);
                        editCategory.setText("");
                        selectColor("RED");
                        viewFlipper.setDisplayedChild(1);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CategoryActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoriesFromFirebase();
        if (viewFlipper != null) {
            viewFlipper.setDisplayedChild(1);
        }
    }

    // 🌟 리스트 청소만 수행 (중요, 할 일 강제 삽입 코드 완전히 제거)
    private void addFixedCategoriesToList() {
        categoryDataList.clear();
    }

    private void loadCategoriesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("categories")
                .orderBy("index", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        addFixedCategoriesToList();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CategoryItem item = document.toObject(CategoryItem.class);
                            item.setId(document.getId());

                            // 고정형 카테고리 명칭 및 ID는 리사이클러뷰 데이터 리스트에 추가하지 않고 건너뜁니다.
                            if (item.getId() != null && (item.getId().contains("FIXED_") ||
                                    "할 일".equals(item.getName()) || "중요".equals(item.getName()))) {
                                continue;
                            }

                            categoryDataList.add(item);
                        }
                        categoryAdapter.notifyDataSetChanged();
                        updateMoreVertLayout();
                    } else {
                        loadCategoriesBackup(user.getUid());
                    }
                });
    }

    private void loadCategoriesBackup(String uid) {
        db.collection("users").document(uid).collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        addFixedCategoriesToList();

                        int fallbackIndex = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CategoryItem item = document.toObject(CategoryItem.class);
                            item.setId(document.getId());

                            if (item.getId() != null && (item.getId().contains("FIXED_") ||
                                    "할 일".equals(item.getName()) || "중요".equals(item.getName()))) {
                                continue;
                            }

                            if (item.getIndex() == 0) {
                                item.setIndex(fallbackIndex++);
                            }
                            categoryDataList.add(item);
                        }
                        categoryAdapter.notifyDataSetChanged();
                        updateMoreVertLayout();
                    }
                });
    }

    private void updateMoreVertLayout() {
        if (imgMoreVert != null && btnTaskListAdd != null) {
            if (categoryDataList.size() > 3) {
                imgMoreVert.setVisibility(android.view.View.VISIBLE);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) btnTaskListAdd.getLayoutParams();
                params.topToBottom = R.id.imgMoreVert;
                btnTaskListAdd.setLayoutParams(params);
            } else {
                imgMoreVert.setVisibility(android.view.View.GONE);
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) btnTaskListAdd.getLayoutParams();
                params.topToBottom = R.id.rvCategoryList;
                btnTaskListAdd.setLayoutParams(params);
            }
        }
    }

    private void selectColor(String colorType) {
        selectedColorType = colorType;
        int darkRes, lightRes;

        switch (colorType.toUpperCase()) {
            case "RED": darkRes = R.color.cat_red_dark; lightRes = R.color.cat_red_light; break;
            case "ORANGE": darkRes = R.color.cat_orange_dark; lightRes = R.color.cat_orange_light; break;
            case "YELLOW": darkRes = R.color.cat_yellow_dark; lightRes = R.color.cat_yellow_light; break;
            case "GREEN": darkRes = R.color.cat_green_dark; lightRes = R.color.cat_green_light; break;
            case "BLUE": darkRes = R.color.cat_blue_dark; lightRes = R.color.cat_blue_light; break;
            case "PURPLE": darkRes = R.color.cat_purple_dark; lightRes = R.color.cat_purple_light; break;
            case "PINK": darkRes = R.color.cat_pink_dark; lightRes = R.color.cat_pink_light; break;
            case "GREY":
            default: darkRes = R.color.cat_grey_dark; lightRes = R.color.cat_grey_light; break;
        }

        int darkColor = ContextCompat.getColor(this, darkRes);
        int lightColor = ContextCompat.getColor(this, lightRes);

        viewColorBar.setBackgroundColor(darkColor);
        layoutInputContainer.setBackgroundColor(lightColor);
    }
}