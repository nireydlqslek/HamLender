package com.example.hamlendar;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Collections;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<CategoryItem> categoryList;

    public CategoryAdapter(List<CategoryItem> categoryList) {
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final int currentPosition = holder.getAdapterPosition();
        if (currentPosition == RecyclerView.NO_POSITION) return;

        CategoryItem item = categoryList.get(currentPosition);
        Context context = holder.itemView.getContext();

        holder.txtCategoryName.setText(item.getName());

        // [파스텔톤 컬러 매핑 테이블]
        String colorType = item.getColorCode();
        if (colorType == null) colorType = "GREY";

        int darkRes, lightRes;
        switch (colorType.toUpperCase()) {
            case "RED": darkRes = R.color.cat_red_dark; lightRes = R.color.cat_red_light; break;
            case "ORANGE": darkRes = R.color.cat_orange_dark; lightRes = R.color.cat_orange_light; break;
            case "YELLOW": darkRes = R.color.cat_yellow_dark; lightRes = R.color.cat_yellow_light; break;
            case "GREEN": darkRes = R.color.cat_green_dark; lightRes = R.color.cat_green_light; break;
            case "BLUE": darkRes = R.color.cat_blue_dark; lightRes = R.color.cat_blue_light; break;
            case "PURPLE": darkRes = R.color.cat_purple_dark; lightRes = R.color.cat_purple_light; break;
            case "PINK": darkRes = R.color.cat_pink_dark; lightRes = R.color.cat_pink_light; break;
            default: darkRes = R.color.cat_grey_dark; lightRes = R.color.cat_grey_light; break;
        }

        holder.viewColorBar.setBackgroundColor(ContextCompat.getColor(context, darkRes));
        Drawable backgroundDrawable = holder.layoutCategoryRoot.getBackground();
        if (backgroundDrawable != null) {
            try {
                Drawable wrappedDrawable = DrawableCompat.wrap(backgroundDrawable.mutate());
                DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, lightRes));
                holder.layoutCategoryRoot.setBackground(wrappedDrawable);
            } catch (Exception e) { e.printStackTrace(); }
        }

        // 🔼 위로 이동 버튼 클릭 리스너
        holder.btnMoveUp.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos > 0 && pos != RecyclerView.NO_POSITION) {
                swapItems(context, pos, pos - 1);
            } else {
                Toast.makeText(context, "이미 가장 위에 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔽 아래로 이동 버튼 클릭 리스너
        holder.btnMoveDown.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos < categoryList.size() - 1 && pos != RecyclerView.NO_POSITION) {
                swapItems(context, pos, pos + 1);
            } else {
                Toast.makeText(context, "이미 가장 아래에 있습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 삭제 및 수정 (기존 다이얼로그 기능 유지)
        holder.btnDelete.setOnClickListener(v -> deleteCategory(context, item, currentPosition));
        holder.itemView.setOnClickListener(v -> {
            String[] options = {"이름 수정하기"};
            new AlertDialog.Builder(context)
                    .setTitle(item.getName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) showEditDialog(context, item, currentPosition);
                        else if (which == 1) deleteCategory(context, item, currentPosition);
                    })
                    .show();
        });
    }

    // 🌟 리스트 내에서 순서를 바꾸고 DB에 저장하는 핵심 메서드
    private void swapItems(Context context, int fromPosition, int toPosition) {
        // 1. 로컬 리스트에서 두 아이템의 위치를 맞바꿈
        Collections.swap(categoryList, fromPosition, toPosition);

        // 2. 바뀐 데이터에 맞게 index 값 재설정
        categoryList.get(fromPosition).setIndex(fromPosition);
        categoryList.get(toPosition).setIndex(toPosition);

        // 3. 어댑터에 변경 알림 (부드러운 애니메이션 효과)
        notifyItemMoved(fromPosition, toPosition);
        notifyItemChanged(fromPosition);
        notifyItemChanged(toPosition);

        // 4. 파이어스토어(원격 DB)에 순서 배치 일괄 동기화 (Batch 사용)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // 🌟 [핵심 수리 1] 순서를 바꿀 때도 UID 말고 이메일 주소 이름표 사용!
            String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            WriteBatch batch = db.batch();

            CategoryItem item1 = categoryList.get(fromPosition);
            CategoryItem item2 = categoryList.get(toPosition);

            if (item1.getId() != null && item2.getId() != null) {
                batch.update(db.collection("users").document(user.getUid()).collection("categories").document(item1.getId()), "index", fromPosition);
                batch.update(db.collection("users").document(user.getUid()).collection("categories").document(item2.getId()), "index", toPosition);

                batch.commit().addOnFailureListener(e -> {
                    Toast.makeText(context, "순서 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void showEditDialog(Context context, CategoryItem item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("카테고리 이름 수정");
        final EditText input = new EditText(context);
        input.setText(item.getName());
        builder.setView(input);

        builder.setPositiveButton("수정", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && item.getId() != null) {
                    // 🌟 [핵심 수리 2] 카테고리 이름을 수정할 때도 이메일 주소 이름표 사용!
                    String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();

                    FirebaseFirestore.getInstance()
                            .collection("users").document(user.getUid())
                            .collection("categories").document(item.getId())
                            .update("name", newName)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "수정되었습니다.", Toast.LENGTH_SHORT).show();
                                item.setName(newName);
                                notifyItemChanged(position);
                            });
                }
            }
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void deleteCategory(Context context, CategoryItem item, int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && item.getId() != null) {
            // 🌟 [핵심 수리 3] 카테고리를 삭제할 때도 이메일 주소 이름표 사용!
            String myEmailKey = user.getEmail() != null ? user.getEmail() : user.getUid();

            FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid())
                    .collection("categories").document(item.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "카테고리가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        categoryList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, categoryList.size());
                    });
        }
    }

    @Override
    public int getItemCount() { return categoryList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategoryName;
        View viewColorBar;
        View layoutCategoryRoot;
        ImageView btnMoveUp, btnMoveDown, btnDelete; // 화살표 및 삭제 버튼 추가

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            viewColorBar = itemView.findViewById(R.id.viewColorBar);
            layoutCategoryRoot = itemView.findViewById(R.id.layoutCategoryRoot);
            btnMoveUp = itemView.findViewById(R.id.btnMoveUp);         // XML의 위로 가기 버튼 매핑
            btnMoveDown = itemView.findViewById(R.id.btnMoveDown);     // XML의 아래로 가기 버튼 매핑
            btnDelete = itemView.findViewById(R.id.btnDelete);         // XML의 휴지통 삭제 버튼 매핑
        }
    }
}