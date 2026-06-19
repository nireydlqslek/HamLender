package com.example.hamlendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FindPwActivity extends AppCompatActivity {

    private EditText etNickname;
    private EditText etPhone;
    private Spinner spinnerQuestion;
    private EditText etAnswer;
    private TextView tvFindPwResult;
    private Button btnDoFindPw;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 최신 EdgeToEdge 전체 화면 활성화
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_find_pw);

        // 상/하단바 패딩 밀림 방지
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // XML 컴포넌트들과 자바 객체 1:1 매핑 연동
        btnBack = findViewById(R.id.btn_back_pw);
        etNickname = findViewById(R.id.et_findPw_nickname);
        etPhone = findViewById(R.id.et_findPw_phone);
        spinnerQuestion = findViewById(R.id.spinner_findPw_question);
        etAnswer = findViewById(R.id.et_findPw_answer);
        tvFindPwResult = findViewById(R.id.tv_findPw_result);
        btnDoFindPw = findViewById(R.id.btn_do_findId);

        // 🌟 [동기화 1] 회원가입창(RegisterActivity)에서 추가한 새로운 3가지 보안 질문 리스트로 완벽 일치!
        String[] questions = {
                "애완동물의 이름은 ?",
                "가장 좋아하는 캐릭터 이름은?",
                "제일 좋아하는 책 이름은 ?"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, questions);
        spinnerQuestion.setAdapter(adapter);

        // 🔙 뒤로가기 화살표 클릭 시 로그인창으로 복귀
        btnBack.setOnClickListener(v -> finish());

        // 🔍 비밀번호 찾기 버튼 클릭 이벤트
        btnDoFindPw.setOnClickListener(v -> findUserPassword());
    }

    /**
     * 별명, 전화번호, 보안 질문 종류, 답변의 4대 보안 조건을 대조하는 메서드
     */
    /**
     * 별명, 전화번호, 보안 질문 종류, 답변의 4대 보안 조건을 대조하는 메서드
     */
    private void findUserPassword() {
        String inputNickname = etNickname.getText().toString().trim();
        String inputPhone = etPhone.getText().toString().trim();

        // 🌟 [핵심 변경] 질문 텍스트 글자 통째로 비교하는 대신, 몇 번째 질문을 선택했는지 '위치 인덱스 번호'로 안전하게 비교합니다!
        int selectedQuestionPos = spinnerQuestion.getSelectedItemPosition();
        String inputAnswer = etAnswer.getText().toString().trim();

        // [방어 코드] 빈 입력값 검사
        if (TextUtils.isEmpty(inputNickname) || TextUtils.isEmpty(inputPhone) || TextUtils.isEmpty(inputAnswer)) {
            Toast.makeText(this, "모든 보안 질문 정보와 칸을 채워주세요! 🐹", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🌟 [실전 연동 완료] 가입창(RegisterActivity)에서 백업해 둔 진짜 유저 데이터 불러오기
        android.content.SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
        String savedNickname = prefs.getString("user_nickname", "");
        String savedPhone = prefs.getString("user_phone", "");
        int savedQuestionPos = prefs.getInt("user_question_pos", -1); // 저장된 질문 번호 (없으면 -1)
        String savedAnswer = prefs.getString("user_answer", "");
        String savedPassword = prefs.getString("user_password", "비밀번호 오류"); // 가입할 때 쓴 진짜 비밀번호

        // 🌟 가짜 예시 데이터는 안녕! '방금 내가 회원가입창에 입력했던 정보'와 완벽 대조합니다.
        if (inputNickname.equals(savedNickname) &&
                inputPhone.equals(savedPhone) &&
                selectedQuestionPos == savedQuestionPos &&  // 질문 종류 일치 확인
                inputAnswer.equals(savedAnswer)) {

            // 4개 보안 관문을 모두 정확하게 통과하면 가입 데이터에 살아있는 진짜 패스워드를 출력!
            tvFindPwResult.setText("회원님의 비밀번호는\n👉 [ " + savedPassword + " ] 👈 입니다. 🐹");
            tvFindPwResult.setVisibility(View.VISIBLE);

        } else {
            // 정보가 단 한 글자라도 삐끗하면 본인 방어 안내창 출력
            tvFindPwResult.setText("보안 질문 정보 혹은 답이 일치하지 않습니다. 😥");
            tvFindPwResult.setVisibility(View.VISIBLE);
        }
    }
}