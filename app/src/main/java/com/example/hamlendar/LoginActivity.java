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
import com.google.firebase.firestore.FirebaseFirestore;

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
        TextView findIdMove = findViewById(R.id.findId_textView);
        TextView findPwMove = findViewById(R.id.findPw_textView);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        loginBtn.setOnClickListener(v -> login());

        registerMove.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        findIdMove.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, FindIdActivity.class)));

        findPwMove.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, FindPwActivity.class)));
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

                        // 🌟 [긴급 수리] 일기장과 건강 락커 폭파 코드 완벽 제거!! 오직 프로필만 갱신합니다!!
                        getSharedPreferences("user_pref", MODE_PRIVATE).edit().clear().apply();

                        // (이 자리에 있던 diary_pref, health_pref clear 삭제 완료!!)

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();

                            FirebaseFirestore.getInstance().collection("users").document(myEmailKey).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();

                                        if (documentSnapshot.exists()) {
                                            String sName = documentSnapshot.getString("user_name");
                                            String sRealName = documentSnapshot.getString("real_name");
                                            String sPhone = documentSnapshot.getString("user_phone");
                                            String sBirth = documentSnapshot.getString("user_birth");
                                            String sNickname = documentSnapshot.getString("user_nickname");
                                            String sProfile = documentSnapshot.getString("user_profile_uri");

                                            if (sName != null) editor.putString("user_name", sName);
                                            if (sRealName != null) editor.putString("real_name", sRealName);
                                            if (sPhone != null) editor.putString("user_phone", sPhone);
                                            if (sBirth != null) editor.putString("user_birth", sBirth);
                                            if (sNickname != null) editor.putString("user_nickname", sNickname);
                                            if (sProfile != null) editor.putString("user_profile_uri", sProfile);
                                        } else {
                                            editor.putString("user_name", user.getDisplayName());
                                            if (user.getPhotoUrl() != null) {
                                                editor.putString("user_profile_uri", user.getPhotoUrl().toString());
                                            }
                                        }
                                        editor.apply();

                                        Toast.makeText(this, "로그인 성공! 🐹", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "로그인 성공! 🐹", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    });
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "로그인 실패";
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}