package com.example.hamlendar;

import android.view.View;
import android.widget.TextView;

import com.kizitonwose.calendar.view.ViewContainer;

public class DayViewContainer extends ViewContainer {

    TextView tvDate, tvEvent1, tvEvent2, tvEvent3, tvEvent4, tvEvent5, tvMore;

    public DayViewContainer(View view) {
        super(view);

        tvDate = view.findViewById(R.id.tvDate);
        tvEvent1 = view.findViewById(R.id.tvEvent1);
        tvEvent2 = view.findViewById(R.id.tvEvent2);
        tvEvent3 = view.findViewById(R.id.tvEvent3);
        tvEvent4 = view.findViewById(R.id.tvEvent4);
        tvEvent5 = view.findViewById(R.id.tvEvent5);
        tvMore = view.findViewById(R.id.tvMore);
    }
}