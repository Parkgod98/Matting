package com.example.Matting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Main_InfoFragment extends Fragment {
    private TextView tvTitle, tvCategory, tvAddress, tvLink, tvRating, tvMapX, tvMapY;
    private RecyclerView reviewRecyclerView;
    private Main_ReviewAdapter mainReviewAdapter;
    private List<Main_Review> mainReviewList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private ImageView btnClose, btnReview;
    private Button btnReserve;

    private RecyclerView mRecyclerView;
    private ArrayList<Community_RecyclerViewItem> mList = new ArrayList<>();;
    private Community_DetailRecyclerViewAdapter mCommunityDetailRecyclerViewAdapter;

    public static Main_InfoFragment newInstance(String title, String category, String address, String link, double rating, int map_x, int map_y) {
        Main_InfoFragment fragment = new Main_InfoFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("category", category);
        args.putString("address", address);
        args.putString("link", link);
        args.putDouble("rating", rating);
        args.putInt("map_x", map_x);
        args.putInt("map_y", map_y);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.info_fragment, container, false);

        // UI 초기화
        tvTitle = view.findViewById(R.id.tvTitle);
        tvCategory = view.findViewById(R.id.tvCategory);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvLink = view.findViewById(R.id.tvLink);
        tvRating = view.findViewById(R.id.tvRating);
        reviewRecyclerView = view.findViewById(R.id.reviewRecyclerView);
        mRecyclerView = view.findViewById(R.id.recyclerView);
        btnClose = view.findViewById(R.id.btnClose); // 닫기 버튼 초기화
        btnReserve = view.findViewById(R.id.btnReserve);
        btnReview = view.findViewById(R.id.btnReview);

        // Firestore 초기화
        firestore = FirebaseFirestore.getInstance();

        // RecyclerView 설정
        mainReviewAdapter = new Main_ReviewAdapter(mainReviewList);
        reviewRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        reviewRecyclerView.setAdapter(mainReviewAdapter);

        mCommunityDetailRecyclerViewAdapter = new Community_DetailRecyclerViewAdapter(mList, getContext());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mCommunityDetailRecyclerViewAdapter);

        // Arguments 확인
        if (getArguments() != null) {
            tvTitle.setText(getArguments().getString("title"));
            tvCategory.setText(getArguments().getString("category"));
            tvAddress.setText(getArguments().getString("address"));
            tvLink.setText(getArguments().getString("link"));
        } else {
            Log.e("InfoFragment", "getArguments() returned null");
        }

        // Firestore에서 리뷰 데이터 가져오기
        fetchReviewsFromFirestore();
        otherMatting();

        // 닫기 버튼 동작
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // InfoFragment를 닫음
                getParentFragmentManager().popBackStack();

                // MainActivity의 메서드를 호출하여 BottomSheet 상태를 STATE_EXPANDED로 설정
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).showBottomSheetExpanded();
                } else {
                    Log.e("BottomSheetDebug", "MainActivity instance not found");
                }
            }
        });

        btnReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CreateCommunityFragment 인스턴스 생성
                Main_ReviewWriteFragment mainReviewWriteFragment = new Main_ReviewWriteFragment();
                // 데이터 전달을 위한 Bundle 생성
                Bundle bundle = new Bundle();
                bundle.putString("restaurant", getArguments().getString("title")); // InfoFragment에서 받은 title 전달
                bundle.putString("address", getArguments().getString("address"));
                mainReviewWriteFragment.setArguments(bundle);

                // Fragment 전환
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.info_fragment_container, mainReviewWriteFragment);
                transaction.addToBackStack(null); // 뒤로 가기 스택에 추가
                transaction.commit();
            }
        });

        btnReserve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CreateCommunityFragment 인스턴스 생성
                Main_CreateMattingFragment mainCreateMattingFragment = new Main_CreateMattingFragment();
                // 데이터 전달을 위한 Bundle 생성
                Bundle bundle = new Bundle();
                bundle.putString("restaurant", getArguments().getString("title")); // InfoFragment에서 받은 title 전달
                bundle.putString("mapx", String.valueOf(getArguments().getInt("map_x")));
                bundle.putString("mapy", String.valueOf(getArguments().getInt("map_y")));
                bundle.putString("address", getArguments().getString("address"));
                mainCreateMattingFragment.setArguments(bundle);
                // Fragment 전환
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.info_fragment_container, mainCreateMattingFragment);
                transaction.addToBackStack(null); // 뒤로 가기 스택에 추가
                transaction.commit();
            }
        });

        return view;
    }

    private void fetchReviewsFromFirestore() {
        firestore.collection("review")
                .whereEqualTo("address", getArguments().getString("address"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mainReviewList.clear();

                        int reviewCount = 0;
                        float totalRating = 0;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Main_Review mainReview = document.toObject(Main_Review.class);
                            mainReviewList.add(mainReview);

                            // 평점 계산
                            Double rating = document.getDouble("rating");
                            if (rating != null) {
                                totalRating += rating;
                            }
                            reviewCount++;
                        }

                        // 평균 평점 계산
                        float averageRating = reviewCount > 0 ? totalRating / reviewCount : 0;

                        mainReviewAdapter.notifyDataSetChanged();

                        // 필요하다면 텍스트뷰에 결과 표시
                        if (tvRating != null) {
                            tvRating.setText(String.format("평점: %.1f (%d개)", averageRating, reviewCount));
                        }
                    } else {
                        Log.e("FirestoreError", "리뷰 데이터를 가져오는 데 실패했습니다.");
                    }
                });
    }

    // 현재 식당의 다른 모임 글
    private void otherMatting() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(Calendar.getInstance().getTime());

        // Firestore에서 "restaurant" 필드가 현재 식당 이름과 일치하는 문서 가져오기
        firestore.collection("community")
                .whereGreaterThanOrEqualTo("date", today)
                .whereEqualTo("restaurant", getArguments().getString("title")) // "restaurant" 필드와 일치하는 데이터만 가져오기
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

                            Community_RecyclerViewItem item = new Community_RecyclerViewItem();
                            item.setDocumentId(docId);
                            item.setMainText(otherTitle);
                            item.setSubText(date);
                            mList.add(item);
                        }

                        // 어댑터에 변경 알림
                        mCommunityDetailRecyclerViewAdapter = new Community_DetailRecyclerViewAdapter(mList, getContext());
                        mRecyclerView.setAdapter(mCommunityDetailRecyclerViewAdapter);
                        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
                    } else {
                        Log.e("FirestoreError", "데이터 가져오기 실패: " + task.getException());
                    }
                });
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
        fetchReviewsFromFirestore();
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
