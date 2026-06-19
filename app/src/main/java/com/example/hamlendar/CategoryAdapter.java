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

        // 색상 및 배경 처리
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

        if (holder.viewColorBar != null) {
            holder.viewColorBar.setBackgroundColor(ContextCompat.getColor(context, darkRes));
        }

        if (holder.layoutCategoryRoot != null) {
            Drawable backgroundDrawable = holder.layoutCategoryRoot.getBackground();
            if (backgroundDrawable != null) {
                try {
                    Drawable wrappedDrawable = DrawableCompat.wrap(backgroundDrawable.mutate());
                    DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(context, lightRes));
                    holder.layoutCategoryRoot.setBackground(wrappedDrawable);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        // 🌟 [수정] 어댑터에는 무조건 순수 커스텀 카테고리만 들어오므로 예외 방어 제거 및 모든 기능 기본 활성화
        if (holder.btnMoveUp != null) holder.btnMoveUp.setVisibility(View.VISIBLE);
        if (holder.btnMoveDown != null) holder.btnMoveDown.setVisibility(View.VISIBLE);
        if (holder.btnDelete != null) holder.btnDelete.setVisibility(View.VISIBLE);

        // 🔼 위로 이동 버튼 클릭 리스너 (0번 인덱스보다 커야 위로 갈 수 있음)
        holder.btnMoveUp.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos > 0 && pos != RecyclerView.NO_POSITION) {
                swapItems(context, pos, pos - 1);
            } else {
                Toast.makeText(context, "더 이상 위로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show();
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

        // 삭제 및 수정 리스너 연결
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> deleteCategory(context, item, holder.getAdapterPosition()));
        }

        holder.itemView.setOnClickListener(v -> {
            String[] options = {"이름 수정하기"};
            new AlertDialog.Builder(context)
                    .setTitle(item.getName())
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) showEditDialog(context, item, holder.getAdapterPosition());
                    })
                    .show();
        });
    }

    // 🌟 [수정] 데이터 리스트 인덱스 보정 처리(-2 제거) 수정
    private void swapItems(Context context, int fromPosition, int toPosition) {
        Collections.swap(categoryList, fromPosition, toPosition);
        categoryList.get(fromPosition).setIndex(fromPosition);
        categoryList.get(toPosition).setIndex(toPosition);

        notifyItemMoved(fromPosition, toPosition);
        notifyItemChanged(fromPosition);
        notifyItemChanged(toPosition);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
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
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategoryName;
        View viewColorBar;
        View layoutCategoryRoot;
        ImageView btnMoveUp, btnMoveDown, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            viewColorBar = itemView.findViewById(R.id.viewColorBar);
            layoutCategoryRoot = itemView.findViewById(R.id.layoutCategoryRoot);
            btnMoveUp = itemView.findViewById(R.id.btnMoveUp);
            btnMoveDown = itemView.findViewById(R.id.btnMoveDown);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}