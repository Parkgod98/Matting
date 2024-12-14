package com.example.Matting;

import static com.example.Matting.Chat_Chatmanage.addNewChatRoom;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Community_DetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private double cur_lat, cur_lon;
    private NaverMap naverMap;

    private RecyclerView mRecyclerView, reviewImageRecyclerView;
    private ArrayList<Community_RecyclerViewItem> mList;
    private ArrayList<String> reviewImageBase64List;
    private Community_DetailRecyclerViewAdapter mCommunityDetailRecyclerViewAdapter;
    private ReviewImageAdapter reviewImageAdapter;

    private String documentId, title, content, userid, restaurant, location, date, time;
    private String address;

    private Marker marker = new Marker();

    private FirebaseAuth mAuth;
    private String userId;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // 채팅방 이동
        Button goChat = findViewById(R.id.go_chat);
        goChat.setOnClickListener(v -> newCommunityChat());

        //로그인 확인
        mAuth = FirebaseAuth.getInstance();
        user = new User(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // 로그인되지 않은 경우, 로그인 페이지로 이동
            Intent loginIntent = new Intent(this, User_LoginActivity.class);
            startActivity(loginIntent);
        } else {
            // 로그인된 사용자 정보 사용
            Log.d("nickname", user.getUserId());
            userId = user.getUserId();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Intent에서 documentId 가져오기
        documentId = getIntent().getStringExtra("documentId");
        Log.d("getDocumentId", "Community_DetailActivity: " + documentId);

        // Firestore에서 데이터 가져오기
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("community").document(documentId);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d("getDocumentId", "DocumentSnapshot data: " + document.getData());
                        title = document.getString("title");
                        content = document.getString("content");
                        location = document.getString("location");
                        userid = document.getString("userid");
                        date = document.getString("date");
                        time = document.getString("time");
                        restaurant = document.getString("restaurant");
                        cur_lon = document.getString("mapx") != null ? Double.parseDouble(document.getString("mapx")) / 10_000_000.0 : 0.0;
                        cur_lat = document.getString("mapy") != null ? Double.parseDouble(document.getString("mapy")) / 10_000_000.0 : 0.0;
                        address = document.getString("address");

                        updateUI();

                        // Firestore 데이터 로드 완료 후 메뉴 갱신
                        invalidateOptionsMenu();

                        // 리뷰 이미지 가져오기
                        fetchReviewImagesFromFirestore();
                    } else {
                        Log.d("getDocumentId", "No such document");
                    }
                } else {
                    Log.d("getDocumentId", "get failed with ", task.getException());
                }
            }
        });
    }

    private void fetchReviewImagesFromFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        reviewImageBase64List = new ArrayList<>();

        firestore.collection("review")
                .whereEqualTo("address", address)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String base64Image = document.getString("imageResource");
                            if (base64Image != null && !base64Image.isEmpty()) { // 빈 문자열 확인 추가
                                reviewImageBase64List.add(base64Image);
                            }
                        }
                        setupReviewImageRecyclerView();
                    } else {
                        Log.e("FirestoreError", "리뷰 이미지를 가져오는 데 실패했습니다.", task.getException());
                    }
                });
    }


    private void setupReviewImageRecyclerView() {
        reviewImageRecyclerView = findViewById(R.id.review_image_recycler_view);
        reviewImageAdapter = new ReviewImageAdapter(this, reviewImageBase64List);
        reviewImageRecyclerView.setAdapter(reviewImageAdapter);
        reviewImageRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.del, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteMenuItem = menu.findItem(R.id.del);

        // Firestore 데이터가 로드되지 않은 경우 메뉴 숨기기
        if (userid == null || userId == null) {
            Log.d("yesmenu", "Firestore 데이터가 아직 로드되지 않음");
            deleteMenuItem.setVisible(false);
            return super.onPrepareOptionsMenu(menu);
        }

        Log.d("yesmenu", "userId: " + userId + ", userid: " + userid);

        // Firestore 데이터가 로드된 후 로그인된 사용자와 게시물 작성자 비교
        if (userId.equals(userid)) {
            deleteMenuItem.setVisible(true); // 메뉴 보이기
        } else {
            deleteMenuItem.setVisible(false); // 메뉴 숨기기
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.del) {
            // 삭제 확인 다이얼로그 표시
            new AlertDialog.Builder(this)
                    .setTitle("게시물 삭제")
                    .setMessage("정말로 삭제하시겠습니까?")
                    .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Firestore에서 게시물 삭제
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection("community").document(documentId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(Community_DetailActivity.this, "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                        finish(); // 액티비티 종료
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(Community_DetailActivity.this, "게시물 삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUI() {
        TextView restaurantName = findViewById(R.id.restaurant_name);
        TextView postTitle = findViewById(R.id.post_title);
        TextView postContent = findViewById(R.id.post_content);
        TextView meetDate = findViewById(R.id.date);
        TextView meetTime = findViewById(R.id.time);
        TextView tvLocation = findViewById(R.id.tvLocation);
        TextView tvId = findViewById(R.id.go_profile);

        restaurantName.setText(restaurant);
        postTitle.setText(title);
        postContent.setText(content);
        meetDate.setText(date);
        meetTime.setText(time);
        tvLocation.setText(location);

        // Firebase Realtime Database에서 닉네임 가져오기
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userid).child("nicknames");
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String nickname = snapshot.getValue(String.class);
                    tvId.setText(nickname != null ? nickname : "Unknown User");
                } else {
                    tvId.setText("Unknown User");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvId.setText("Error");
                Log.e("CommunityDetail", "Error fetching nickname: ", error.toException());
            }
        });

        // 버튼 클릭 시 UserProfileActivity로 이동
        tvId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent userProfileIntent = new Intent(Community_DetailActivity.this, UserProfileActivity.class);
                userProfileIntent.putExtra("username", userid); // `userid` 전달
                startActivity(userProfileIntent);
            }
        });

        // 지도 초기화
        initMap();

        ImageButton fullScreenMapButton = findViewById(R.id.btn_full_screen_map);
        fullScreenMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullScreenMap();
            }
        });

        // 같은 식당의 다른 모임 글
        otherMatting();

        // 뒤로가기
        ImageButton goBackButton = findViewById(R.id.go_back);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }


    // 지도 초기화 메서드
    private void initMap() {
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    // 지도 로드 완료 시 호출되는 콜백
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        //배경 지도 선택
        naverMap.setMapType(NaverMap.MapType.Basic);
        //건물 표시
        naverMap.setLayerGroupEnabled(naverMap.LAYER_GROUP_BUILDING, true);
        //위치 및 각도 조정
        CameraPosition cameraPosition = new CameraPosition(
                new LatLng(cur_lat, cur_lon),   // 위치 지정
                16,                           // 줌 레벨
                0,                          // 기울임 각도
                0                           // 방향
        );
        naverMap.setCameraPosition(cameraPosition);

        marker.setPosition(new LatLng(cur_lat, cur_lon));
        marker.setMap(naverMap);
        marker.setCaptionText(restaurant);
    }

    // 전체 화면 지도 표시 메서드
    private void showFullScreenMap() {
        FullScreenMapFragment fullScreenMapFragment = new FullScreenMapFragment(cur_lat, cur_lon, restaurant);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fullScreenMapFragment)  // 현재 화면 전체를 덮도록 설정
                .addToBackStack(null)  // 뒤로가기 시 원래 화면으로 돌아가기
                .commit();
    }

    // 같은 식당의 다른 모임 글
    private void otherMatting() {
        // Firestore 인스턴스 가져오기
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        mRecyclerView = findViewById(R.id.recyclerView);
        mList = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(Calendar.getInstance().getTime());

        // Firestore에서 "restaurant" 필드가 현재 식당 이름과 일치하는 문서 가져오기
        firestore.collection("community")
                .whereGreaterThanOrEqualTo("date", today)
                .whereEqualTo("restaurant", restaurant) // "restaurant" 필드와 일치하는 데이터만 가져오기
                .orderBy("date", Query.Direction.ASCENDING) // 가까운 시간 순으로 정렬
                .orderBy("restaurant")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mList.clear(); // 기존 데이터 초기화
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String docId = document.getId(); // documentId 가져오기
                            String otherTitle = document.getString("title");
                            String date = document.getString("date");

                            if (!docId.equals(documentId)) { // 현재 게시물 제외
                                Community_RecyclerViewItem item = new Community_RecyclerViewItem();
                                item.setDocumentId(docId);
                                item.setMainText(otherTitle);
                                item.setSubText(date);
                                mList.add(item);
                            }
                        }

                        // 어댑터에 변경 알림
                        mCommunityDetailRecyclerViewAdapter = new Community_DetailRecyclerViewAdapter(mList, this);
                        mRecyclerView.setAdapter(mCommunityDetailRecyclerViewAdapter);
                        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
                    } else {
                        Log.e("FirestoreError", "데이터 가져오기 실패: " + task.getException());
                    }
                });
    }

    // RecyclerView 항목 추가 메서드
    public void addItem(String imgName, String mainText, String subText) {
        Community_RecyclerViewItem item = new Community_RecyclerViewItem();

        item.setImgName(imgName);
        item.setMainText(mainText);
        item.setSubText(subText);

        mList.add(item);
    }

    // 팝업창
    private void setupClickListenerForPopup(TextView textView, String title, String message) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // AlertDialog 생성 및 설정
                AlertDialog.Builder builder = new AlertDialog.Builder(Community_DetailActivity.this);
                builder.setTitle(title);
                builder.setMessage(message);

                // 확인 버튼 추가
                builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // 팝업 닫기
                    }
                });

                // 팝업 창 표시
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    // 전체 화면으로 이미지 표시 메서드
    private void showFullScreenImage(int imageResId) {
        // Dialog 생성
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // 다이얼로그 타이틀 제거
        dialog.setContentView(R.layout.dialog_fullscreen_image); // 전체 화면 이미지를 위한 레이아웃 설정

        // Dialog의 ImageView 설정
        ImageView fullScreenImageView = dialog.findViewById(R.id.fullscreen_image);
        fullScreenImageView.setImageResource(imageResId);  // 이미지 리소스 설정

        // Dialog 클릭 시 닫기
        fullScreenImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    //채팅방 연결
    private void newCommunityChat() {
        User user = new User(this);
        TextView postTitle = findViewById(R.id.post_title);
        addNewChatRoom(postTitle.getText().toString(), user, documentId);
        Intent intent = new Intent(Community_DetailActivity.this, Chat_ChatroomActivity.class);
        intent.putExtra("chatRoomId", documentId);
        startActivity(intent);
        return;
    }
}
