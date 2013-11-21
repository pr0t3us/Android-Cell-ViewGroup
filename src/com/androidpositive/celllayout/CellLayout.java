package com.androidpositive.celllayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

/**
 * A layout manager that allows splitting parents area into evenly sized cells grid. Each child can be positioned acress one or several cells.
 *  
 */
public class CellLayout extends ViewGroup implements OnTouchListener, OnLongClickListener {
    public static final String T = "CellLayout";
    /**
     * Default size in dp that will be used for a cell in case no other clues were given by parent.
     */
    private static final int DEFAULT_CELL_SIZE = 48;
    
    private static int ANIMATION_DURATION = 250;

    /**
     * Number of coumns.
     */
    private int columns = 4;

    /**
     * An optional margin to be applied to each child.
     */
    private int spacing = 0;

    private float cellSize;

    /*-------------------------*/   
    
    private SparseIntArray newPositions = new SparseIntArray();

    private OnClickListener onClickListener = null;

    private int initialX;
    private int initialY;

    private int lastTouchX;
    private int lastTouchY;

    private boolean movingView;
    private int dragged = -1;
    
    private int pageWidth;
    private int pageHeight;

    private int biggestChildWidth;
    private int biggestChildHeight;

    private int computedRowCount;


    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initAttrs(context, attrs);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        initAttrs(context, attrs);
    }

    public CellLayout(Context context) {
        super(context);
    }

    public void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CellLayout, 0, 0);

        try {
            columns = a.getInt(R.styleable.CellLayout_columns, 4);
            spacing = a.getDimensionPixelSize(R.styleable.CellLayout_spacing, 0);
        } finally {
            a.recycle();
        }
        init();
    }

    private void init() {
        setOnTouchListener(this);
        setOnLongClickListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = 0;

        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            cellSize = (float) (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()) / (float) columns;
        } else {
            cellSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CELL_SIZE, getResources().getDisplayMetrics());
            width = (int) (columns * cellSize);
        }

        int childCount = getChildCount();
        View child;

        int maxRow = 0;

        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);

            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int top = layoutParams.top;
            int w = layoutParams.width;
            int h = layoutParams.height;

            int bottom = top + h;

            int childWidthSpec = MeasureSpec.makeMeasureSpec((int) (w * cellSize) - spacing * 2, MeasureSpec.EXACTLY);
            int childHeightSpec = MeasureSpec.makeMeasureSpec((int) (h * cellSize) - spacing * 2, MeasureSpec.EXACTLY);
            child.measure(childWidthSpec, childHeightSpec);

            if (bottom > maxRow) {
                maxRow = bottom;
            }

        }

        int measuredHeight = Math.round(maxRow * cellSize) + getPaddingTop() + getPaddingBottom();
        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            int atMostHeight = MeasureSpec.getSize(heightMeasureSpec);
            height = Math.min(atMostHeight, measuredHeight);
        } else {
            height = measuredHeight;
        }
        searchBiggestChildMeasures();
        // Log.e(T, "::onMeasure:" + "setMeasuredDimension " + ";width=" + width + ";height=" + height);
        // Log.e(T, "::onMeasure:" + "setMeasuredDimension " + ";biggestChildWidth=" + biggestChildWidth + ";biggestChildHeight=" + biggestChildHeight);
        setMeasuredDimension(width, height);
        computedRowCount = maxRow;
        pageWidth = width;
        pageHeight = height;
        // Log.w(T, "::onMeasure:" + "maxRow="+maxRow);
        // Log.w(T, "::onMeasure:" + "cellsize="+cellSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();

        View child;
        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);

            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int top = (int) (layoutParams.top * cellSize) + getPaddingTop() + spacing;
            int left = (int) (layoutParams.left * cellSize) + getPaddingLeft() + spacing;
            int right = (int) ((layoutParams.left + layoutParams.width) * cellSize) + getPaddingLeft() - spacing;
            int bottom = (int) ((layoutParams.top + layoutParams.height) * cellSize) + getPaddingTop() - spacing;

            child.layout(left, top, right, bottom);
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        /**
         * An Y coordinate of the top most cell the view resides in.
         */
        int top = 0;

        /**
         * An X coordinate of the left most cell the view resides in.
         */
        int left = 0;

        /**
         * Number of cells occupied by the view horizontally.
         */
        int width = 1;

        /**
         * Number of cells occupied by the view vertically.
         */
        int height = 1;

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout);
            left = a.getInt(R.styleable.CellLayout_layout_left, 0);
            top = a.getInt(R.styleable.CellLayout_layout_top, 0);
            height = a.getInt(R.styleable.CellLayout_layout_cellsHeight, -1);
            width = a.getInt(R.styleable.CellLayout_layout_cellsWidth, -1);

            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams params) {
            super(params);

            if (params instanceof LayoutParams) {
                LayoutParams cellLayoutParams = (LayoutParams) params;
                left = cellLayoutParams.left;
                top = cellLayoutParams.top;
                height = cellLayoutParams.height;
                width = cellLayoutParams.width;
            }
        }

        public LayoutParams() {
            this(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

    }

    private void searchBiggestChildMeasures() {
        biggestChildWidth = 0;
        biggestChildHeight = 0;
        for (int index = 0; index < getItemViewCount(); index++) {
            View child = getChildAt(index);

            if (biggestChildHeight < child.getMeasuredHeight()) {
                biggestChildHeight = child.getMeasuredHeight();
            }

            if (biggestChildWidth < child.getMeasuredWidth()) {
                biggestChildWidth = child.getMeasuredWidth();
            }
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return onTouch(null, event);
    }

    @Override
    public boolean onLongClick(View v) {
        Log.i(T, ":Touch:onLongClick:" + "" + positionForView(v));
        if (positionForView(v) != -1) {
            
            movingView = true;
            dragged = positionForView(v);
            draggedChildPosition  = positionForView(v);
            draggedView = getChildAt(draggedChildPosition);
            bringDraggedToFront();

            animateMoveAllItems();

            animateDragged();

            return true;
        }

        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Log.i(T, "::onTouch:" + "");
        int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(event);
                break;
        }
        if (aViewIsDragged()) return true;
        return false;
    }
    


    private void touchDown(MotionEvent event) {
        Log.d(T, "::touchDown:" + "");
        initialX = (int) event.getRawX();
        initialY = (int) event.getRawY();
        
        // lastTouchX = (int)event.getRawX() + (currentPage() * gridPageWidth);
        // lastTouchY = (int)event.getRawY();
    }  
    
    //TODO: have swapping childs done
    private void touchMove(MotionEvent event) {
        if (movingView && aViewIsDragged()) {
            lastTouchX = (int) event.getX();
            lastTouchY = (int) event.getY();
            // Log.i(T, "::touchMove:" + "lastTouchX="+lastTouchX+";lastTouchY="+lastTouchY);

            ensureThereIsNoArtifact();

            moveDraggedView(lastTouchX, lastTouchY);
            manageSwapPosition(lastTouchX, lastTouchY);
            // manageEdgeCoordinates(lastTouchX);
            // manageDeleteZoneHover(lastTouchX, lastTouchY);
        }
    }

    private void touchUp(MotionEvent event) {
//        View draggedChild = getChildAt(draggedChildPosition);
//        View swapChild = getChildAt(swapChildPosition);
        Log.i(T, "::touchUp:" + "!aViewIsDragged()="+!aViewIsDragged());
        Log.i(T, "::touchUp:" + "dragged "+dragged);
        Log.i(T, "::touchUp:" + "draggedChildPosition="+draggedChildPosition+";swapChildPosition="+swapChildPosition);
        Log.i(T, "::touchUp:" + "draggedChild != null "+(draggedView != null));
        Log.i(T, "::touchUp:" + "swapChild != null "+(swapView != null));

        if (!aViewIsDragged()) {
            
            // if(onClickListener != null) {
            // View clickedView = getChildAt(getTargetAtCoor((int) event.getX(), (int) event.getY()));
            // if(clickedView != null)
            // onClickListener.onClick(clickedView);
            // }
        } else {
            cancelAnimations();
            if (draggedView != null && swapView != null) {
//              swapViews(draggedChild, swapChild);
              swapChildViews(dragged, swapChildPosition);
              invalidate();
              requestLayout();
          }
            // manageChildrenReordering();
            // hideDeleteView();
            // cancelEdgeTimer();

            movingView = false;
            dragged = -1;
            // container.enableScroll();

        }
    }

    private int positionForView(View v) {
        for (int index = 0; index < getItemViewCount(); index++) {
            View child = getChildView(index);
            if (isPointInsideView(initialX, initialY, child)) {
                Log.i(T, "::positionForView:" + "index " + index + "  x=" + initialX + ";y=" + initialY);
                return index;
            }
        }
        Log.i(T, "::positionForView:" + "index -1 ");
        return -1;
    }

    private boolean isPointInsideView(float x, float y, View view) {
        // Log.i(T, "::isPointInsideView:" + "x="+x+";y="+y);
        // int location[] = new int[2];
        // view.getLocationOnScreen(location);
        // int viewX = location[0];
        // int viewY = location[1];
        return inViewBounds(view, (int) x, (int) y);
        // if (pointIsInsideViewBounds(x, y, view, viewX, viewY)) {
        // return true;
        // } else {
        // return false;
        // }
    }

    public boolean pointIsInsideViewBounds(float x, float y, View view, int viewX, int viewY) {
        return (x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()));
    }

    Rect outRect = new Rect();
    int[] location = new int[2];

    private boolean inViewBounds(View view, int x, int y) {
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    private int getItemViewCount() {
        // -1 to remove the DeleteZone from the loop
        // return getChildCount()-1;
        return getChildCount();
    }

    private View getChildView(int index) {
        // if (weWereMovingDraggedBetweenPages()) {
        // if (index >= dragged) {
        // return getChildAt(index -1);
        // }
        // }
        return getChildAt(index);

    }



    private void bringDraggedToFront() {
        View draggedView = getChildAt(dragged);
        draggedView.bringToFront();
    }

    private void animateMoveAllItems() {
        Log.i(T, "::animateMoveAllItems:" + "");
        Animation rotateAnimation = createFastRotateAnimation();

        for (int i = 0; i < getItemViewCount(); i++) {
            View child = getChildAt(i);
            child.startAnimation(rotateAnimation);
        }
    }

    private Animation createFastRotateAnimation() {
        Animation rotate = new RotateAnimation(-2.0f, 2.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        rotate.setRepeatMode(Animation.REVERSE);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setDuration(60);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());

        return rotate;
    }

    private void animateDragged() {

        ScaleAnimation scale = new ScaleAnimation(1f, 1.4f, 1f, 1.4f, biggestChildWidth / 2, biggestChildHeight / 2);
        scale.setDuration(200);
        scale.setFillAfter(true);
        scale.setFillEnabled(true);

        // if (aViewIsDragged()) {
        View draggedView = getDraggedView();
        // Log.e("animateDragged", ((TextView)draggedView.findViewWithTag("text")).getText().toString());
        if (draggedView != null) {
            draggedView.clearAnimation();
            draggedView.startAnimation(scale);
        }
        // }
    }

    private void cancelAnimations() {
        for (int i = 0; i < getItemViewCount(); i++) {
            View child = getChildAt(i);
            child.clearAnimation();
        }
    }

    private boolean aViewIsDragged() {
        return weWereMovingDraggedBetweenPages();
    }

    private boolean weWereMovingDraggedBetweenPages() {
        return dragged != -1;
    }

    private View getDraggedView() {

        // Log.i(T, "::getDraggedView:" + "dragged " + dragged + " from " + getChildCount());
        // return getChildAt(2);
        return getChildAt(getChildCount() - 1);
        // return getChildAt(dragged);
    }
    
    private void ensureThereIsNoArtifact() {
        invalidate();
    }
    
    int draggedChildPosition;
    int swapChildPosition;
    
    View draggedView;
    View swapView;

    private void moveDraggedView(int x, int y) {
        View childAt = getDraggedView();

        int width = childAt.getMeasuredWidth();
        int height = childAt.getMeasuredHeight();

        int l = x - (1 * width / 2);
        int t = y - (1 * height / 2);

        childAt.layout(l, t, l + width, t + height);
//        draggedChildPosition = lastTarget;
//        draggedChildPosition = indexOfChild(childAt);
    }

    private void manageSwapPosition(int x, int y) {
        int positionInPage = getPositionInPage(x, y);
        swapChildPosition = positionInPage-1;
        if (swapChildPosition < 0) {
            swapChildPosition = 0;
        } else if (swapChildPosition >= getChildCount()) {
            swapChildPosition = getChildCount() - 1;
        }
        swapView = getChildAt(swapChildPosition);
//        int target = positionOfItem(positionInPage);
//        if (childHasMoved(target) && target != lastTarget) {
//            animateGap(target, positionInPage);
//            lastTarget = target;
//        }
    }

    private int getTargetAtCoor(int x, int y) {
        int positionInPage = getPositionInPage(x, y);
        int positionOfItem = positionOfItem(positionInPage);
        Log.e(T, "::getTargetAtCoor:" + "positionInPage = " + positionInPage + ";positionOfItem = " + positionOfItem);
        // return positionOfItem(page, positionInPage);
        return positionOfItem;
    }

    private int getPositionInPage(int x, int y) {
        int col = getColumnOfCoordinate(x);
        int row = getRowOfCoordinate(y);
        Log.w(T, "::getTargetAtCoor:" + "col=" + col + ";row=" + row);
        int positionInPage = col + (row * columns);
        return positionInPage;
    }

    private int getColumnOfCoordinate(int x) {
        int col = 0;
        for (int i = 1; i <= columns; i++) {
            int colRightBorder = (int) ((i * cellSize));
            if (x < colRightBorder) {
                break;
            }
            col++;
        }
        Log.i(T, "::getColumnOfCoordinate:" + "col=" + col);
        return col;
    }

    private int getRowOfCoordinate(int y) {
        int row = 0;
        for (int i = 1; i <= computedRowCount; i++) {
            if (y < i * cellSize) {
                break;
            }
            row++;
        }
        return row;
    }

    private int positionOfItem(int childIndex) {
        int currentGlobalIndex = 0;

        // for (int currentItemIndex = 0; currentItemIndex < getItemViewCount(); currentItemIndex++) {
        // if (childIndex == currentItemIndex) {
        // return currentGlobalIndex;
        // }
        // currentGlobalIndex++;
        // }
        for (int index = 0; index < getItemViewCount(); index++) {

            View child = getChildAt(index);
            int childWidth = child.getMeasuredWidth();
            Log.i(T, "::positionOfItem:" + "childWidth=" + childWidth);
            if (childWidth <= cellSize) {
                // Log.i(T, "::positionOfItem:" + "childWidth <= cellSize");
                currentGlobalIndex++;
            } else {
                currentGlobalIndex = (currentGlobalIndex + (int) (childWidth / cellSize));
                // Log.e(T, "::positionOfItem:" + "currentGlobalIndex="+currentGlobalIndex);
            }
            if (childIndex == currentGlobalIndex) {
                return currentGlobalIndex;
            }
        }

        return -1;
    }

    private boolean childHasMoved(int position) {
        return position != -1;
    }

    private void animateGap(int targetLocationInGrid, int positionInPage) {
        Log.i(T, "::animateGap:" + "targetLocationInGrid="+targetLocationInGrid+";positionInPage="+positionInPage);
        int viewAtPosition = positionInPage;
//        int viewAtPosition = currentViewAtPosition(targetLocationInGrid);
        
        Log.i(T, "::animateGap:" + "viewAtPosition="+viewAtPosition);
        if (viewAtPosition == dragged) {
            return;
        }

        View targetView = getChildView(viewAtPosition);
        if (targetView == null) {
            return;
        }

        // Log.e("animateGap target",
        // ((TextView)targetView.findViewWithTag("text")).getText().toString());

        Point oldXY = getCoorForIndex(viewAtPosition);
        Point newXY = getCoorForIndex(newPositions.get(dragged, dragged));

        Point oldOffset = computeTranslationStartDeltaRelativeToRealViewPosition(positionInPage, viewAtPosition, oldXY);
        Point newOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, newXY);

        animateMoveToNewPosition(targetView, oldOffset, newOffset);
        saveNewPositions(positionInPage, viewAtPosition);
    }
    
    private void saveNewPositions(int targetLocation, int viewAtPosition) {
        newPositions.put(viewAtPosition, newPositions.get(dragged, dragged));
        newPositions.put(dragged, targetLocation);
//        tellAdapterToSwapDraggedWithTarget(newPositions.get(dragged, dragged), newPositions.get(viewAtPosition, viewAtPosition));
    }

    private int currentViewAtPosition(int targetLocation) {
        int viewAtPosition = targetLocation;
        for (int i = 0; i < newPositions.size(); i++) {
            int value = newPositions.valueAt(i);
            if (value == targetLocation) {
                viewAtPosition = newPositions.keyAt(i);
                break;
            }
        }
        return viewAtPosition;
    }

    private Point getCoorForIndex(int index) {

        int row = index / columns;
        int col = index - (row * columns);

        int x = (int) (cellSize * col);
        int y = (int) (cellSize * row);

        return new Point(x, y);
    }
    
    private Point getCoorForIndex(int index, int i) {
        return null;
//        ItemPosition page = itemInformationAtPosition(index);
//
//        int row = page.itemIndex / computedColumnCount;
//        int col = page.itemIndex - (row * computedColumnCount);
//
//        int x = (currentPage() * gridPageWidth) + (columnWidthSize * col);
//        int y = rowHeightSize * row;
//
//        return new Point(x, y);
    }

    
    private Point computeTranslationEndDeltaRelativeToRealViewPosition(Point oldXY, Point newXY) {
        return new Point(newXY.x - oldXY.x, newXY.y - oldXY.y);
    }

    private Point computeTranslationStartDeltaRelativeToRealViewPosition(int targetLocation, int viewAtPosition, Point oldXY) {
        Point oldOffset;
        if (viewWasAlreadyMoved(targetLocation, viewAtPosition)) {
            Point targetLocationPoint = getCoorForIndex(targetLocation);
            oldOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, targetLocationPoint);
        } else {
            oldOffset = new Point(0, 0);
        }
        return oldOffset;
    }
    
    private boolean viewWasAlreadyMoved(int targetLocation, int viewAtPosition) {
        Log.i(T, "::viewWasAlreadyMoved:" + (viewAtPosition != targetLocation));
        return viewAtPosition != targetLocation;
    }
    
    private void animateMoveToNewPosition(View targetView, Point oldOffset, Point newOffset) {
        AnimationSet set = new AnimationSet(true);

        Animation rotate = createFastRotateAnimation();
        Animation translate = createTranslateAnimation(oldOffset, newOffset);

        set.addAnimation(rotate);
        set.addAnimation(translate);

        targetView.clearAnimation();
        targetView.startAnimation(set);
    }

    private TranslateAnimation createTranslateAnimation(Point oldOffset, Point newOffset) {
        TranslateAnimation translate =
                new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x, Animation.ABSOLUTE, newOffset.x, Animation.ABSOLUTE, oldOffset.y, Animation.ABSOLUTE,
                        newOffset.y);
        translate.setDuration(ANIMATION_DURATION);
        translate.setFillEnabled(true);
        translate.setFillAfter(true);
        translate.setInterpolator(new AccelerateDecelerateInterpolator());
        return translate;
    }
    
    private void swapChildViews(int childPos1, int childPos2) {
        Log.e(T, "::swapChildViews:" + "childPos2 "+childPos2);
        Log.e(T, "::swapChildViews:" + "childCount "+getChildCount());
        View child1 = getDraggedView();
//                getChildAt(childPos1);
        View child2 = getChildAt(childPos2);
//        Rect child1Bounds = getViewBounds(child1);
//        Rect child2Bounds = getViewBounds(child2);
        removeView(child1);
        addView(child1, childPos2);
        removeView(child2);
        addView(child2, childPos1);
//        child1.setClipBounds(child2Bounds);
//        child2.setClipBounds(child1Bounds);
        swapViews(child1, child2);
    }
    
    private void swapViews(View child1, View child2) {
        LayoutParams child1Params = (LayoutParams) child1.getLayoutParams();
        LayoutParams child2Params = (LayoutParams) child2.getLayoutParams();
        LayoutParams tmp = new LayoutParams(child1Params);

        swap(child1Params, child2Params);
        swap(child2Params, tmp);
    }
    
    private static void swap(LayoutParams dst, LayoutParams src) {
        dst.width = src.width;
        dst.height = src.height;
        dst.top = src.top;
        dst.left = src.left;
    }
    
    private static Rect getViewBounds(View child) {
        return new Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
    }
    
    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
    }    


}
