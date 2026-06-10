package com.example.hamlendar;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class InviteFriendActivity extends AppCompatActivity {

    private LinearLayout layoutProfileContainer;
    private FrameLayout btnAddFriend; // + 버튼
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friend);

        layoutProfileContainer = findViewById(R.id.layoutProfileContainer);
        btnAddFriend = findViewById(R.id.circleFrame);

        // 🌟 플러스(+) 버튼 클릭 시 이메일 입력 다이얼로그 띄우기
        btnAddFriend.setOnClickListener(v -> showInviteDialog());
    }

    private void showInviteDialog() {
        final EditText etEmail = new EditText(this);
        etEmail.setHint("친구의 이메일을 입력하세요");

        new AlertDialog.Builder(this)
                .setTitle("햄구 초대하기 🐹")
                .setMessage("친구의 이메일 주소를 정확히 적어주세요.")
                .setView(etEmail)
                .setPositiveButton("초대", (dialog, which) -> {
                    String email = etEmail.getText().toString().trim();
                    if (!email.isEmpty()) {
                        searchFriendFromFirebase(email);
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 🌟 파이어베이스에서 이메일로 친구 찾기
    private void searchFriendFromFirebase(String email) {
        db.collection("users").whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // 친구를 찾았을 때 닉네임 가져오기 (가입할 때 et_nickname으로 저장한 필드명)
                        String friendNickname = queryDocumentSnapshots.getDocuments().get(0).getString("nickname");
                        if (friendNickname == null) friendNickname = "햄구";

                        // 🌟 성공! 나랑 + 버튼 사이에 친구 프로필 원 쏙 집어넣기
                        addFriendCircleView(friendNickname);
                        Toast.makeText(this, friendNickname + "님이 초대되었습니다! 🎉", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "존재하지 않는 사용자 이메일입니다. 😥", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // 🌟 가로 상자(LinearLayout) 안에 새로운 원형 뷰 동적으로 꽂아주는 치트키 로직
    private void addFriendCircleView(String nickname) {
        // 1. 원형 배경을 가진 FrameLayout 새내기 만들기
        FrameLayout friendFrame = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(80), // 가로 80dp
                dpToPx(80)  // 세로 80dp
        );
        params.setMarginStart(dpToPx(16)); // 기존 마진 유지용 16dp 간격
        friendFrame.setLayoutParams(params);
        friendFrame.setBackgroundResource(R.drawable.bg_green_circle); // 연두색 테두리 배경 장착!

        // 2. 그 안에 들어갈 닉네임 텍스트뷰 만들기
        TextView tvNickname = new TextView(this);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER; // 중앙 정렬
        tvNickname.setLayoutParams(textParams);
        tvNickname.setText(nickname);
        tvNickname.setTextColor(android.graphics.Color.BLACK);
        tvNickname.setTextSize(14);
        tvNickname.setTypeface(null, android.graphics.Typeface.BOLD);

        // 3. 결합하기
        friendFrame.addView(tvNickname);

        // 4. 🌟 핵심 치트키: 전체 컨테이너의 맨 마지막에서 바로 직전 칸(즉, + 버튼 바로 왼쪽 자리)에 쏙 삽입!
        int plusButtonIndex = layoutProfileContainer.indexOfChild(btnAddFriend);
        layoutProfileContainer.addView(friendFrame, plusButtonIndex);
    }

    // dp 단위를 px 단위로 안전하게 변경해 주는 변환기 함수
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}