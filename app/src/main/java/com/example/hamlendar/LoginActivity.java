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

        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> login());

        registerMove.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
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

                        // ✅ context 명확히
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);

                        finish();

                    } else {

                        // 🔥 진짜 원인 출력 (이거 중요)
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "로그인 실패";

                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();

                        // Logcat에도 출력
                        if (task.getException() != null) {
                            task.getException().printStackTrace();
                        }
                    }
                });
    }
}