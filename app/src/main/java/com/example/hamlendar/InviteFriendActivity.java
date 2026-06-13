package com.example.hamlendar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class InviteFriendActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private LinearLayout layoutProfileContainer;
    private LinearLayout layoutFriendSchedules;
    private TextView tvFriendDiarySummary;
    private TextView tvTargetName;

    private String todayStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friend);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        todayStr = LocalDate.now().toString(); // "2026-06-14" 형태

        layoutProfileContainer = findViewById(R.id.layoutProfileContainer);
        layoutFriendSchedules = findViewById(R.id.layoutFriendSchedules);
        tvFriendDiarySummary = findViewById(R.id.tvFriendDiarySummary);
        tvTargetName = findViewById(R.id.tvTargetName);

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🌟 [나래님 도면 연동] 정적으로 박아둔 [+] 버튼에 이메일 초대 팝업 연결!
        FrameLayout circleFrame = findViewById(R.id.circleFrame);
        if (circleFrame != null) {
            circleFrame.setOnClickListener(v -> showInviteEmailDialog());
        }

        // 내 전용 프로필 서클(첫 번째 FrameLayout) 클릭 시 내 데이터 로드 리스너 부여
        FrameLayout plusFrame = findViewById(R.id.plusFrame);
        if (plusFrame != null) {
            plusFrame.setOnClickListener(v -> loadUserDailyData(currentUser.getUid(), "나", "close_friend"));
        }

        // 초기 화면 구동 시 데이터 배치 수행
        loadAcceptedFriendsAndRender();
        loadUserDailyData(currentUser.getUid(), "나", "close_friend");
    }

    // 🟢 1. [나]와 [+] 사이에만 친구 원형 이미지를 동적으로 비집고 밀어 넣는 핵심 렌더러
    private void loadAcceptedFriendsAndRender() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).collection("friends").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (layoutProfileContainer == null) return;

                    // 1. 순정 상태 원상 복구 (나와 +만 남기고 사이에 낀 옛날 뷰들 소거)
                    while (layoutProfileContainer.getChildCount() > 2) {
                        layoutProfileContainer.removeViewAt(1);
                    }

                    float density = getResources().getDisplayMetrics().density;
                    int circleSize = (int) (80 * density);
                    int marginSize = (int) (16 * density);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String friendUid = doc.getId();
                        String friendName = doc.getString("name");
                        String status = doc.getString("status");
                        String tier = doc.getString("tier");

                        // 오직 관계 설정이 정상 수락("accepted") 또는 내게 온 요청("received")인 유저만 링에 배치
                        if ("accepted".equals(status) || "received".equals(status)) {

                            FrameLayout friendFrame = new FrameLayout(this);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(circleSize, circleSize);
                            params.setMargins(marginSize, 0, 0, 0); // 16dp 간격 배치 규칙 준수
                            friendFrame.setLayoutParams(params);

                            // 초록색 원형 프로필 테두리 장식 입히기
                            GradientDrawable strokeDrawable = new GradientDrawable();
                            strokeDrawable.setShape(GradientDrawable.OVAL);
                            strokeDrawable.setStroke(5, Color.parseColor("#4CAF50")); // 메인 초록색

                            // 🟢 친구가 추가되면 프로필 사진으로 초록 원 내부를 완벽 채우기
                            ImageView imgProfile = new ImageView(this);
                            FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(circleSize, circleSize);
                            imgProfile.setLayoutParams(imgParams);
                            imgProfile.setImageResource(R.drawable.hamicon); // 🐹 친구 전용 프로필 이미지 뷰포트 배치
                            imgProfile.setScaleType(ImageView.ScaleType.FIT_CENTER);

                            if ("received".equals(status)) {
                                strokeDrawable.setStroke(5, Color.parseColor("#FF9800")); // 대기자는 오렌지 경고색 테두리
                            } else if ("close_friend".equals(tier)) {
                                strokeDrawable.setColor(Color.parseColor("#E8F5E9")); // 친한 친구는 부드러운 초록색 배경 음영 보너스
                            }
                            imgProfile.setBackground(strokeDrawable);
                            friendFrame.addView(imgProfile);

                            // 🌟 핵심 인덱스 밀어내기 기법 적용 (언제나 [+] 바로 앞자리에 주입)
                            int insertIndex = layoutProfileContainer.getChildCount() - 1;
                            layoutProfileContainer.addView(friendFrame, insertIndex);

                            // 프로필 터치 클릭 액션 설정
                            friendFrame.setOnClickListener(v -> {
                                if ("received".equals(status)) {
                                    showAcceptDialog(friendUid, friendName);
                                } else {
                                    // 일반 수락 상태라면 하단에 일정 및 일기 즉각 분기 로드 후 등급 편집 팝업 출력
                                    loadUserDailyData(friendUid, friendName, tier);
                                    showTierToggleDialog(friendUid, friendName, tier);
                                }
                            });
                        }
                    }
                });
    }

    // 🟢 2. 중앙 이메일 탐색 및 초대 팝업창
    private void showInviteEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📨 햄린더 친구 초대");

        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("친구의 이메일을 입력하세요");
        inputEmail.setPadding(40, 40, 40, 40);
        builder.setView(inputEmail);

        builder.setPositiveButton("초대 전송", (dialog, which) -> {
            String email = inputEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) return;

            if (email.equals(currentUser.getEmail())) {
                Toast.makeText(this, "자기 자신은 초대할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users").whereEqualTo("email", email).get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(InviteFriendActivity.this, "가입되지 않은 이메일입니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DocumentSnapshot targetUser = queryDocumentSnapshots.getDocuments().get(0);
                        String targetUid = targetUser.getId();
                        String targetName = targetUser.getString("name");

                        Map<String, Object> myRequest = new HashMap<>();
                        myRequest.put("email", email);
                        myRequest.put("name", targetName != null ? targetName : "사용자");
                        myRequest.put("status", "sent");
                        myRequest.put("tier", "friend");

                        Map<String, Object> targetRequest = new HashMap<>();
                        targetRequest.put("email", currentUser.getEmail());
                        targetRequest.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "새로운 친구");
                        targetRequest.put("status", "received");
                        targetRequest.put("tier", "friend");

                        db.collection("users").document(currentUser.getUid()).collection("friends").document(targetUid).set(myRequest);
                        db.collection("users").document(targetUid).collection("friends").document(currentUser.getUid()).set(targetRequest)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(InviteFriendActivity.this, "초대를 전송했습니다!", Toast.LENGTH_SHORT).show();
                                    loadAcceptedFriendsAndRender();
                                });
                    });
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    // 🟢 3. 친구 수락 제어 다이얼로그
    private void showAcceptDialog(String friendUid, String friendName) {
        new AlertDialog.Builder(this)
                .setTitle("친구 초대가 와있습니다")
                .setMessage(friendName + "님의 초대를 수락하시겠습니까?")
                .setNegativeButton("거절", null)
                .setPositiveButton("수락", (dialog, which) -> {
                    db.collection("users").document(currentUser.getUid()).collection("friends").document(friendUid).update("status", "accepted");
                    db.collection("users").document(friendUid).collection("friends").document(currentUser.getUid()).update("status", "accepted")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(InviteFriendActivity.this, "이제 친구 관계입니다!", Toast.LENGTH_SHORT).show();
                                loadAcceptedFriendsAndRender();
                            });
                }).show();
    }

    // 🟢 4. 등급 설정 토글 다이얼로그 (일반 친구 ↔ 친한 친구)
    private void showTierToggleDialog(String friendUid, String friendName, String currentTier) {
        String menuOption = "close_friend".equals(currentTier) ? "일반 친구로 변경 (일기 숨기기)" : "⭐ 친한 친구로 지정 (일기 공유)";
        new AlertDialog.Builder(this)
                .setTitle(friendName + "님 권한 설정")
                .setItems(new String[]{menuOption, "취소"}, (dialog, which) -> {
                    if (which == 0) {
                        String nextTier = "close_friend".equals(currentTier) ? "friend" : "close_friend";
                        db.collection("users").document(currentUser.getUid()).collection("friends").document(friendUid)
                                .update("tier", nextTier)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(InviteFriendActivity.this, "권한 변경 완료!", Toast.LENGTH_SHORT).show();
                                    loadAcceptedFriendsAndRender();
                                });
                    }
                }).show();
    }

    // 🟢 5. [핵심 지표] 선택된 인물의 데이터 연동 및 권한 분기 필터링 (화면 하단 출력 영역)
    // 💡 InviteFriendActivity.java의 맨 아래 loadUserDailyData 메서드를 이 코드로 덮어쓰기 해주세요!

    private void loadUserDailyData(String uid, String name, String tier) {
        if (tvTargetName == null || layoutFriendSchedules == null || tvFriendDiarySummary == null) return;

        tvTargetName.setText(name + "님의 오늘 하루");
        layoutFriendSchedules.removeAllViews();
        tvFriendDiarySummary.setText("작성된 일기가 없거나 오픈 권한이 제한되어 있습니다.");

        // 🌟 [핵심 수정] 일기장 전용 한글 포맷 생성 (예: "6월 14일 (일)")
        java.time.format.DateTimeFormatter diaryFormatter =
                java.time.format.DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN);
        String diaryTodayStr = LocalDate.now().format(diaryFormatter);

        // [A. 일정 로드] - 일정은 기존대로 "2026-06-14" (todayStr)로 조회
        db.collection("users").document(uid).collection("schedules")
                .whereEqualTo("date", todayStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("오늘 잡힌 일정이 없습니다.");
                        tvEmpty.setTextColor(Color.GRAY);
                        layoutFriendSchedules.addView(tvEmpty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TextView tvSchedule = new TextView(this);
                        tvSchedule.setText("• " + doc.getString("title"));
                        tvSchedule.setTextSize(14);
                        tvSchedule.setPadding(0, 10, 0, 10);
                        tvSchedule.setTextColor(Color.BLACK);
                        layoutFriendSchedules.addView(tvSchedule);
                    }
                });

        // [B. 일기 요약 제어 분기] - 한글 날짜 이름표(diaryTodayStr)를 사용하여 매칭 성공!
        if (uid.equals(currentUser.getUid())) {
            // 내 일기는 한글 이름표로 로컬 SharedPreferences에서 가져옴
            String localSummary = getSharedPreferences("diary_pref", MODE_PRIVATE).getString("summary_" + diaryTodayStr, "");
            if (!localSummary.isEmpty()) {
                tvFriendDiarySummary.setText(localSummary);
            } else {
                tvFriendDiarySummary.setText("오늘 작성된 일기 요약이 없습니다.");
            }
        } else {
            // 친구의 일기는 상대방이 파이어베이스에 올린 한글 이름표 도큐먼트를 조회
            db.collection("users").document(uid).collection("friends").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && "close_friend".equals(documentSnapshot.getString("tier"))) {

                            // 파이어베이스 도큐먼트 ID도 한글 날짜 이름표(diaryTodayStr)로 매칭!
                            db.collection("users").document(uid).collection("summaries").document(diaryTodayStr).get()
                                    .addOnSuccessListener(diaryDoc -> {
                                        if (diaryDoc.exists() && diaryDoc.getString("summary") != null) {
                                            tvFriendDiarySummary.setText(diaryDoc.getString("summary"));
                                        } else {
                                            tvFriendDiarySummary.setText("친한 친구 관계이나 상대방이 오늘 일기를 작성하지 않았습니다.");
                                        }
                                    });
                        } else {
                            tvFriendDiarySummary.setText("🔒 상대방의 '친한 친구' 등급에게만 투명하게 공개되는 비밀 일기입니다.");
                        }
                    });
        }

    }
}