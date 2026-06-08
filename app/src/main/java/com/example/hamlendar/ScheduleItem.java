package com.example.hamlendar;

final class ScheduleItem {
    final String id;
    final String title;
    final String category;
    final String memo;

    ScheduleItem(String id, String title, String category, String memo) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.memo = memo;
    }
}
