package com.example.hamlendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TimeTable extends View {

    //가로 세로 10분씩 6칸 1시간씩 24칸
    private static final int COLS = 6;
    private static final int ROWS = 24;

    // 각 칸의 색상 저장
    // null이면 비어있는 칸
    private String[][] cells = new String[ROWS][COLS];

    // 색칠용 Paint
    private Paint fillPaint;

    // 격자선용 Paint
    private Paint linePaint;

    // 현재 선택된 색상
    private String selectedColor = "#FF5722";

    // 한 칸 크기
    private float cellWidth;
    private float cellHeight;

    // 현재 드래그 중인지
    private boolean isDragging = false;

    // 드래그 중 이미 지나간 칸 체크
    private boolean[][] visited = new boolean[ROWS][COLS];

    // 현재 드래그에서 변경된 칸들
    private List<CellChange> currentChanges;

    // Undo(다시) 기능용 스택
    private Stack<List<CellChange>> undoStack = new Stack<>();

    // 생성자
    public TimeTable(Context context) {
        super(context);
        init();
    }

    public TimeTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeTable(Context context,
                      AttributeSet attrs,
                     int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init();
    }



    // 초기 설정
    private void init() {

        // 배경 투명
        setBackgroundColor(Color.TRANSPARENT);

        // 칸 내부 색칠용
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);

        // 격자선용
        linePaint = new Paint();

        // 선 색
        linePaint.setColor(Color.LTGRAY);

        // 선 두께
        linePaint.setStrokeWidth(2f);

        // 매우 중요
        // 선만 그림
        linePaint.setStyle(Paint.Style.STROKE);
    }


    // 화면 그리기
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 한 칸 크기 계산
        cellWidth = getWidth() / (float) COLS;
        cellHeight = getHeight() / (float) ROWS;

        // 전체 칸 반복
        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                float left = col * cellWidth;
                float top = row * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;

                // 색칠된 칸만 내부 색 채우기
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

                // 격자선 그리기
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


    // 터치 처리
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // 현재 터치 위치를 칸 번호로 변환
        int col = (int) (event.getX() / cellWidth);
        int row = (int) (event.getY() / cellHeight);

        // 범위 밖 방지
        if (col < 0 || col >= COLS ||
                row < 0 || row >= ROWS) {

            return true;
        }

        switch (event.getAction()) {

            // 처음 눌렀을 때
            case MotionEvent.ACTION_DOWN:

                isDragging = true;

                // 방문 기록 초기화
                clearVisited();

                // 현재 드래그 기록 시작
                currentChanges = new ArrayList<>();

                // 현재 칸 색칠
                paintCell(row, col);

                break;

            // 드래그 중
            case MotionEvent.ACTION_MOVE:

                if (isDragging) {

                    paintCell(row, col);
                }

                break;

            // 손 뗐을 때
            case MotionEvent.ACTION_UP:

                isDragging = false;

                // 현재 작업 저장
                if (currentChanges != null &&
                        !currentChanges.isEmpty()) {

                    undoStack.push(currentChanges);
                }

                break;
        }

        return true;
    }

    // 실제 칸 색칠
    private void paintCell(int row, int col) {

        // 이미 지나간 칸이면 무시
        if (visited[row][col]) {
            return;
        }

        visited[row][col] = true;

        // 이전 색상 저장
        String beforeColor = cells[row][col];

        // 이미 같은 색이면 무시
        if (selectedColor.equals(beforeColor)) {
            return;
        }

        // 실제 색 변경
        cells[row][col] = selectedColor;

        // Undo 저장용
        CellChange change = new CellChange(
                row,
                col,
                beforeColor,
                selectedColor
        );

        currentChanges.add(change);

        // 다시 그리기
        invalidate();
    }

    // 방문 기록 초기화
    private void clearVisited() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                visited[row][col] = false;
            }
        }
    }

    // 현재 선택 색상 변경
    public void setSelectedColor(String color) {

        selectedColor = color;
    }

    // 마지막 작업 되돌리기
    public void undoLastAction() {

        if (undoStack.isEmpty()) {
            return;
        }

        // 최근 작업 가져오기
        List<CellChange> lastChanges = undoStack.pop();

        // 이전 색으로 복구
        for (CellChange change : lastChanges) {

            cells[change.row][change.col]
                    = change.beforeColor;
        }

        invalidate();
    }

    // 전체 삭제
    public void clearAll() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                cells[row][col] = null;
            }
        }

        undoStack.clear();

        invalidate();
    }

    // 현재 데이터 가져오기
    public String[][] getCells() {

        return cells;
    }

    // 외부 데이터 적용
    // Firebase 불러오기 등에 사용
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

    // XML Preview 안정화용
    @Override
    protected void onMeasure(int widthMeasureSpec,
                             int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // 칸 변경 기록 클래스
    // Undo 기능에 사용
    public static class CellChange {

        // 위치
        public int row;
        public int col;

        // 변경 전 색
        public String beforeColor;

        // 변경 후 색
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