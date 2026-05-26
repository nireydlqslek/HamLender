package com.example.hamlendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TimeTable extends View {

    // =========================
    // GRID SIZE
    // =========================
    private static final int COLS = 6;
    private static final int ROWS = 24;

    // =========================
    // CELL DATA
    // null = empty
    // =========================
    private String[][] cells = new String[ROWS][COLS];

    // =========================
    // PAINT
    // =========================
    private Paint fillPaint;
    private Paint linePaint;

    // =========================
    // CURRENT COLOR
    // =========================
    private String selectedColor = "#FF5722";

    // =========================
    // CELL SIZE
    // =========================
    private float cellWidth;
    private float cellHeight;

    // =========================
    // DRAG STATE
    // =========================
    private boolean isDragging = false;

    // 중복 저장 방지
    private boolean[][] visited = new boolean[ROWS][COLS];

    // 현재 드래그 변경사항
    private List<CellChange> currentChanges;

    // Undo Stack
    private Stack<List<CellChange>> undoStack = new Stack<>();

    // =========================
    // CONSTRUCTOR
    // =========================
    public TimeTable(Context context) {
        super(context);
        init();
    }

    public TimeTable(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeTable(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // =========================
    // INIT
    // =========================
    private void init() {

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);

        linePaint = new Paint();
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    // =========================
    // DRAW
    // =========================
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        cellWidth = getWidth() / (float) COLS;
        cellHeight = getHeight() / (float) ROWS;

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                float left = col * cellWidth;
                float top = row * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;

                // =========================
                // CELL FILL
                // =========================
                if (cells[row][col] != null) {

                    fillPaint.setColor(
                            Color.parseColor(cells[row][col])
                    );

                    canvas.drawRect(
                            left,
                            top,
                            right,
                            bottom,
                            fillPaint
                    );
                }

                // =========================
                // GRID LINE
                // =========================
                canvas.drawRect(
                        left,
                        top,
                        right,
                        bottom,
                        linePaint
                );
            }
        }
    }

    // =========================
    // TOUCH EVENT
    // =========================
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int col = (int) (event.getX() / cellWidth);
        int row = (int) (event.getY() / cellHeight);

        // 범위 밖
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) {
            return true;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                isDragging = true;

                clearVisited();

                currentChanges = new ArrayList<>();

                paintCell(row, col);

                break;

            case MotionEvent.ACTION_MOVE:

                if (isDragging) {
                    paintCell(row, col);
                }

                break;

            case MotionEvent.ACTION_UP:

                isDragging = false;

                if (currentChanges != null &&
                        !currentChanges.isEmpty()) {

                    undoStack.push(currentChanges);
                }

                break;
        }

        return true;
    }


    // PAINT CELL
    private void paintCell(int row, int col) {

        // 이미 처리한 셀
        if (visited[row][col]) {
            return;
        }

        visited[row][col] = true;

        String beforeColor = cells[row][col];

        // 이미 같은 색이면 무시
        if (selectedColor.equals(beforeColor)) {
            return;
        }

        // 변경
        cells[row][col] = selectedColor;

        // undo 저장
        CellChange change = new CellChange(
                row,
                col,
                beforeColor,
                selectedColor
        );

        currentChanges.add(change);

        invalidate();
    }


    // VISITED RESET
    private void clearVisited() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                visited[row][col] = false;
            }
        }
    }


    // SET COLOR
    public void setSelectedColor(String color) {
        this.selectedColor = color;
    }

    // UNDO
    public void undoLastAction() {

        if (undoStack.isEmpty()) {
            return;
        }

        List<CellChange> lastChanges = undoStack.pop();

        for (CellChange change : lastChanges) {

            cells[change.row][change.col]
                    = change.beforeColor;
        }

        invalidate();
    }

    // CLEAR ALL
    public void clearAll() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                cells[row][col] = null;
            }
        }

        undoStack.clear();

        invalidate();
    }

    // GET CELLS
    public String[][] getCells() {
        return cells;
    }


    // SET CELLS
    public void setCells(String[][] newCells) {

        if (newCells == null) {
            return;
        }

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                cells[row][col] = newCells[row][col];
            }
        }

        invalidate();
    }

    // CELL CHANGE MODEL
    public static class CellChange {

        public int row;
        public int col;

        public String beforeColor;
        public String afterColor;

        public CellChange(
                int row,
                int col,
                String beforeColor,
                String afterColor
        ) {
            this.row = row;
            this.col = col;
            this.beforeColor = beforeColor;
            this.afterColor = afterColor;
        }
    }
}
