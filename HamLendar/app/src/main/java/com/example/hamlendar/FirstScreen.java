package com.example.hamlendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FirstScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_first_screen);

        // 버튼 연결
        Button loginBtn = findViewById(R.id.login);
        Button joinBtn = findViewById(R.id.join);

        // 로그인 버튼 클릭 → LoginActivity로 이동
        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(FirstScreen.this, LoginActivity.class);
            startActivity(intent);
        });

        // 회원가입 버튼 클릭 → RegisterActivity로 이동
        joinBtn.setOnClickListener(v -> {
            Intent intent = new Intent(FirstScreen.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 상태바/내비게이션바 영역과 화면 내용이 겹치지 않도록 여백 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
