package com.example.hamlendar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.logintext);
        password = findViewById(R.id.passwordtext);
        Button loginBtn = findViewById(R.id.loginbutton);
        TextView registerMove = findViewById(R.id.register_textView);

        // Firebase 로그인 기능 사용을 위한 객체 생성
        mAuth = FirebaseAuth.getInstance();

        // 로그인 버튼 클릭 -> 입력한 이메일/비밀번호로 로그인 시도
        loginBtn.setOnClickListener(v -> login());

        // 회원가입 하러가기 클릭 -> 회원가입 화면으로 이동
        registerMove.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void login() {
        // 사용자가 입력한 이메일과 비밀번호 가져오기
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        // 빈칸이 있으면 로그인 시도하지 않고 안내 문구 출력
        if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword)) {
            Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase에 이메일/비밀번호 로그인 요청
        mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "이메일 또는 비밀번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
