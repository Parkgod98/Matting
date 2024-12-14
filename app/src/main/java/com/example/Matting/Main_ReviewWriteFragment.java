package com.example.Matting;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main_ReviewWriteFragment extends Fragment {
    private EditText etReviewContent;
    private Button btnSubmit;
    private ImageButton btnClose;
    private TextView tvRestaurant;
    private String address, email, username;
    private RatingBar ratingBar;
    private ImageView placeholderImage;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private LinearLayout imageUploadButton;
    private User user;
    private FirebaseAuth mAuth;

    @SuppressLint("WrongViewCast")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.review_fragment, container, false);

        // FirebaseAuth 객체 생성
        mAuth = FirebaseAuth.getInstance();
        user = new User(getActivity());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // 로그인되지 않은 경우, 로그인 페이지로 이동
            Intent loginIntent = new Intent(getActivity(), User_LoginActivity.class);
            startActivity(loginIntent);
        } else {
            // 로그인된 사용자 정보 사용
            Log.d("nickname", user.getUserId());
            email = user.getEmail();
//            String userId = currentUser.getUid();
            if (email != null) {
                username = extractIdFromEmail(email);
            }
        }

        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnClose = view.findViewById(R.id.btnClose);
        tvRestaurant = view.findViewById(R.id.tvRestaurant);
        etReviewContent = view.findViewById(R.id.etReviewContent);
        ratingBar = view.findViewById(R.id.ratingBar);
        imageUploadButton = view.findViewById(R.id.imageUploadButton);
        placeholderImage = view.findViewById(R.id.placeholderImage);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        placeholderImage.setImageURI(imageUri); // 선택한 이미지 표시
                    }
                }
        );

        // 전달받은 데이터 수신
        Bundle arguments = getArguments();
        if (arguments != null) {
            String restaurant = arguments.getString("restaurant");
            address = arguments.getString("address");
            if (restaurant != null) {
                tvRestaurant.setText(restaurant); // 전달받은 title 값을 EditText에 설정
            }
        }

        // 버튼 클릭 이벤트 설정
        imageUploadButton.setOnClickListener(v -> openGallery());

        btnSubmit.setOnClickListener(v -> {
            String content = etReviewContent.getText().toString().trim();
            float rating = ratingBar.getRating();

            if (validateInputs(content)) {
                if (imageUri != null) {
                    String encodedImage = encodeImageToBase64(imageUri);
                    if (encodedImage != null) {
                        saveReviewToFirestore(encodedImage, content, rating); // Firestore에 Base64 이미지 저장
                    } else {
                        Toast.makeText(getActivity(), "이미지 인코딩에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    saveReviewToFirestore(null, content, rating); // 이미지 없이 저장
                }
            }
        });

        btnClose.setOnClickListener(v -> {
            // 이전 화면으로 돌아가기
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent); // 새로운 API로 갤러리 시작
    }

    private boolean validateInputs(String content) {
        if (content.isEmpty()) {
            Toast.makeText(getActivity(), "내용을 입력하세요", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveReviewToFirestore(String encodedImage, String content, float rating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> review = new HashMap<>();
        review.put("address", address);
        review.put("content", content);
        review.put("date", Timestamp.now());
        review.put("rating", rating);
        review.put("username", username);
        review.put("imageResource", encodedImage); // Base64로 인코딩된 이미지 저장

        db.collection("review")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getActivity(), "리뷰가 저장되었습니다", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "리뷰 저장에 실패했습니다", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "리뷰 저장 실패", e);
                });
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (maxWidth / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (maxHeight * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }


    private String encodeImageToBase64(Uri imageUri) {
        try {
            ContentResolver contentResolver = requireContext().getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            Bitmap resizedBitmap = resizeBitmap(bitmap, 1024, 1024); // 최대 해상도 1024x1024로 제한

            // 이미지 압축 및 Base64 인코딩
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 100; // 초기 압축 품질
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            while (baos.toByteArray().length >= 1024 * 1024) {
                // 1MB 이하가 될 때까지 품질을 낮춤
                quality -= 5;
                baos.reset();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }
            byte[] imageBytes = baos.toByteArray();

            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private String extractIdFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        }
        return email; // '@'가 없는 경우 원본 반환
    }

    @Override
    public void onResume() {
        super.onResume();
        // MainActivity의 BottomNavigationView 숨기기
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottomNavigation);
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // InfoFragment가 화면을 떠날 때 BottomNavigationView 다시 표시
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottomNavigation);
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }
}
