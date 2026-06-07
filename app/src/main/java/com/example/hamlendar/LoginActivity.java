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

        // 기존 바인딩 요소들
        email = findViewById(R.id.logintext);
        password = findViewById(R.id.passwordtext);
        Button loginBtn = findViewById(R.id.loginbutton);
        TextView registerMove = findViewById(R.id.register_textView);

        // 🌟 새롭게 추가된 아이디 찾기 / 비밀번호 찾기 텍스트뷰 바인딩
        TextView findIdMove = findViewById(R.id.findId_textView);
        TextView findPwMove = findViewById(R.id.findPw_textView);

        mAuth = FirebaseAuth.getInstance();

        // 1. 로그인 버튼 클릭 리스너
        loginBtn.setOnClickListener(v -> login());

        // 2. 회원가입 하러가기 클릭 리스너
        registerMove.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // 🌟 3. 아이디 찾기 누르면 FindIdActivity로 전환
        findIdMove.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindIdActivity.class);
            startActivity(intent);
        });

        // 🌟 4. 비밀번호 찾기 누르면 FindPwActivity로 전환
        findPwMove.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, FindPwActivity.class);
            startActivity(intent);
        });
    }

    private void login() {
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword)) {
            Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "로그인 실패";

                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

                        if (task.getException() != null) {
                            task.getException().printStackTrace();
                        }
                    }
                });
    }
}