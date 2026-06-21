package com.example.hamlendar;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
    private LinearLayout layoutFriendDiary;

    private String todayStr;
    private String diaryTodayStr;
    private ListenerRegistration friendsListener;

    // 🌟 [핵심 3] 친구 초대 창의 "나의 오늘 하루" 요약도 내 개인 금고에서 꺼내옵니다!
    private String getDiaryPrefName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && user.getEmail() != null ? "diary_pref_" + user.getEmail() : "diary_pref";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friend);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        todayStr = LocalDate.now().toString();
        java.time.format.DateTimeFormatter diaryFormatter =
                java.time.format.DateTimeFormatter.ofPattern("M월 d일 (E)", java.util.Locale.KOREAN);
        diaryTodayStr = LocalDate.now().format(diaryFormatter);

        layoutProfileContainer = findViewById(R.id.layoutProfileContainer);
        layoutFriendSchedules = findViewById(R.id.layoutFriendSchedules);
        tvFriendDiarySummary = findViewById(R.id.tvFriendDiarySummary);
        tvTargetName = findViewById(R.id.tvTargetName);
        layoutFriendDiary = findViewById(R.id.layoutFriendDiary);

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FrameLayout circleFrame = findViewById(R.id.circleFrame);
        if (circleFrame != null) {
            circleFrame.setOnClickListener(v -> showInviteEmailDialog());
        }

        FrameLayout plusFrame = findViewById(R.id.plusFrame);
        if (plusFrame != null) {
            plusFrame.setOnClickListener(v -> {
                Toast.makeText(this, "나의 오늘 하루를 보고 계십니다! 🐹", Toast.LENGTH_SHORT).show();
            });
        }

        loadMyDailyData();
        loadAcceptedFriendsAndRender();
    }

    private void loadMyDailyData() {
        if (tvTargetName != null) tvTargetName.setText("나의 오늘 하루");
        if (layoutFriendSchedules == null || tvFriendDiarySummary == null) return;

        layoutFriendSchedules.removeAllViews();

        String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();

        db.collection("users").document(myEmailKey).collection("schedules")
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

        String localSummary = getSharedPreferences(getDiaryPrefName(), MODE_PRIVATE).getString("summary_" + diaryTodayStr, "");
        if (!localSummary.isEmpty()) {
            tvFriendDiarySummary.setText(localSummary);
        } else {
            tvFriendDiarySummary.setText("오늘 작성된 내 일기 요약이 없습니다.");
        }
    }

    private void loadAcceptedFriendsAndRender() {
        if (currentUser == null) return;

        String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();

        friendsListener = db.collection("users").document(myEmailKey).collection("friends")
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) return;
                    if (queryDocumentSnapshots == null || layoutProfileContainer == null || layoutFriendDiary == null) return;

                    while (layoutProfileContainer.getChildCount() > 2) {
                        layoutProfileContainer.removeViewAt(1);
                    }
                    layoutFriendDiary.removeAllViews();

                    float density = getResources().getDisplayMetrics().density;
                    int circleSize = (int) (80 * density);
                    int marginSize = (int) (16 * density);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String friendEmail = doc.getId();
                        String friendName = doc.getString("name");
                        String status = doc.getString("status");
                        String tier = doc.getString("tier");
                        String friendProfileUriStr = doc.getString("friend_profile_uri");

                        if (friendEmail.equalsIgnoreCase(myEmailKey)) continue;

                        if ("accepted".equals(status) || "received".equals(status)) {

                            FrameLayout friendFrame = new FrameLayout(this);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(circleSize, circleSize);
                            params.setMargins(marginSize, 0, 0, 0);
                            friendFrame.setLayoutParams(params);

                            GradientDrawable strokeDrawable = new GradientDrawable();
                            strokeDrawable.setShape(GradientDrawable.OVAL);
                            strokeDrawable.setStroke(5, Color.parseColor("#4CAF50"));

                            ImageView imgProfile = new ImageView(this);
                            FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(circleSize, circleSize);
                            imgProfile.setLayoutParams(imgParams);
                            imgProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            if ("received".equals(status)) {
                                strokeDrawable.setStroke(5, Color.parseColor("#FF9800"));
                            } else if ("close_friend".equals(tier)) {
                                strokeDrawable.setColor(Color.parseColor("#E8F5E9"));
                            }
                            imgProfile.setBackground(strokeDrawable);

                            if (!TextUtils.isEmpty(friendProfileUriStr)) {
                                Glide.with(InviteFriendActivity.this)
                                        .load(Uri.parse(friendProfileUriStr))
                                        .circleCrop()
                                        .placeholder(R.drawable.hamicon)
                                        .error(R.drawable.hamicon)
                                        .into(imgProfile);
                            } else {
                                imgProfile.setImageResource(R.drawable.hamicon);
                            }

                            TextView tvNick = new TextView(this);
                            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                            textParams.gravity = Gravity.CENTER;
                            tvNick.setLayoutParams(textParams);
                            tvNick.setText(friendName != null ? friendName : friendEmail);
                            tvNick.setTextColor(Color.BLACK);
                            tvNick.setTextSize(12);

                            friendFrame.addView(imgProfile);
                            friendFrame.addView(tvNick);

                            int insertIndex = layoutProfileContainer.getChildCount() - 1;
                            layoutProfileContainer.addView(friendFrame, insertIndex);

                            friendFrame.setOnClickListener(v -> {
                                if ("received".equals(status)) {
                                    showAcceptDialog(friendEmail, tvNick.getText().toString());
                                } else {
                                    showFriendManagementDialog(friendEmail, tvNick.getText().toString(), tier);
                                }
                            });

                            if ("accepted".equals(status)) {
                                createFriendFeedDynamicView(friendEmail, tvNick.getText().toString(), tier);
                            }
                        }
                    }
                });
    }

    private void createFriendFeedDynamicView(String friendEmail, String friendName, String tier) {
        float density = getResources().getDisplayMetrics().density;

        LinearLayout friendSection = new LinearLayout(this);
        friendSection.setOrientation(LinearLayout.VERTICAL);
        friendSection.setPadding(0, (int) (20 * density), 0, 0);

        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * density));
        divParams.setMargins(0, 0, 0, (int) (16 * density));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#DDDDDD"));
        friendSection.addView(divider);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(friendName + "님의 오늘 하루");
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(Color.BLACK);
        tvTitle.setPadding(0, 0, 0, (int) (12 * density));
        friendSection.addView(tvTitle);

        TextView tvSubSchedule = new TextView(this);
        tvSubSchedule.setText("📅 친구의 오늘 일정");
        tvSubSchedule.setTextSize(14);
        tvSubSchedule.setTextColor(Color.BLACK);
        tvSubSchedule.setPadding(0, 0, 0, (int) (6 * density));
        friendSection.addView(tvSubSchedule);

        LinearLayout scheduleBox = new LinearLayout(this);
        scheduleBox.setOrientation(LinearLayout.VERTICAL);
        scheduleBox.setBackgroundResource(R.drawable.edittext_box);
        scheduleBox.setBackgroundColor(Color.WHITE);
        scheduleBox.setElevation(1 * density);
        scheduleBox.setPadding((int) (14 * density), (int) (14 * density), (int) (14 * density), (int) (14 * density));
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 0, 0, (int) (16 * density));
        scheduleBox.setLayoutParams(boxParams);
        friendSection.addView(scheduleBox);

        db.collection("users").document(friendEmail).collection("schedules")
                .whereEqualTo("date", todayStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("오늘 잡힌 친구의 일정이 없습니다.");
                        tvEmpty.setTextColor(Color.GRAY);
                        scheduleBox.addView(tvEmpty);
                        return;
                    }
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TextView tvSchedule = new TextView(this);
                        tvSchedule.setText("• " + doc.getString("title"));
                        tvSchedule.setTextSize(14);
                        tvSchedule.setPadding(0, 8, 0, 8);
                        tvSchedule.setTextColor(Color.BLACK);
                        scheduleBox.addView(tvSchedule);
                    }
                });

        TextView tvSubDiary = new TextView(this);
        tvSubDiary.setText("✨ 친구의 AI 일기 요약");
        tvSubDiary.setTextSize(14);
        tvSubDiary.setTextColor(Color.BLACK);
        tvSubDiary.setPadding(0, 0, 0, (int) (6 * density));
        friendSection.addView(tvSubDiary);

        LinearLayout diaryBox = new LinearLayout(this);
        diaryBox.setOrientation(LinearLayout.VERTICAL);
        diaryBox.setBackgroundResource(R.drawable.edittext_box);
        diaryBox.setBackgroundColor(Color.parseColor("#F2F9F6"));
        diaryBox.setElevation(1 * density);
        diaryBox.setPadding((int) (14 * density), (int) (14 * density), (int) (14 * density), (int) (14 * density));
        diaryBox.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvDiarySummary = new TextView(this);
        tvDiarySummary.setText("🔒 상대방이 나를 '친한 친구'로 지정해야 볼 수 있는 비밀 일기입니다.");
        tvDiarySummary.setTextColor(Color.BLACK);
        tvDiarySummary.setTextSize(13);
        diaryBox.addView(tvDiarySummary);
        friendSection.addView(diaryBox);

        String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();

        // 🌟 [수정 완료] 상대방(friendEmail)의 친구 목록에서 나(myEmailKey)의 등급이 '친한 친구'인지 확인!
        db.collection("users").document(friendEmail).collection("friends").document(myEmailKey).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && "close_friend".equals(documentSnapshot.getString("tier"))) {
                        db.collection("users").document(friendEmail).collection("summaries").document(diaryTodayStr).get()
                                .addOnSuccessListener(diaryDoc -> {
                                    if (diaryDoc.exists() && diaryDoc.getString("summary") != null) {
                                        tvDiarySummary.setText(diaryDoc.getString("summary"));
                                    } else {
                                        tvDiarySummary.setText("친한 친구 관계이나 상대방이 오늘 일기를 작성하지 않았습니다.");
                                    }
                                });
                    } else {
                        tvDiarySummary.setText("🔒 상대방이 나를 '친한 친구'로 지정해야 볼 수 있는 비밀 일기입니다.");
                    }
                });

        layoutFriendDiary.addView(friendSection);
    }

    private void showInviteEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📨 햄린더 친구 초대");

        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("친구의 이메일을 입력하세요");
        inputEmail.setPadding(40, 40, 40, 40);
        builder.setView(inputEmail);

        builder.setPositiveButton("초대 전송", (dialog, which) -> {
            String targetEmail = inputEmail.getText().toString().trim();
            if (TextUtils.isEmpty(targetEmail)) return;

            if (targetEmail.equalsIgnoreCase(currentUser.getEmail())) {
                Toast.makeText(this, "자기 자신은 초대할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();

            SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
            String myNickname = prefs.getString("user_name", "새로운 친구");
            String myProfileUriStr = prefs.getString("user_profile_uri", "");

            Map<String, Object> myRequest = new HashMap<>();
            myRequest.put("email", targetEmail);
            myRequest.put("name", targetEmail);
            myRequest.put("status", "sent");
            myRequest.put("tier", "friend");

            Map<String, Object> targetRequest = new HashMap<>();
            targetRequest.put("email", myEmailKey);
            targetRequest.put("name", myNickname);
            targetRequest.put("status", "received");
            targetRequest.put("tier", "friend");

            if (!TextUtils.isEmpty(myProfileUriStr)) {
                targetRequest.put("friend_profile_uri", myProfileUriStr);
            }

            db.collection("users").document(myEmailKey).collection("friends").document(targetEmail).set(myRequest);
            db.collection("users").document(targetEmail).collection("friends").document(myEmailKey).set(targetRequest)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(InviteFriendActivity.this, "🎉 초대를 성공적으로 전송했습니다!", Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void showAcceptDialog(String friendEmail, String friendName) {
        String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();

        new AlertDialog.Builder(this)
                .setTitle("친구 초대가 와있습니다")
                .setMessage(friendName + "님의 초대를 수락하시겠습니까?")
                .setNegativeButton("거절", null)
                .setPositiveButton("수락", (dialog, which) -> {

                    SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
                    String myNickname = prefs.getString("user_name", "새로운 친구");
                    String myProfileUriStr = prefs.getString("user_profile_uri", "");

                    Map<String, Object> friendUpdates = new HashMap<>();
                    friendUpdates.put("status", "accepted");
                    friendUpdates.put("name", myNickname);
                    if (!TextUtils.isEmpty(myProfileUriStr)) {
                        friendUpdates.put("friend_profile_uri", myProfileUriStr);
                    }

                    Map<String, Object> myUpdates = new HashMap<>();
                    myUpdates.put("status", "accepted");

                    if (!TextUtils.isEmpty(friendName) && !friendName.contains("@")) {
                        myUpdates.put("name", friendName);
                    }

                    db.collection("users").document(myEmailKey).collection("friends").document(friendEmail).update(myUpdates);
                    db.collection("users").document(friendEmail).collection("friends").document(myEmailKey).update(friendUpdates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(InviteFriendActivity.this, "🤝 이제 친구 관계입니다!", Toast.LENGTH_SHORT).show();
                            });
                }).show();
    }

    private void showFriendManagementDialog(String friendEmail, String friendName, String currentTier) {
        String menuOption = "close_friend".equals(currentTier) ? "일반 친구로 변경" : "⭐ 친한 친구로 지정 (일기 공유)";
        String[] items = {menuOption, "❌ 친구 삭제하기", "취소"};

        new AlertDialog.Builder(this)
                .setTitle(friendName + "님 권한 및 관계 설정")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        String nextTier = "close_friend".equals(currentTier) ? "friend" : "close_friend";
                        String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();
                        db.collection("users").document(myEmailKey).collection("friends").document(friendEmail)
                                .update("tier", nextTier)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(InviteFriendActivity.this, "권한 변경 완료!", Toast.LENGTH_SHORT).show();
                                });
                    } else if (which == 1) {
                        showRealDeleteConfirmDialog(friendEmail, friendName);
                    }
                }).show();
    }

    private void showRealDeleteConfirmDialog(String friendEmail, String friendName) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ 진짜로 삭제하시겠습니까?")
                .setMessage(friendName + "님과의 모든 친구 관계, 오늘 일정, 일기장 공유가 즉시 차단되며 목록에서 사라집니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제 확정", (dialog, which) -> {
                    String myEmailKey = currentUser.getEmail() != null ? currentUser.getEmail() : currentUser.getUid();
                    db.collection("users").document(myEmailKey).collection("friends").document(friendEmail).delete();
                    db.collection("users").document(friendEmail).collection("friends").document(myEmailKey).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(InviteFriendActivity.this, "친구 관계가 완전히 철회되었습니다. 햄!", Toast.LENGTH_SHORT).show();
                            });
                }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendsListener != null) {
            friendsListener.remove();
        }
    }
}