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

import java.util.HashMap;
import java.util.Map;

//기능 : 드래그 해서 색칠, 다시하기(직전 저장. undo), 미니테이블을 보기 전용으로, 드래그 완료 이벤트 전달

public class TimeTable extends View {

    //변수 선언 시작--------------------------------------------------------------------------

    //가로 6칸
    private static final int COLS = 6;

    //세로 24칸
    private static final int ROWS = 24;

    //시간 표시용 라벨
    private static final int LABEL_COLS = 1;
    private static final int LABEL_ROWS = 1;

    private static final int DISPLAY_COLS = COLS + LABEL_COLS;
    private static final int DISPLAY_ROWS = ROWS + LABEL_ROWS;

    private Paint textPaint;

    // 각 칸의 색상 저장
    // null이면 비어있는 칸
    private String[][] cells = new String[ROWS][COLS];

    // 칸 내부 색칠용 Paint
    private Paint fillPaint;

    // 격자선용 Paint
    private Paint linePaint;

    // 현재 선택된 색상
    private String selectedColor = "#FF5722";

    // 한 칸 크기
    private float cellWidth;
    private float cellHeight;

    // 현재 드래그 중인지 체크
    private boolean isDragging = false;

    // 편집 가능 여부
    // true = 수정 가능
    // false = 보기 전용
    private boolean editable = true;

    // 드래그 중 이미 지나간 칸 체크
    private boolean[][] visited = new boolean[ROWS][COLS];

    // 현재 드래그에서 변경된 칸들 저장
    private List<CellChange> currentChanges;

    // Undo(다시) 기능용 스택
    private Stack<List<CellChange>> undoStack = new Stack<>();

    // 드래그 완료 리스너
    // 드래그 종료 후 팝업 띄우기용
    private OnDragCompleteListener dragCompleteListener;

    // 각 칸이 어떤 드래그 묶음에 속하는지 저장
    // -1이면 묶음 없음
    private int[][] groupIds = new int[ROWS][COLS];

    // 드래그 묶음 번호
    private int nextGroupId = 1;

    // 지우개 모드 여부
    private boolean eraserMode = false;

    private int currentGroupId = -1;

    // 전체 삭제 요청 리스너
    private OnRequestClearAllListener requestClearAllListener;

    // 드래그 묶음별 라벨 저장
    private Map<Integer, String> groupLabels = new HashMap<>();

    // 라벨 수정 모드 여부
    private boolean labelEditMode = false;

    // 라벨 수정 요청 리스너
    private OnLabelEditRequestListener labelEditRequestListener;

    private Paint labelPaint;
    //변수 선언 끝--------------------------------------------------------------------------


    //생성자 시작--------------------------------------------------------------------------

    // 기본 생성자
    public TimeTable(Context context) {
        super(context);
        init();
    }

    // XML에서 View 생성 시 사용되는 생성자
    public TimeTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    // 스타일 포함 생성자
    public TimeTable(Context context,
                     AttributeSet attrs,
                     int defStyleAttr) {

        super(context, attrs, defStyleAttr);
        init();
    }

    //생성자 끝--------------------------------------------------------------------------


    //초기 설정 시작--------------------------------------------------------------------------

    // 초기 설정
    private void init() {

        // 배경 투명
        setBackgroundColor(Color.TRANSPARENT);

        // 칸 내부 색칠용 Paint 설정
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);

        // 격자선 Paint 설정
        linePaint = new Paint();

        // 선 색상
        linePaint.setColor(Color.LTGRAY);

        // 선 두께
        linePaint.setStrokeWidth(2f);

        // 선만 그리기
        linePaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);

        labelPaint = new Paint(textPaint);
        labelPaint.setTextSize(24f); // 원하는 크기로 조절
        labelPaint.setFakeBoldText(true);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                groupIds[row][col] = -1;
            }
        }
    }

    //초기 설정 끝--------------------------------------------------------------------------


    //편집 가능 여부 설정 시작--------------------------------------------------------------------------

    // 편집 가능 여부 변경
    // 미니 타임테이블은 false 사용
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    // 현재 편집 가능 여부 반환
    public boolean isEditable() {
        return editable;
    }

    // 일반 색칠 모드
    public void setDrawMode() {
        eraserMode = false;
    }

    // 지우개 모드
    public void setEraserMode() {
        eraserMode = true;
    }

    //편집 가능 여부 설정 끝--------------------------------------------------------------------------


    //드래그 완료 리스너 시작--------------------------------------------------------------------------

    // 드래그 완료 리스너 연결
    public void setOnDragCompleteListener(
            OnDragCompleteListener listener
    ) {
        this.dragCompleteListener = listener;
    }

    // 드래그 완료 이벤트 인터페이스
    public interface OnDragCompleteListener {

        // 드래그 완료 시 호출
        void onDragComplete(int groupId);
    }

    // 전체 삭제 요청 이벤트 인터페이스
    public interface OnRequestClearAllListener {

        // 전체 삭제 요청 시 호출
        void onRequestClearAll();
    }

    //드래그 완료 리스너 끝--------------------------------------------------------------------------


    //화면 그리기 시작--------------------------------------------------------------------------

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        cellWidth = getWidth() / (float) DISPLAY_COLS;
        cellHeight = getHeight() / (float) DISPLAY_ROWS;

        // 전체 격자 그리기
        for (int row = 0; row < DISPLAY_ROWS; row++) {
            for (int col = 0; col < DISPLAY_COLS; col++) {

                float left = col * cellWidth;
                float top = row * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;

                // 실제 데이터 영역만 색칠
                if (row > 0 && col > 0) {
                    String color = cells[row - 1][col - 1];

                    if (color != null) {
                        try {
                            fillPaint.setColor(Color.parseColor(color));
                        } catch (Exception e) {
                            fillPaint.setColor(Color.GRAY);
                        }

                        canvas.drawRect(left, top, right, bottom, fillPaint);
                    }
                }

                // 격자선
                canvas.drawRect(left, top, right, bottom, linePaint);
            }
        }

        // 상단 분 표시
        String[] minutes = {"0", "10", "20", "30", "40", "50"};

        for (int col = 1; col < DISPLAY_COLS; col++) {
            float x = col * cellWidth + cellWidth / 2f;
            float y = cellHeight / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f);

            canvas.drawText(minutes[col - 1], x, y, textPaint);
        }

        // 좌측 시간 표시
        for (int row = 1; row < DISPLAY_ROWS; row++) {
            float x = cellWidth / 2f;
            float y = row * cellHeight + cellHeight / 2f
                    - ((textPaint.descent() + textPaint.ascent()) / 2f);

            canvas.drawText(String.valueOf(row - 1), x, y, textPaint);
        }

        // 드래그 묶음 라벨 표시
        for (Integer groupId : groupLabels.keySet()) {

            String label = groupLabels.get(groupId);

            if (label == null || label.isEmpty()) {
                continue;
            }

            GroupBounds bounds = findGroupBounds(groupId);

            if (bounds == null) {
                continue;
            }

            float left = (bounds.minCol + 1) * cellWidth;
            float top = (bounds.minRow + 1) * cellHeight;
            float right = (bounds.maxCol + 2) * cellWidth;
            float bottom = (bounds.maxRow + 2) * cellHeight;

            float centerX = (left + right) / 2f;
            float centerY = (top + bottom) / 2f
                    - ((textPaint.descent() + textPaint.ascent()) / 2f);

            drawSpacedLabel(
                    canvas,
                    label,
                    bounds,
                    centerY
            );
        }


    }

    // 해당 묶음의 첫 번째 칸 찾기
    private GroupBounds findGroupBounds(int targetGroupId) {

        GroupBounds bounds = null;

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                if (groupIds[row][col] == targetGroupId) {

                    if (bounds == null) {
                        bounds = new GroupBounds(row, col);
                    } else {
                        bounds.include(row, col);
                    }
                }
            }
        }

        return bounds;
    }

    private String fitLabelToWidth(String label, float maxWidth) {

        if (label == null) {
            return "";
        }

        if (textPaint.measureText(label) <= maxWidth) {
            return label;
        }

        String ellipsis = "…";

        for (int i = label.length(); i > 0; i--) {

            String candidate =
                    label.substring(0, i) + ellipsis;

            if (textPaint.measureText(candidate) <= maxWidth) {
                return candidate;
            }
        }

        return ellipsis;
    }

    private void drawSpacedLabel(
            Canvas canvas,
            String label,
            GroupBounds bounds,
            float centerY
    ) {
        if (label == null || label.trim().isEmpty()) {
            return;
        }

        String trimmedLabel = label.trim();

        if (trimmedLabel.length() > 7) {
            trimmedLabel = trimmedLabel.substring(0, 7);
        }

        float left = (bounds.minCol + 1) * cellWidth;
        float right = (bounds.maxCol + 2) * cellWidth;
        float maxWidth = right - left - 4f;

        float centerX = (left + right) / 2f;

        canvas.save();

        float textWidth = labelPaint.measureText(trimmedLabel);

        if (textWidth > maxWidth && textWidth > 0) {
            float scaleX = maxWidth / textWidth;
            canvas.scale(scaleX, 1f, centerX, centerY);
        }

        canvas.drawText(
                trimmedLabel,
                centerX,
                centerY,
                labelPaint
        );

        canvas.restore();
    }


    //화면 그리기 끝--------------------------------------------------------------------------


    //터치 처리 시작--------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // 미니 타임테이블 수정 방지
        // false면 편집 불가능
        if (!editable) {
            return true;
        }

        // 칸 크기 계산 전이면 종료
        if (cellWidth <= 0 || cellHeight <= 0) {
            return true;
        }

        // 현재 터치 위치를 칸 번호로 변환
        int displayCol = (int) (event.getX() / cellWidth);
        int displayRow = (int) (event.getY() / cellHeight);

// 라벨 영역 터치 무시
        if (displayCol <= 0 || displayRow <= 0) {
            return true;
        }

        int col = displayCol - 1;
        int row = displayRow - 1;

        // 범위 밖 방지
        if (col < 0 || col >= COLS ||
                row < 0 || row >= ROWS) {

            return true;
        }

        // 라벨 수정 모드일 때는 색칠하지 않고 라벨 수정 요청만 보냄
        if (labelEditMode && event.getAction() == MotionEvent.ACTION_DOWN) {

            int groupId = groupIds[row][col];

            if (groupId != -1 && labelEditRequestListener != null) {

                String currentLabel = groupLabels.get(groupId);

                labelEditRequestListener.onLabelEditRequest(
                        groupId,
                        currentLabel
                );
            }

            return true;
        }

        switch (event.getAction()) {

            // 처음 눌렀을 때
            case MotionEvent.ACTION_DOWN:

                isDragging = true;

                clearVisited();

                currentChanges = new ArrayList<>();

                // 이번 드래그의 묶음 번호 생성
                currentGroupId = nextGroupId++;

                paintCell(row, col);

                break;

            // 드래그 중
            case MotionEvent.ACTION_MOVE:

                if (isDragging) {

                    // 지나가는 칸 색칠
                    paintCell(row, col);
                }

                break;

            // 손 뗐을 때
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                isDragging = false;

                if (eraserMode && isEnoughCellsVisitedForClearAll()) {

                    if (requestClearAllListener != null) {
                        requestClearAllListener.onRequestClearAll();
                    }

                    currentGroupId = -1;
                    break;
                }

                if (currentChanges != null &&
                        !currentChanges.isEmpty()) {

                    undoStack.push(currentChanges);

                    // 지우개 모드가 아닐 때만 라벨 입력 요청
                    if (!eraserMode && dragCompleteListener != null) {
                        dragCompleteListener.onDragComplete(currentGroupId);
                    }
                }

                currentGroupId = -1;

                break;
        }

        return true;
    }

    //터치 처리 끝--------------------------------------------------------------------------


    //실제 칸 색칠 시작--------------------------------------------------------------------------

    private void paintCell(int row, int col) {

        if (visited[row][col]) {
            return;
        }

        visited[row][col] = true;

        String beforeColor = cells[row][col];
        int beforeGroupId = groupIds[row][col];

        // 지우개 모드
        if (eraserMode) {

            if (beforeGroupId == -1) {
                return;
            }

            eraseGroup(beforeGroupId);
            return;
        }

        // 색칠 모드
        if (selectedColor.equals(beforeColor)) {
            return;
        }

        cells[row][col] = selectedColor;
        groupIds[row][col] = currentGroupId;

        CellChange change = new CellChange(
                row,
                col,
                beforeColor,
                selectedColor,
                beforeGroupId,
                currentGroupId
        );

        if (currentChanges != null) {
            currentChanges.add(change);
        }

        invalidate();
    }

    //실제 칸 색칠 끝--------------------------------------------------------------------------


    //방문 기록 초기화 시작--------------------------------------------------------------------------

    private void clearVisited() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                visited[row][col] = false;
            }
        }
    }

    //방문 기록 초기화 끝--------------------------------------------------------------------------

    // 전체 칸을 지나갔는지 확인
    // 전체 칸 중 일정 비율 이상 지나갔는지 확인
    private boolean isEnoughCellsVisitedForClearAll() {

        int visitedCount = 0;
        int totalCount = ROWS * COLS;

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                if (visited[row][col]) {
                    visitedCount++;
                }
            }
        }

        float ratio = visitedCount / (float) totalCount;

        return ratio >= 0.8f;
    }



    //색상 변경 시작--------------------------------------------------------------------------

    // 현재 선택 색상 변경
    public void setSelectedColor(String color) {

        selectedColor = color;
    }

    //색상 변경 끝--------------------------------------------------------------------------

    // 드래그 묶음 라벨 저장
    public void setGroupLabel(int groupId, String label) {

        if (groupId == -1) {
            return;
        }

        if (label == null || label.trim().isEmpty()) {
            groupLabels.remove(groupId);
        } else {
            groupLabels.put(groupId, label.trim());
        }

        invalidate();
    }


    //Undo 기능 시작--------------------------------------------------------------------------

    private void removeUnusedLabels() {

        List<Integer> unusedLabels = new ArrayList<>();

        for (Integer groupId : groupLabels.keySet()) {
            if (findGroupBounds(groupId) == null) {
                unusedLabels.add(groupId);
            }
        }

        for (Integer groupId : unusedLabels) {
            groupLabels.remove(groupId);
        }
    }

    // 마지막 작업 되돌리기
    public void undoLastAction() {

        if (undoStack.isEmpty()) {
            return;
        }

        List<CellChange> lastChanges = undoStack.pop();

        for (CellChange change : lastChanges) {
            cells[change.row][change.col] = change.beforeColor;
            groupIds[change.row][change.col] = change.beforeGroupId;
        }

        removeUnusedLabels();

        invalidate();
    }

    //Undo 기능 끝--------------------------------------------------------------------------


    // 라벨 수정 모드 ON/OFF
    public void setLabelEditMode(boolean labelEditMode) {
        this.labelEditMode = labelEditMode;
    }

    // 라벨 수정 요청 리스너 연결
    public void setOnLabelEditRequestListener(
            OnLabelEditRequestListener listener
    ) {
        this.labelEditRequestListener = listener;
    }

    // 라벨 수정 요청 인터페이스
    public interface OnLabelEditRequestListener {
        void onLabelEditRequest(int groupId, String currentLabel);
    }


    //지우개 함수 시작 ------------------------------------------------
    private void eraseGroup(int targetGroupId) {

        List<CellChange> eraseChanges = new ArrayList<>();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {

                if (groupIds[row][col] == targetGroupId) {

                    CellChange change = new CellChange(
                            row,
                            col,
                            cells[row][col],
                            null,
                            groupIds[row][col],
                            -1
                    );

                    eraseChanges.add(change);

                    cells[row][col] = null;
                    groupIds[row][col] = -1;
                }
            }
        }

        if (!eraseChanges.isEmpty()) {
            undoStack.push(eraseChanges);
        }

        groupLabels.remove(targetGroupId);

        invalidate();
    }
    //지우개 함수 끝---------------------------------------------


    //전체 삭제 시작--------------------------------------------------------------------------

    // 전체 칸 삭제
    public void clearAll() {

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                cells[row][col] = null;
                groupIds[row][col] = -1;
            }
        }

        undoStack.clear();
        groupLabels.clear();
        nextGroupId = 1;
        currentGroupId = -1;

        invalidate();
    }

    // 전체 삭제 요청 리스너 연결
    public void setOnRequestClearAllListener(
            OnRequestClearAllListener listener
    ) {
        this.requestClearAllListener = listener;
    }

    //전체 삭제 끝--------------------------------------------------------------------------



    //데이터 반환 시작--------------------------------------------------------------------------

    // 현재 셀 데이터 반환
    public String[][] getCells() {

        return copyCells(cells);
    }

    public int[][] getGroupIds() {
        int[][] copied = new int[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                copied[row][col] = groupIds[row][col];
            }
        }

        return copied;
    }

    public Map<Integer, String> getGroupLabels() {
        return new HashMap<>(groupLabels);
    }

    //데이터 반환 끝--------------------------------------------------------------------------



    //데이터 적용 시작--------------------------------------------------------------------------

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

        // 화면 다시 그리기
        invalidate();
    }

    public void setGroupIds(int[][] newGroupIds) {
        if (newGroupIds == null) return;

        int maxGroupId = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                groupIds[row][col] = newGroupIds[row][col];
                maxGroupId = Math.max(maxGroupId, newGroupIds[row][col]);
            }
        }
        nextGroupId = maxGroupId + 1;

        invalidate();
    }

    public void setGroupLabels(Map<Integer, String> newLabels) {
        groupLabels.clear();

        if (newLabels != null) {
            groupLabels.putAll(newLabels);
        }

        invalidate();
    }

    //데이터 적용 끝--------------------------------------------------------------------------



    //배열 복사 시작--------------------------------------------------------------------------

    // 배열 깊은 복사용
    private String[][] copyCells(String[][] source) {

        String[][] copied =
                new String[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {

            for (int col = 0; col < COLS; col++) {

                copied[row][col] = source[row][col];
            }
        }

        return copied;
    }

    //배열 복사 끝--------------------------------------------------------------------------



    //XML Preview 안정화용 시작--------------------------------------------------------------------------

    @Override
    protected void onMeasure(int widthMeasureSpec,
                             int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec,
                heightMeasureSpec);
    }

    //XML Preview 안정화용 끝--------------------------------------------------------------------------


    private static class GroupBounds {

        int minRow;
        int maxRow;
        int minCol;
        int maxCol;

        GroupBounds(int row, int col) {
            minRow = row;
            maxRow = row;
            minCol = col;
            maxCol = col;
        }

        void include(int row, int col) {
            if (row < minRow) minRow = row;
            if (row > maxRow) maxRow = row;
            if (col < minCol) minCol = col;
            if (col > maxCol) maxCol = col;
        }
    }

    //Undo 데이터 저장 클래스 시작--------------------------------------------------------------------------

    // 칸 변경 기록 클래스
    // Undo 기능에 사용
    public static class CellChange {

        public int row;
        public int col;

        public String beforeColor;
        public String afterColor;

        public int beforeGroupId;
        public int afterGroupId;

        public CellChange(
                int row,
                int col,
                String beforeColor,
                String afterColor,
                int beforeGroupId,
                int afterGroupId
        ) {

            this.row = row;
            this.col = col;

            this.beforeColor = beforeColor;
            this.afterColor = afterColor;

            this.beforeGroupId = beforeGroupId;
            this.afterGroupId = afterGroupId;
        }
    }

    //Undo 데이터 저장 클래스 끝--------------------------------------------------------------------------
}