package com.example.hamlendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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

public class FindIdActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etPhone;
    private EditText etBirth;
    private TextView tvFindIdResult;
    private Button btnDoFindId;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🌟 상단바와 하단 네비게이션바를 앱 테마와 투명하게 엮어주는 최신 EdgeToEdge 적용!
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_id);

        // 🌟 에러 방지: ScrollView 내부의 ConstraintLayout 또는 최상단 뷰에 상/하단바 패딩을 유연하게 적용
        // xml 파일의 최상단 스크롤 뷰나 컨텐츠 영역이 부드럽게 시스템 바 아래로 안착합니다.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🎯 정중앙 정렬 레이아웃xml의 컴포넌트 ID들과 자바 객체 1:1 매핑 연동
        btnBack = findViewById(R.id.btn_back);
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etBirth = findViewById(R.id.et_birth);
        tvFindIdResult = findViewById(R.id.tv_findId_result);
        btnDoFindId = findViewById(R.id.btn_do_findId);

        // 🔙 1. 왼쪽 상단 뒤로가기 아이콘 클릭 시 현재 창 닫고 로그인창으로 복귀
        btnBack.setOnClickListener(v -> finish());

        // 🔍 2. 초록색 아이디 찾기 버튼 클릭 시 매칭 검증 이벤트 실행
        btnDoFindId.setOnClickListener(v -> findUserMailId());
    }

    /**
     * 이름, 전화번호, 생년월일 8자리를 검증하여 아이디(이메일)를 찾아주는 메서드
     */
    /**
     * 이름, 전화번호, 생년월일 8자리를 검증하여 아이디(이메일)를 찾아주는 메서드
     */
    private void findUserMailId() {
        String inputName = etName.getText().toString().trim();
        String inputPhone = etPhone.getText().toString().trim();
        String rawBirth = etBirth.getText().toString().trim();

        String inputBirth = rawBirth.replace(".", "");

        if (TextUtils.isEmpty(inputName) || TextUtils.isEmpty(inputPhone) || TextUtils.isEmpty(inputBirth)) {
            Toast.makeText(this, "이름, 전화번호, 생년월일을 모두 입력해주세요! 🐹", Toast.LENGTH_SHORT).show();
            return;
        }

        if (inputBirth.length() != 8) {
            Toast.makeText(this, "생년월일은 8자리 숫자로 정확히 입력해주세요! (예: 20260601)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🌟 [핵심] SharedPreferences에서 가입된 진짜 유저 정보 꺼내오기!
        android.content.SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
        String savedName = prefs.getString("real_name", "");
        String savedPhone = prefs.getString("user_phone", "");
        String savedBirth = prefs.getString("user_birth", "");
        String savedEmail = prefs.getString("user_email_id", "가입된 이메일을 찾을 수 없습니다.");

        // 🌟 가짜 데이터 대신, '방금 내가 가입한 정보'와 입력값을 대조합니다!
        if (inputName.equals(savedName) && inputPhone.equals(savedPhone) && inputBirth.equals(savedBirth)) {

            // 일치하면 기기에 저장되어 있던 실제 이메일을 출력!
            tvFindIdResult.setText("조회된 아이디\n👉 " + savedEmail + " 🐹 👈");
            tvFindIdResult.setVisibility(View.VISIBLE);

        } else {
            tvFindIdResult.setText("일치하는 햄린더 회원이 없습니다. 😥");
            tvFindIdResult.setVisibility(View.VISIBLE);
        }
    }
    }
