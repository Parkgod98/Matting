package com.example.Matting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.MainViewHolder> {

    private Context context;
    private List<Main> mainList;

    public MainAdapter(Context context, List<Main> mainList) {
        this.context = context;
        this.mainList = mainList;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_main_info_recycler_item, parent, false);
        return new MainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        Main main = mainList.get(position);
        holder.tvTitle.setText(main.getTitle());
        holder.tvCategory.setText(main.getCategory());
        holder.tvAddress.setText(main.getAddress());
        holder.tvRating.setText(String.valueOf(main.getRating()));

        // 이미지 초기화
        holder.image1.setImageResource(R.drawable.ic_image); // 기본 이미지 설정
        holder.image2.setImageResource(R.drawable.ic_image);
        holder.image3.setImageResource(R.drawable.ic_image);

        String address = main.getAddress();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        AtomicReference<Integer> cnt = new AtomicReference<>(0);

        firestore.collection("review")
                .whereEqualTo("address", address) // 특정 주소와 일치하는 리뷰 검색
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) { // 결과가 있을 때
                            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                String base64Image = document.getString("imageResource");

                                if (base64Image != null && !base64Image.isEmpty()) { // 유효한 이미지 확인
                                    // Base64 문자열을 디코딩
                                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    cnt.set(cnt.get() + 1);

                                    // UI 업데이트
                                    if (cnt.get() == 1) {
                                        holder.image1.setImageBitmap(decodedByte);
                                    } else if (cnt.get() == 2) {
                                        holder.image2.setImageBitmap(decodedByte);
                                    } else if (cnt.get() == 3) {
                                        holder.image3.setImageBitmap(decodedByte);
                                        return;
                                    }
                                }
                            }
                            // 유효한 이미지가 없는 경우 기본 이미지 설정
                        } else {
                            Log.w("ReviewImage", "일치하는 리뷰가 없습니다.");
                        }
                    } else {
                        Log.e("FirestoreError", "리뷰 이미지를 가져오는 데 실패했습니다.", task.getException());
                    }
                });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Main_InfoFragment mainInfoFragment = Main_InfoFragment.newInstance(
                        main.getTitle(),
                        main.getCategory(),
                        main.getAddress(),
                        main.getLink(),
                        main.getRating(),
                        main.getMapX(),
                        main.getMapY()
                );

                // Activity를 가져와서 BottomSheet 숨기기
                AppCompatActivity activity = (AppCompatActivity) v.getContext();
//                BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(activity.findViewById(R.id.bottom_sheet_layout));
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                ((MainActivity) activity).hideBottomSheet();

                // 전체 화면으로 InfoFragment 표시
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.info_fragment_container, mainInfoFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }


    @Override
    public int getItemCount() {
        return mainList.size();
    }

    public static class MainViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvAddress, tvRating;
        ImageView image1, image2, image3;

        public MainViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvRating = itemView.findViewById(R.id.tvRating);
            image1 = itemView.findViewById(R.id.image1);
            image2 = itemView.findViewById(R.id.image2);
            image3 = itemView.findViewById(R.id.image3);
        }
    }
}
