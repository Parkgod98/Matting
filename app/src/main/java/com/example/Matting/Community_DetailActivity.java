package com.example.Matting;
import static com.example.Matting.Chat_Chatmanage.addNewChatRoom;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import java.util.ArrayList;


public class Community_DetailActivity extends AppCompatActivity implements OnMapReadyCallback{

    private double cur_lat, cur_lon;
    private NaverMap naverMap;

    private RecyclerView mRecyclerView;
    private ArrayList<RecyclerViewItem> mList;
    private Community_DetailRecyclerViewAdapter mCommunityDetailRecyclerViewAdapter;

    private String title, content, restaurant, location, date, time;

    private Marker marker = new Marker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        title = getIntent().getStringExtra("title");
        content = getIntent().getStringExtra("content");
        restaurant = getIntent().getStringExtra("restaurant");
        location = getIntent().getStringExtra("info");
        date = getIntent().getStringExtra("date");
        time = getIntent().getStringExtra("time");
        cur_lon = Double.parseDouble(getIntent().getStringExtra("mapx")) / 10_000_000.0;
        cur_lat = Double.parseDouble(getIntent().getStringExtra("mapy")) / 10_000_000.0;

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

        // 페이지 이동 버튼
        TextView goRestaurant = findViewById(R.id.go_restaurant);
        TextView goProgile = findViewById(R.id.go_profile);
        Button goChat = findViewById(R.id.go_chat);
        setupClickListenerForPopup(goRestaurant, "식당 정보", "식당 정보 페이지로 이동합니다.");
        setupClickListenerForPopup(goProgile, "아이디", "프로필 페이지로 이동합니다.");
        setupClickListenerForPopup(goChat, "채팅", "채팅 페이지로 이동합니다.");
        goChat.setOnClickListener(v ->  newCommunityChat());

        // 게시글, 본문
        TextView restaurantName = findViewById(R.id.restaurant_name);
        TextView postTitle = findViewById(R.id.post_title);
        TextView postContent = findViewById(R.id.post_content);
        TextView meetDate = findViewById(R.id.date);
        TextView meetTime = findViewById(R.id.time);
        TextView tvLocation = findViewById(R.id.tvLocation);
        restaurantName.setText(restaurant);
        postTitle.setText(title);
        postContent.setText(content);
        meetDate.setText(date);
        meetTime.setText(time);
        tvLocation.setText(location);

        // 뒤로가기
        ImageButton goBackButton = findViewById(R.id.go_back);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 이미지 클릭 시 전체 화면으로 이미지 보기
        ImageView thumbnailImage = findViewById(R.id.thumbnail_image);
        thumbnailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullScreenImage(R.drawable.food);  // 확대할 이미지 리소스를 전달
            }
        });
    }

    // 맵 위치 업데이트 메서드
    private void updateMapLocation() {
        if (naverMap != null) {
            CameraPosition cameraPosition = new CameraPosition(new LatLng(cur_lat, cur_lon), 13);
            naverMap.setCameraPosition(cameraPosition);
        }
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
                15,                           // 줌 레벨
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

        // 현재 게시물의 documentId 가져오기 (Intent로 전달받았다고 가정)
//        String currentTitle = getIntent().getStringExtra("documentId");

        // Firestore에서 "restaurant" 필드가 현재 식당 이름과 일치하는 문서 가져오기
        firestore.collection("community")
                .whereEqualTo("restaurant", restaurant) // "restaurant" 필드와 일치하는 데이터만 가져오기
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mList.clear(); // 기존 데이터 초기화
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Firestore 데이터를 RecyclerViewItem으로 변환
                            String otherTitle = document.getString("title");
                            if (!otherTitle.equals(title)) { // 현재 게시물 제목과 다른 경우만 추가
                                String date = document.getString("date");
                                addItem("iconName", otherTitle, date); // 데이터 추가
                            } // 데이터 추가
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
    public void addItem(String imgName, String mainText, String subText){
        RecyclerViewItem item = new RecyclerViewItem();

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
    private void newCommunityChat(){
        User user = new User(this);
        TextView postTitle = findViewById(R.id.post_title);
        addNewChatRoom(postTitle.getText().toString(),user);
        Intent intent = new Intent(Community_DetailActivity.this, Chat_ChatroomActivity.class);
        intent.putExtra("chatRoomId", postTitle.getText().toString());
        startActivity(intent);
        return;
    }
}