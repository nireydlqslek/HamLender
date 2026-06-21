package com.example.hamlendar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etBirth, etNickname, etEmail, etPassword, etPasswordConfirm, etAnswer;
    private Spinner spinnerEmailDomain, spinnerQuestion;
    private Button registerBtn;
    private TextView tvGotoLogin;
    private FrameLayout profileFrame;
    private ImageView profileImageView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedDomain = "";
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri originalUri = result.getData().getData();
                    if (originalUri != null) {
                        Uri localUri = copyUriToInternalStorage(originalUri);
                        if (localUri != null) {
                            selectedImageUri = localUri;
                            profileImageView.setImageURI(selectedImageUri);
                        } else {
                            Toast.makeText(this, "이미지 복사에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etBirth = findViewById(R.id.et_birth);
        etNickname = findViewById(R.id.et_nickname);
        etEmail = findViewById(R.id.et_email);
        spinnerEmailDomain = findViewById(R.id.spinner_email_domain);
        etPassword = findViewById(R.id.et_password);
        etPasswordConfirm = findViewById(R.id.et_password_confirm);
        spinnerQuestion = findViewById(R.id.spinner_question);
        etAnswer = findViewById(R.id.et_answer);
        registerBtn = findViewById(R.id.register_btn);
        tvGotoLogin = findViewById(R.id.gotologin);
        profileFrame = findViewById(R.id.profileFrame);
        profileImageView = findViewById(R.id.imageView3);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        profileFrame.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        tvGotoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        String[] domains = {"직접 입력", "naver.com", "gmail.com", "daum.net", "hanmail.net"};
        ArrayAdapter<String> domainAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, domains);
        spinnerEmailDomain.setAdapter(domainAdapter);
        spinnerEmailDomain.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDomain = position == 0 ? "" : domains[position];
                checkInputFields();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] securityQuestions = {"애완동물의 이름은 ?", "가장 좋아하는 캐릭터 이름은?", "제일 좋아하는 책 이름은 ?"};
        ArrayAdapter<String> questionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, securityQuestions);
        spinnerQuestion.setAdapter(questionAdapter);

        setupTextWatchers();
        registerBtn.setOnClickListener(v -> register());
    }

    private Uri copyUriToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // 🌟 [최후의 방어선] 사용자가 이메일을 치기 전에 사진을 눌러도 절대 겹치지 않게, '현재 시간(0.001초)'을 파일 이름에 박아버립니다!
            String uniqueName = "profile_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getFilesDir(), uniqueName);

            OutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupTextWatchers() {
        TextWatcher inputWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { checkInputFields(); }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etName.addTextChangedListener(inputWatcher);
        etPhone.addTextChangedListener(inputWatcher);
        etBirth.addTextChangedListener(inputWatcher);
        etNickname.addTextChangedListener(inputWatcher);
        etEmail.addTextChangedListener(inputWatcher);
        etPassword.addTextChangedListener(inputWatcher);
        etPasswordConfirm.addTextChangedListener(inputWatcher);
        etAnswer.addTextChangedListener(inputWatcher);
    }

    private void checkInputFields() {
        boolean isAllFilled = !TextUtils.isEmpty(etName.getText()) && !TextUtils.isEmpty(etPhone.getText()) &&
                !TextUtils.isEmpty(etBirth.getText()) && !TextUtils.isEmpty(etNickname.getText()) &&
                !TextUtils.isEmpty(etEmail.getText()) && !TextUtils.isEmpty(etPassword.getText()) &&
                !TextUtils.isEmpty(etPasswordConfirm.getText()) && !TextUtils.isEmpty(etAnswer.getText());

        if (isAllFilled) registerBtn.setText("회원가입");
        else registerBtn.setText("모든 정보가 입력되지 않았습니다");
    }

    private void register() {
        String userName = etName.getText().toString().trim();
        String userPassword = etPassword.getText().toString().trim();
        String confirmPassword = etPasswordConfirm.getText().toString().trim();
        String userNickname = etNickname.getText().toString().trim();
        String emailId = etEmail.getText().toString().trim();
        String answer = etAnswer.getText().toString().trim();

        String fullEmail = emailId;
        if (!TextUtils.isEmpty(selectedDomain)) fullEmail = emailId + "@" + selectedDomain;

        if (TextUtils.isEmpty(fullEmail) || TextUtils.isEmpty(userPassword) ||
                TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(userName) || TextUtils.isEmpty(answer)) {
            Toast.makeText(this, "필수 정보를 모두 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!userPassword.equals(confirmPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalFullEmail = fullEmail;
        mAuth.createUserWithEmailAndPassword(fullEmail, userPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String displayName = TextUtils.isEmpty(userNickname) ? userName : userNickname;
                        saveUserName(displayName, finalFullEmail);
                    } else {
                        String message = task.getException() == null ? "회원가입 실패" : task.getException().getMessage();
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserName(String displayName, String fullEmail) {
        String realName = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String birth = etBirth.getText().toString().trim().replace(".", "");
        String nickname = etNickname.getText().toString().trim();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", fullEmail);
        userData.put("user_name", displayName);
        userData.put("real_name", realName);
        userData.put("user_phone", phone);
        userData.put("user_birth", birth);
        userData.put("user_nickname", nickname);
        if (selectedImageUri != null) {
            userData.put("user_profile_uri", selectedImageUri.toString());
        }

        db.collection("users").document(fullEmail).set(userData);

        SharedPreferences prefs = getSharedPreferences("user_pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_name", displayName);
        editor.putString("real_name", realName);
        editor.putString("user_phone", phone);
        editor.putString("user_birth", birth);
        editor.putString("user_email_id", fullEmail);
        editor.putString("user_nickname", nickname);
        editor.putString("user_password", etPassword.getText().toString().trim());
        editor.putInt("user_question_pos", spinnerQuestion.getSelectedItemPosition());
        editor.putString("user_answer", etAnswer.getText().toString().trim());

        if (selectedImageUri != null) {
            editor.putString("user_profile_uri", selectedImageUri.toString());
        }
        editor.apply();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            moveToLogin();
            return;
        }

        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName);

        if (selectedImageUri != null) {
            profileBuilder.setPhotoUri(selectedImageUri);
        }

        user.updateProfile(profileBuilder.build())
                .addOnCompleteListener(profileTask -> {
                    Toast.makeText(this, "회원가입 성공! 🐹", Toast.LENGTH_SHORT).show();
                    moveToLogin();
                });
    }

    private void moveToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}