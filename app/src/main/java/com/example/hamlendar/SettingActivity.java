package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

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

        // 1. XML 뷰 컴포넌트 바인딩
        ImageView btnBack = findViewById(R.id.back_icon);
        TextView btnMyInfo = findViewById(R.id.btnMyInfo);
        TextView btnFriend = findViewById(R.id.btnFriend);
        TextView btnCategory = findViewById(R.id.btnCategory);
        TextView btnLogout = findViewById(R.id.btnLogout);
        TextView btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        SwitchCompat switchAi = findViewById(R.id.switchAi);

        // 2. Firebase 인증 객체 초기화
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // 3. AI 리포트 스위치 토글 설정 상태 불러오기 & 저장 시스템
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isAiEnabled = sharedPreferences.getBoolean("isAiEnabled", true);
        switchAi.setChecked(isAiEnabled);

        switchAi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isAiEnabled", isChecked);
            editor.apply();

            String status = isChecked ? "AI 리포트 기능이 켜졌습니다." : "AI 리포트 기능이 꺼졌습니다.";
            Toast.makeText(SettingActivity.this, status, Toast.LENGTH_SHORT).show();
        });

        // 4. 클릭 리스너 이벤트 등록 영역 (중복 제거 완료)

        // 🔙 뒤로가기 버튼 클릭
        btnBack.setOnClickListener(v -> finish());

        // 🌟 내 정보 열람 클릭 -> 프로필 화면 이동
        btnMyInfo.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // 👥 친구 관리 클릭 -> 친구초대 화면 이동
        btnFriend.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, InviteFriendActivity.class);
            startActivity(intent);
        });

        // 📂 카테고리 편집 클릭 -> 카테고리 화면 이동
        btnCategory.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, CategoryActivity.class);
            startActivity(intent);
        });

        // 🚪 로그아웃 클릭 -> 로그아웃 후 첫 화면 이동
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SettingActivity.this, FirstScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 🚨 회원 탈퇴 클릭 -> 비밀번호 확인 다이얼로그 표시
        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog());
    } // 👈 onCreate 메서드는 여기서 정상적으로 딱 한 번 닫혀야 합니다.

    // [회원 탈퇴 비밀번호 검증 팝업창]
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

    // [실제 Firebase 데이터베이스 계정 삭제 처리]
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