package com.example.Matting;

import static com.example.Matting.Chat_Chatmanage.addNewChatRoom;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Main_CreateMattingFragment extends Fragment {
    private EditText etTitle, etContent, etLocation, etRestaurant, etDate, etTime;
    private Button btnCreatePost, dateButton, timeButton;
    private ImageButton closeButton;
    private String mapx, mapy, userId, address;

    private User user;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_community, container, false);

        //로그인 확인
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
            userId = user.getUserId();
        }

        etTitle = view.findViewById(R.id.etTitle);
        etContent = view.findViewById(R.id.etContent);
        etLocation = view.findViewById(R.id.etLocation);
        etRestaurant = view.findViewById(R.id.etRestaurant);
        btnCreatePost = view.findViewById(R.id.btnCreatePost);
        closeButton = view.findViewById(R.id.btnClose);
        etDate = view.findViewById(R.id.etdate);
        etTime = view.findViewById(R.id.ettime);
        dateButton = view.findViewById(R.id.dateButton);
        timeButton = view.findViewById(R.id.timeButton);

        // 날짜/시간 변경 버튼 클릭 이벤트
        dateButton.setOnClickListener(v -> showDatePickerDialog());
        timeButton.setOnClickListener(v -> showTimePickerDialog());

        // 전달받은 데이터 수신
        Bundle arguments = getArguments();
        if (arguments != null) {
            String restaurant = arguments.getString("restaurant");
            mapx = arguments.getString("mapx");
            mapy = arguments.getString("mapy");
            address = arguments.getString("address");
            Log.d("mapxy", "putExtra" + mapx + mapy);
            if (restaurant != null) {
                etRestaurant.setText(restaurant); // 전달받은 title 값을 EditText에 설정
            }
        }

        btnCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = etTitle.getText().toString();
                String content = etContent.getText().toString();
                String location = etLocation.getText().toString();
                String restaurant = etRestaurant.getText().toString();
                String date = etDate.getText().toString();
                String time = etTime.getText().toString();

                if (!title.isEmpty() && !content.isEmpty() && !date.isEmpty() && !time.isEmpty()) {
                    // Firestore에 데이터 저장
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    CollectionReference communityRef = db.collection("community");

                    Map<String, Object> post = new HashMap<>();
                    post.put("title", title);
                    post.put("content", content);
                    post.put("location", location);
                    post.put("restaurant", restaurant);
                    post.put("date", date);
                    post.put("time", time);
                    post.put("mapx", mapx);
                    post.put("mapy", mapy);
                    post.put("userid", userId); // 현재 사용자 ID 추가
                    post.put("address", address);

                    communityRef.add(post)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(getActivity(), "게시물이 저장되었습니다.", Toast.LENGTH_SHORT).show();

                                //채팅 생성
                                User user = new User(getActivity());
                                addNewChatRoom(title, user, documentReference.getId());

                                // CommunityActivity로 이동
                                Intent intent = new Intent(getActivity(), CommunityActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), "게시물 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                Log.w("Firestore", "Error adding document", e);
                            });


                } else {
                    // 제목 또는 내용이 비어있을 경우 메시지 표시
                    Toast.makeText(getActivity(), "제목, 내용, 날짜, 시간은 필수 입력 사항입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CreateFragment를 닫고 BackStack에서 이전 프래그먼트(InfoFragment)를 다시 보여줌
                getParentFragmentManager().popBackStack();
            }
        });

        return view;
    }

    private void showDatePickerDialog() {
        // 현재 날짜 가져오기
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // DatePickerDialog 생성 및 표시
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, selectedYear, selectedMonth, selectedDay) -> {
            // 선택한 날짜를 YYYY-MM-DD 형식으로 변환
            String formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
            etDate.setText(formattedDate); // 선택한 날짜를 EditText에 설정
        }, year, month, day);

        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        // 현재 시간 가져오기
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // TimePickerDialog 생성 및 표시
        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (view, selectedHour, selectedMinute) -> {
            String period = selectedHour < 12 ? "오전" : "오후";
            int adjustedHour = selectedHour % 12 == 0 ? 12 : selectedHour % 12; // 12시 형식 처리
            String formattedTime = period + " " + adjustedHour + "시 " + selectedMinute + "분";
            etTime.setText(formattedTime); // 선택한 시간을 EditText에 설정
        }, hour, minute, false);

        timePickerDialog.show();
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
