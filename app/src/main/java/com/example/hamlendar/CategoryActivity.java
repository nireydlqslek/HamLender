package com.example.hamlendar;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {

    // 상단 버튼
    ImageView closeBtn, menuBtn;

    // 입력창
    EditText editCategory;

    // 하단 버튼
    Button completeBtn, cancelBtn;

    // 색상 버튼
    ImageButton redBtn, orangeBtn, yellowBtn,
            greenBtn, blueBtn, purpleBtn,
            pinkBtn, greyBtn;

    // 선택 색상 저장
    String selectedColor = "#FFB2B2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XML 연결
        setContentView(R.layout.activity_category);

        // =========================
        // View 연결
        // =========================

        closeBtn = findViewById(R.id.imageView2);
        menuBtn = findViewById(R.id.imageView3);

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

        // =========================
        // 색상 버튼 클릭 이벤트
        // =========================

        redBtn.setOnClickListener(v ->
                selectColor("#FFB2B2"));

        orangeBtn.setOnClickListener(v ->
                selectColor("#FFD180"));

        yellowBtn.setOnClickListener(v ->
                selectColor("#FFF59D"));

        greenBtn.setOnClickListener(v ->
                selectColor("#A5D6A7"));

        blueBtn.setOnClickListener(v ->
                selectColor("#90CAF9"));

        purpleBtn.setOnClickListener(v ->
                selectColor("#CE93D8"));

        pinkBtn.setOnClickListener(v ->
                selectColor("#F8BBD0"));

        greyBtn.setOnClickListener(v ->
                selectColor("#E0E0E0"));

        // =========================
        // 완료 버튼
        // =========================

        completeBtn.setOnClickListener(v -> {

            String categoryName =
                    editCategory.getText().toString().trim();

            // 입력 안 했을 때
            if(categoryName.isEmpty()){

                editCategory.setError("카테고리 이름을 입력하세요");
                return;
            }

            // 결과 출력
            Toast.makeText(
                    CategoryActivity.this,
                    "카테고리 : "
                            + categoryName
                            + "\n선택 색상 : "
                            + selectedColor,
                    Toast.LENGTH_SHORT
            ).show();

        });

        // =========================
        // 취소 버튼
        // =========================

        cancelBtn.setOnClickListener(v -> finish());

        // =========================
        // 닫기 버튼
        // =========================

        closeBtn.setOnClickListener(v -> finish());

    }

    // =========================
    // 색상 선택 메서드
    // =========================

    private void selectColor(String colorCode){

        selectedColor = colorCode;

        // EditText 배경색 변경
        editCategory.setBackgroundColor(
                Color.parseColor(colorCode)
        );

    }
}
