package com.example.hamlendar;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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

        // 하단 카테고리 추가 버튼 누를 때 이벤트
        if (btnTaskListAdd != null) {
            btnTaskListAdd.setOnClickListener(v -> {
                // 🌟 추가 질문 해결: 새로 추가 폼으로 들어갈 때 에러 상태와 붉은색 글씨/라인을 깨끗하게 비워줍니다.
                editCategory.setError(null);
                editCategory.clearFocus();

                editCategory.setText("");
                selectColor("transparent"); // 기본 컬러 폼으로 리셋
                viewFlipper.setDisplayedChild(0);
            });
        }

        // 리사이클러뷰 세팅
        categoryAdapter = new CategoryAdapter(categoryDataList);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryList.setAdapter(categoryAdapter);

        // 데이터 원격 로드
        loadCategoriesFromFirebase();

        // 설정창 진입 시 초기 화면은 목록 화면(1)
        viewFlipper.setDisplayedChild(1);

        // 색상 선택 이벤트
        redBtn.setOnClickListener(v -> selectColor("RED"));
        orangeBtn.setOnClickListener(v -> selectColor("ORANGE"));
        yellowBtn.setOnClickListener(v -> selectColor("YELLOW"));
        greenBtn.setOnClickListener(v -> selectColor("GREEN"));
        blueBtn.setOnClickListener(v -> selectColor("BLUE"));
        purpleBtn.setOnClickListener(v -> selectColor("PURPLE"));
        pinkBtn.setOnClickListener(v -> selectColor("PINK"));
        greyBtn.setOnClickListener(v -> selectColor("GREY"));

        // 화면 전환 리스너
        btnBackToInput.setOnClickListener(v -> finish());
        menuBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));
        closeBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));
        cancelBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));

        // 완료 버튼 (파이어베이스 데이터 저장)
        completeBtn.setOnClickListener(v -> {
            String categoryName = editCategory.getText().toString().trim();

            if(categoryName.isEmpty()){
                editCategory.setError("카테고리 이름을 입력하세요");
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            CategoryItem newItem = new CategoryItem(categoryName, selectedColorType);
            newItem.setIndex(categoryDataList.size()); // 순서 값 지정

            db.collection("users").document(user.getUid()).collection("categories")
                    .add(newItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(CategoryActivity.this, "카테고리가 저장되었습니다!", Toast.LENGTH_SHORT).show();

                        newItem.setId(documentReference.getId());
                        categoryDataList.add(newItem);
                        categoryAdapter.notifyDataSetChanged();

                        // 🌟 추가 질문 해결: 저장 성공 시 에러 상태를 초기화(Null)하여 다음 진입 시 붉은 테두리가 생기지 않도록 방지합니다.
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

    private void loadCategoriesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // 🌟 중요: 'index' 필드가 없는 구형 데이터가 있으면 에러가 발생하므로,
        // 쿼리가 실패하더라도 앱이 멈추거나 리스트가 안 불려오지 않게 완벽하게 예외 처리를 추가했습니다.
        db.collection("users").document(user.getUid()).collection("categories")
                .orderBy("index", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        categoryDataList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CategoryItem item = document.toObject(CategoryItem.class);
                            item.setId(document.getId());
                            categoryDataList.add(item);
                        }
                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        // 만약 'index' 정렬 색인 오류 등으로 실패하면 백업용으로 전체 데이터를 그냥 로드합니다.
                        loadCategoriesBackup(user.getUid());
                    }
                });
    }

    // 데이터 유실 방지용 백업 로더 (index 필드가 없는 기존 유저용 방어 코드)
    private void loadCategoriesBackup(String uid) {
        db.collection("users").document(uid).collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        categoryDataList.clear();
                        int fallbackIndex = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CategoryItem item = document.toObject(CategoryItem.class);
                            item.setId(document.getId());
                            if (item.getIndex() == 0) {
                                item.setIndex(fallbackIndex++);
                            }
                            categoryDataList.add(item);
                        }
                        categoryAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void selectColor(String colorType) {
        selectedColorType = colorType;
        int darkRes;
        int lightRes;

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