package com.example.hamlendar;

public class CategoryItem {
    private String id;
    private String name;
    private String colorCode;
    private int index; // 🌟 순서 정렬을 위한 인덱스 필드 추가

    public CategoryItem() {} // 파이어베이스용 기본 생성자

    public CategoryItem(String name, String colorCode) {
        this.name = name;
        this.colorCode = colorCode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    // 🌟 getter / setter 추가
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
}