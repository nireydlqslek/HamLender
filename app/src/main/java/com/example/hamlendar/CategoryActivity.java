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

        if (btnTaskListAdd != null) {
            btnTaskListAdd.setOnClickListener(v -> {
                editCategory.setError(null);
                editCategory.clearFocus();
                editCategory.setText("");
                selectColor("transparent");
                viewFlipper.setDisplayedChild(0);
            });
        }

        categoryAdapter = new CategoryAdapter(categoryDataList);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryList.setAdapter(categoryAdapter);

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

        // 🌟 [핵심 수리 완료] 카테고리 저장할 때 UID 말고 "이메일 이름표" 폴더에 쏙 저장합니다!
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
            newItem.setIndex(categoryDataList.size());

            // 💡 여기서 암호 폴더 대신 이메일 폴더로 경로를 바꿔줍니다!
            String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();

            db.collection("users").document(myEmailKey).collection("categories")
                    .add(newItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(CategoryActivity.this, "카테고리가 저장되었습니다!", Toast.LENGTH_SHORT).show();

                        newItem.setId(documentReference.getId());
                        categoryDataList.add(newItem);
                        categoryAdapter.notifyDataSetChanged();

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

    // 🌟 [핵심 수리 완료] 카테고리 목록 읽어올 때도 "이메일 이름표" 폴더에서 똑바로 읽어옵니다!
    private void loadCategoriesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // 💡 읽어올 때도 암호 폴더 대신 이메일 폴더로 경로를 맞춰줍니다!
        String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();

        db.collection("users").document(myEmailKey).collection("categories")
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
                        loadCategoriesBackup(myEmailKey);
                    }
                });
    }

    private void loadCategoriesBackup(String emailKey) {
        db.collection("users").document(emailKey).collection("categories")
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