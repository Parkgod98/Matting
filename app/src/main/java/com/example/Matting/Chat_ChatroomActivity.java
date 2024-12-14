package com.example.Matting;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Chat_ChatroomActivity extends AppCompatActivity {
    private ListView listViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;
    private Chat_MessageAdapter chatMessageAdapter;
    private List<Chat_Message> chatMessageList;
    private DatabaseReference db;
    private User user;
    private String chatroomId;
    private ArrayList<String> userlist;
    private TextView usercnt;
    private Button buttonMeetingInfo;
    private String meetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.chat_chatroom);

        // 액션바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 뒤로가기 버튼 활성화
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        getSupportActionBar().setTitle("채팅방");
        toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));
        listViewMessages = findViewById(R.id.listViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        usercnt = findViewById(R.id.usercnt);
        buttonMeetingInfo = findViewById(R.id.buttonMeetingInfo);

        user = new User(this);
        // Intent로부터 채팅방 ID를 가져옴
        Intent intent = getIntent();
        chatroomId = intent.getStringExtra("chatRoomId");

        db = FirebaseDatabase.getInstance().getReference().child(chatroomId);
        db.child("id").setValue(chatroomId);
        chatMessageList = new ArrayList<>();
        chatMessageAdapter = new Chat_MessageAdapter(this, chatMessageList, user.getUserId());
        listViewMessages.setAdapter(chatMessageAdapter);

        // users 노드의 변경 사항을 실시간으로 감지
        db.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // DataSnapshot을 리스트로 변환
                    List<String> userList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String userId = snapshot.getValue(String.class);
                        if (userId != null) {
                            userList.add(userId);
                        }
                    }
                    // userList를 사용하여 추가 작업 수행
                    usercnt.setText(String.valueOf(userList.size()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 에러 처리
            }
        });

        buttonSend.setOnClickListener(v -> {
            String messageText = editTextMessage.getText().toString();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
                editTextMessage.setText("");
            }
        });

        buttonMeetingInfo.setOnClickListener(v -> {
            // 프래그먼트 실행
            MeetingInfoFragment fragment = MeetingInfoFragment.newInstance(chatroomId, chatroomId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, fragment);
            transaction.commit(); // 백 스택에 추가하지 않음
        });


        receiveMessages();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // 뒤로가기 버튼 클릭 시 이전 액티비티로 이동
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage(String message) {
        String messageId = db.child("messages").push().getKey();
        Chat_Message chatMessageData = new Chat_Message(user.getUserId(), message, System.currentTimeMillis());
        db.child("messages").child(messageId).setValue(chatMessageData);
    }

    private void receiveMessages() {
        db.child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Chat_Message chatMessage = messageSnapshot.getValue(Chat_Message.class);
                    if (chatMessage != null) {
                        chatMessageList.add(chatMessage);
                    }
                }
                chatMessageAdapter.notifyDataSetChanged();
                listViewMessages.setSelection(chatMessageAdapter.getCount() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 에러 처리
            }
        });
    }
}
