package com.example.hamlendar;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat; // 🌟 안전한 색상 입히기를 위한 컴팩트 도구
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

        // 🌟 [에러 해결 해결책] VectorDrawable 테러 방지 및 안전한 색상 입히기
        Drawable backgroundDrawable = holder.viewColorCircle.getBackground();
        if (backgroundDrawable != null && item.getColorCode() != null) {
            try {
                // 특정 Drawable 클래스로 강제 캐스팅하지 않고, 감싸기(Wrap) 방식을 사용하여 형변환 에러를 원천 차단합니다.
                Drawable wrappedDrawable = DrawableCompat.wrap(backgroundDrawable.mutate());
                DrawableCompat.setTint(wrappedDrawable, Color.parseColor(item.getColorCode()));
                holder.viewColorCircle.setBackground(wrappedDrawable);
            } catch (Exception e) {
                // 만약 컬러 코드 문자열이 잘못되었을 경우를 대비한 안전망 예외 처리
                e.printStackTrace();
            }
        }

        // 🌟 카테고리 아이템 클릭 시 [수정 / 삭제] 선택 다이얼로그 노출
        holder.itemView.setOnClickListener(v -> {
            String[] options = {"이름 수정하기", "카테고리 삭제하기"};
            new AlertDialog.Builder(context)
                    .setTitle(item.getName() + " 카테고리 관리")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            showEditDialog(context, item, currentPosition);
                        } else if (which == 1) {
                            deleteCategory(context, item, currentPosition);
                        }
                    })
                    .show();
        });
    }

    // 이름 수정 다이얼로그 팝업 함수
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
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 삭제 처리 함수
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
        return categoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategoryName;
        View viewColorCircle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategoryName = itemView.findViewById(R.id.txtCategoryName);
            viewColorCircle = itemView.findViewById(R.id.viewColorCircle);
        }
    }
}