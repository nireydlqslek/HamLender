package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private EditText passwordConfirm;
    private EditText name;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        email = findViewById(R.id.edit_email);
        password = findViewById(R.id.edit_password);
        passwordConfirm = findViewById(R.id.edit_password2);
        name = findViewById(R.id.edit_name);
        Button registerBtn = findViewById(R.id.register_btn);
        TextView loginMove = findViewById(R.id.login_move);

        // Firebase 회원가입 기능 사용을 위한 객체 생성
        mAuth = FirebaseAuth.getInstance();

        // 회원가입 버튼 클릭 -> 입력값 확인 후 계정 생성
        registerBtn.setOnClickListener(v -> register());

        // 이미 계정이 있는 경우 로그인 화면으로 이동
        loginMove.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void register() {
        // 사용자가 입력한 회원가입 정보 가져오기
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();
        String confirmPassword = passwordConfirm.getText().toString().trim();
        String userName = name.getText().toString().trim();

        // 하나라도 비어 있으면 회원가입하지 않음
        if (TextUtils.isEmpty(userEmail)
                || TextUtils.isEmpty(userPassword)
                || TextUtils.isEmpty(confirmPassword)
                || TextUtils.isEmpty(userName)) {
            Toast.makeText(this, "모든 정보를 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 비밀번호와 비밀번호 확인 값이 같은지 검사
        if (!userPassword.equals(confirmPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase에 새 계정 생성 요청
        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserName(userName);
                    } else {
                        String message = task.getException() == null
                                ? "회원가입 실패"
                                : task.getException().getMessage();
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserName(String userName) {
        // 메인 화면에서 바로 사용할 수 있도록 기기에도 이름 저장
        SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
        prefs.edit().putString("user_name", userName).apply();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            moveToLogin();
            return;
        }

        // Firebase 사용자 프로필 displayName에 회원가입 name 저장
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(profileTask -> {
                    Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                    moveToLogin();
                });
    }

    private void moveToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
