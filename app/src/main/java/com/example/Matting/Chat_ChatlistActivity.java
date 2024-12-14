package com.example.Matting;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Chat_ChatlistActivity extends AppCompatActivity {
    private ListView listViewChatRooms;
    private List<String> chatRoomList;
    private List<String> chatRoomNameList;
    private Chat_ChatRoomAdapter adapter;
    private User user;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //로그인 확인
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // 로그인 페이지로 이동하고 결과를 기다림
            Intent loginIntent = new Intent(Chat_ChatlistActivity.this, User_LoginActivity.class);
            startActivityForResult(loginIntent, 1001); // 1001은 요청 코드
        }

        //파이어베이스 초기화
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.chat_chatroomlist);
        user = new User(this);

        listViewChatRooms = findViewById(R.id.listViewChatRooms);
        chatRoomList = new ArrayList<>();
        chatRoomNameList = new ArrayList<>();
        adapter = new Chat_ChatRoomAdapter(this, chatRoomNameList, chatRoomList);

        listViewChatRooms.setAdapter(adapter);

        // Firebase에서 채팅방 목록을 로드
        FirebaseDatabase.getInstance().getReference("users").child(user.getUserId()).child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatRoomList.clear();
                chatRoomNameList.clear();
                for (DataSnapshot chatRoomSnapshot : snapshot.getChildren()) {
                    String chatRoomId = chatRoomSnapshot.getValue(String.class);
                    if (chatRoomId != null) {
                        chatRoomList.add(chatRoomId);

                        // 각 채팅방의 이름을 가져오기
                        FirebaseDatabase.getInstance().getReference(chatRoomId).child("chatname").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String chatRoomName = snapshot.getValue(String.class);
                                if (chatRoomName != null) {
                                    chatRoomNameList.add(chatRoomName);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // 에러 처리
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 에러 처리
            }
        });

        listViewChatRooms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedChatRoomId = chatRoomList.get(position);
                Intent intent = new Intent(Chat_ChatlistActivity.this, Chat_ChatroomActivity.class);
                intent.putExtra("chatRoomId", selectedChatRoomId);
                startActivity(intent);
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 툴바의 기본 타이틀 제거
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // BottomNavigationView 초기화
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_chat); // 채팅 탭 선택

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent homeIntent = new Intent(Chat_ChatlistActivity.this, MainActivity.class);
                    startActivity(homeIntent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_feed) {
                    Intent feedIntent = new Intent(Chat_ChatlistActivity.this, Feed_MainActivity.class);
                    startActivity(feedIntent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_community) {
                    Intent communityIntent = new Intent(Chat_ChatlistActivity.this, CommunityActivity.class);
                    startActivity(communityIntent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_mypage) {
                    Intent mypageIntent = new Intent(Chat_ChatlistActivity.this, MyProfileActivity.class);
                    startActivity(mypageIntent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }
/*
    private void showAddChatRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Chat Room");

        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newChatRoomId = input.getText().toString();
            if (!newChatRoomId.isEmpty()) {
                addNewChatRoom(newChatRoomId, user,);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }*/
}
