package com.example.hamlendar;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 상단 뒤로가기 및 하단 액션 버튼 바인딩
        ImageView btnBack = findViewById(R.id.back_icon);
        TextView btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        TextView btnLogout = findViewById(R.id.btnLogout);

        // 친구 관리 버튼 가져오기
        TextView btnFriend = findViewById(R.id.btnFriend);

        // 🌟 [핵심 연동] XML에서 준비해 둔 '내 정보 열람' 버튼 가져오기!
        TextView btnMyInfo = findViewById(R.id.btnMyInfo);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // 🔙 뒤로가기 버튼 클릭 -> 이전 화면(메인 또는 마이페이지)으로 돌아가기
        btnBack.setOnClickListener(v -> finish());

        // 🌟 1. 내 정보 열람 클릭 -> 방금 만든 프로필 액티비티(ProfileActivity)로 스르륵 이동!
        btnMyInfo.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // 👥 2. 친구 관리 클릭 -> 친구초대 액티비티(InviteFriendActivity)로 이동
        btnFriend.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, InviteFriendActivity.class);
            startActivity(intent);
        });

        // 🚪 3. 로그아웃 클릭 -> Firebase 로그아웃 후 첫 화면으로 이동
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SettingActivity.this, FirstScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 🚨 4. 회원 탈퇴 클릭 -> 비밀번호 확인 다이얼로그 표시
        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog());
    }

    private void showDeleteDialog() {
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("비밀번호를 다시 입력하세요");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("회원 탈퇴")
                .setMessage("비밀번호 확인 후 계정을 삭제합니다.")
                .setView(passwordInput)
                .setPositiveButton("탈퇴하기", (dialog, which) -> {
                    String password = passwordInput.getText().toString().trim();
                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(this, "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    deleteAccount(password);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteAccount(String password) {
        String email = user.getEmail();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.delete().addOnCompleteListener(deleteTask -> {
                            if (deleteTask.isSuccessful()) {
                                Toast.makeText(this, "회원 탈퇴 완료", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SettingActivity.this, FirstScreen.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "탈퇴 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "비밀번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}