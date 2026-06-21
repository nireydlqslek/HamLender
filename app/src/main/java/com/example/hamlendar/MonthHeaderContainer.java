package com.example.hamlendar;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kizitonwose.calendar.view.ViewContainer;

public class MonthHeaderContainer extends ViewContainer {

    final TextView tvMonth;
    final ImageView btnPrev;
    final ImageView btnNext;

    public MonthHeaderContainer(View view) {
        super(view);
        tvMonth = view.findViewById(R.id.tvMonth);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
    }
}