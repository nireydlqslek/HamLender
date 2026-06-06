package com.example.hamlendar;

import com.google.firebase.firestore.Exclude; // 🌟 추가

public class CategoryItem {
    @Exclude // 파이어베이스에 다시 저장할 때 이 필드는 제외하도록 설정
    private String id;
    private String name;
    private String colorCode;

    public CategoryItem() {}

    public CategoryItem(String name, String colorCode) {
        this.name = name;
        this.colorCode = colorCode;
    }

    // 🌟 ID Getter/Setter 추가
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }
}