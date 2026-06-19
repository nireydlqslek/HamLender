package com.example.hamlendar;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView ivMyProfile;
    private EditText etName, etPhone, etBirth, etNickname;
    private TextView tvMyEmail;
    private EditText etNewPassword, etNewPasswordConfirm;
    private Button btnSaveProfile;

    private FirebaseAuth mAuth;
    private boolean isReadyToSave = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btn_back);
        ivMyProfile = findViewById(R.id.iv_my_profile);
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etBirth = findViewById(R.id.et_birth);
        etNickname = findViewById(R.id.et_nickname);
        tvMyEmail = findViewById(R.id.tv_my_email_text);

        etNewPassword = findViewById(R.id.et_new_password);
        etNewPasswordConfirm = findViewById(R.id.et_new_password_confirm);
        btnSaveProfile = findViewById(R.id.btn_save_profile);

        mAuth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> finish());

        // 내 정보 복원 기동
        loadRegisterUserData();

        setupPasswordInputWatchers();

        btnSaveProfile.setOnClickListener(v -> {
            if (!isReadyToSave) {
                Toast.makeText(this, "새 비밀번호와 확인 칸을 모두 채워주셔야 저장이 가능합니다! 🐹", Toast.LENGTH_SHORT).show();
            } else {
                updateNewPassword();
            }
        });
    }

    /**
     * 🏠 [복원 엔진 완벽 수리] 가입창 상자 이름표인 "user_pref"와 100% 동치 결합 완료!
     */
    private void loadRegisterUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // 💡 [버그 완전 격파] RegisterActivity 양식과 똑같이 순정 "user_pref" 상자를 열어야 정상 로드됩니다!
        SharedPreferences prefs = getGetSharedPreferences("user_pref", MODE_PRIVATE);

        String registerRealName = prefs.getString("real_name", "");
        String registerPhone = prefs.getString("user_phone", "");
        String registerBirth = prefs.getString("user_birth", "");
        String registerNickname = prefs.getString("user_name", currentUser.getDisplayName());
        String registerEmail = prefs.getString("user_email_id", currentUser.getEmail());

        etName.setText(registerRealName);
        etPhone.setText(registerPhone);
        etBirth.setText(registerBirth);
        etNickname.setText(registerNickname);
        tvMyEmail.setText(registerEmail);

        // 💡 [2중 철통 방어 사진 로드]
        // 내 폰 서랍장 경로가 깨졌을 때를 대비하여, 파이어베이스 계정 자체에 내장된 고유 PhotoUrl 주소를 최우선으로 Glide 로드합니다!
        if (currentUser.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.hampic)
                    .error(R.drawable.hampic)
                    .into(ivMyProfile);
        } else {
            String registerProfileUriStr = prefs.getString("user_profile_uri", "");
            if (!TextUtils.isEmpty(registerProfileUriStr)) {
                Glide.with(this)
                        .load(Uri.parse(registerProfileUriStr))
                        .circleCrop()
                        .placeholder(R.drawable.hampic)
                        .error(R.drawable.hampic)
                        .into(ivMyProfile);
            } else {
                ivMyProfile.setImageResource(R.drawable.hampic);
            }
        }
    }

    // SharedPreferences 오타 방지용 가상 브릿지 메서드
    private SharedPreferences getGetSharedPreferences(String name, int mode) {
        return getSharedPreferences(name, mode);
    }

    private void setupPasswordInputWatchers() {
        TextWatcher pwWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pw = etNewPassword.getText().toString().trim();
                String pwConfirm = etNewPasswordConfirm.getText().toString().trim();

                if (!TextUtils.isEmpty(pw) && !TextUtils.isEmpty(pwConfirm)) {
                    btnSaveProfile.setText("저장");
                    isReadyToSave = true;
                } else {
                    btnSaveProfile.setText("수정가능");
                    isReadyToSave = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etNewPassword.addTextChangedListener(pwWatcher);
        etNewPasswordConfirm.addTextChangedListener(pwWatcher);
    }

    private void updateNewPassword() {
        String newPw = etNewPassword.getText().toString().trim();
        String newPwConfirm = etNewPasswordConfirm.getText().toString().trim();

        if (!newPw.equals(newPwConfirm)) {
            Toast.makeText(this, "비밀번호 확인이 서로 일치하지 않습니다. 😥", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPw.length() < 6) {
            Toast.makeText(this, "비밀번호는 최소 6자리 이상 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPw)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 여기도 공용 순정 상자로 일치 완료
                            SharedPreferences prefs = getGetSharedPreferences("user_pref", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("user_password", newPw);
                            editor.apply();

                            Toast.makeText(ProfileActivity.this, "비밀번호가 성공적으로 변경되었습니다! 🐹💚", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "변경 실패";
                            Toast.makeText(ProfileActivity.this, "실패: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}