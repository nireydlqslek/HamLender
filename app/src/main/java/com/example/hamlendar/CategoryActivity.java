package com.example.hamlendar;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
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
    Button completeBtn, cancelBtn;
    ImageButton redBtn, orangeBtn, yellowBtn, greenBtn, blueBtn, purpleBtn, pinkBtn, greyBtn;
    String selectedColor = "#FFB2B2";

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

        // 뷰 연결
        viewFlipper = findViewById(R.id.viewFlipper);
        closeBtn = findViewById(R.id.imageView2);
        menuBtn = findViewById(R.id.btnDetail);
        editCategory = findViewById(R.id.edit03);
        completeBtn = findViewById(R.id.button);
        cancelBtn = findViewById(R.id.button2);

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

        // 리사이클러뷰 세팅
        categoryAdapter = new CategoryAdapter(categoryDataList);
        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        rvCategoryList.setAdapter(categoryAdapter);

        // 데이터 원격 로드
        loadCategoriesFromFirebase();

        // 🌟 요구사항 반영: 화면이 켜질 때는 무조건 카테고리 "추가 입력 폼(index 0)"이 바로 나오게 만듭니다!
        viewFlipper.setDisplayedChild(0);

        // 색상 선택 이벤트
        redBtn.setOnClickListener(v -> selectColor("#FFB2B2"));
        orangeBtn.setOnClickListener(v -> selectColor("#FFD180"));
        yellowBtn.setOnClickListener(v -> selectColor("#FFF59D"));
        greenBtn.setOnClickListener(v -> selectColor("#A5D6A7"));
        blueBtn.setOnClickListener(v -> selectColor("#90CAF9"));
        purpleBtn.setOnClickListener(v -> selectColor("#CE93D8"));
        pinkBtn.setOnClickListener(v -> selectColor("#F8BBD0"));
        greyBtn.setOnClickListener(v -> selectColor("#E0E0E0"));

        // 🌟 화면 전환 내비게이션 재정의
        // 추가 화면에서 상단 좌측 '메뉴(삼선) 버튼'을 누르면 저장된 목록 화면(index 1)으로 이동합니다.
        menuBtn.setOnClickListener(v -> viewFlipper.setDisplayedChild(1));

        // 목록 화면에서 '뒤로가기(←)' 화살표를 누르면 다시 추가 입력 폼(index 0)으로 돌아옵니다.
        btnBackToInput.setOnClickListener(v -> viewFlipper.setDisplayedChild(0));

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

            CategoryItem newItem = new CategoryItem(categoryName, selectedColor);

            db.collection("users").document(user.getUid()).collection("categories")
                    .add(newItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(CategoryActivity.this, "카테고리가 서버에 저장되었습니다!", Toast.LENGTH_SHORT).show();

                        newItem.setId(documentReference.getId());
                        categoryDataList.add(newItem);
                        categoryAdapter.notifyDataSetChanged();

                        // 저장 후 입력창 리셋 및 목록 화면(index 1)으로 전환해서 보여주기
                        editCategory.setText("");
                        selectColor("#FFB2B2");
                        editCategory.setBackgroundColor(Color.parseColor("#EAEFEF"));
                        editCategory.setTextColor(Color.BLACK);
                        viewFlipper.setDisplayedChild(1);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CategoryActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // 추가 폼에서 취소 또는 우상단 X 버튼 누르면 액티비티를 끄고 메인(설정)화면으로 퇴장
        cancelBtn.setOnClickListener(v -> finish());
        closeBtn.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoriesFromFirebase();
        // 🌟 액티비티가 다시 켜질 때도 무조건 첫 번째 추가 화면이 먼저 뜨도록 강제 초기화!
        if (viewFlipper != null) {
            viewFlipper.setDisplayedChild(0);
        }
    }

    private void loadCategoriesFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("categories")
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
                    }
                });
    }

    private void selectColor(String colorCode){
        selectedColor = colorCode;
        editCategory.setBackgroundColor(Color.parseColor(colorCode));
    }
}