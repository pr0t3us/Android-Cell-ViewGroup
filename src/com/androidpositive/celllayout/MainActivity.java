package com.androidpositive.celllayout;

import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnPreDrawListener;

import com.androidpositive.celllayout.CellLayout.LayoutParams;

public class MainActivity extends Activity {

    private CellLayout cellLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cellLayout = (CellLayout) findViewById(R.id.cell_layout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.rearrange:
                doRandomRearrange();
                return true;
            case R.id.resset:
                setContentView(R.layout.activity_main);
                cellLayout = (CellLayout) findViewById(R.id.cell_layout);
                cellLayout.invalidate();
                cellLayout.refreshDrawableState();
                return true;
        }
        return true;
    }

    private static class ChildPair {
        View child1;
        View child2;
        Rect child1Bounds;
        Rect child2Bounds;
    }
    
    private void doRearrange() {
        Random rnd = new Random();

        ArrayList<View> children = new ArrayList<View>();
        final ArrayList<ChildPair> pairs = new ArrayList<ChildPair>();
        for (int i = 0; i < cellLayout.getChildCount(); i++) {
            children.add(cellLayout.getChildAt(i));
        }

        int pairsSize = cellLayout.getChildCount() / 2;
        for (int i = 0; i < pairsSize; i++) {
            ChildPair pair = new ChildPair();
            pair.child1 = children.remove(i);

            pair.child1Bounds = getViewBounds(pair.child1);

            pairs.add(pair);
        }

        for (ChildPair childPair : pairs) {
            final View child1 = childPair.child1;
            final View child2 = childPair.child2;

            LayoutParams child1Params = (LayoutParams) child1.getLayoutParams();
            LayoutParams child2Params = (LayoutParams) child2.getLayoutParams();
            LayoutParams tmp = new LayoutParams(child1Params);

            swap(child1Params, child2Params);
            swap(child2Params, tmp);
        }
        cellLayout.requestLayout();

        cellLayout.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                cellLayout.getViewTreeObserver().removeOnPreDrawListener(this);

                // ArrayList<ValueAnimator> animators = new ArrayList<ValueAnimator>();
                // for (ChildPair childPair : pairs) {
                // Rect child1New = getViewBounds(childPair.child1);
                // Rect child2New = getViewBounds(childPair.child2);
                //
                // animators.add(createAnimator(childPair.child1, childPair.child1Bounds, child1New));
                // animators.add(createAnimator(childPair.child2, childPair.child2Bounds, child2New));
                // }
                //
                // AnimatorSet as = new AnimatorSet();
                // as.playTogether(animators.toArray(new ValueAnimator[animators.size()]));
                // as.setDuration(300);
                // as.setInterpolator(new AccelerateDecelerateInterpolator());
                // as.start();
                return true;
            }
        });

    }

    private void doRandomRearrange() {
        Random rnd = new Random();

        ArrayList<View> children = new ArrayList<View>();
        final ArrayList<ChildPair> pairs = new ArrayList<ChildPair>();
        for (int i = 0; i < cellLayout.getChildCount(); i++) {
            children.add(cellLayout.getChildAt(i));
        }

        int pairsSize = cellLayout.getChildCount() / 2;
        for (int i = 0; i < pairsSize; i++) {
            ChildPair pair = new ChildPair();
            int randomIdx = rnd.nextInt(children.size());
            pair.child1 = children.remove(randomIdx);
            randomIdx = rnd.nextInt(children.size());
            pair.child2 = children.remove(randomIdx);

            pair.child1Bounds = getViewBounds(pair.child1);
            pair.child2Bounds = getViewBounds(pair.child2);

            pairs.add(pair);
        }

        for (ChildPair childPair : pairs) {
            final View child1 = childPair.child1;
            final View child2 = childPair.child2;

            LayoutParams child1Params = (LayoutParams) child1.getLayoutParams();
            LayoutParams child2Params = (LayoutParams) child2.getLayoutParams();
            LayoutParams tmp = new LayoutParams(child1Params);

            swap(child1Params, child2Params);
            swap(child2Params, tmp);
        }
        cellLayout.requestLayout();

        cellLayout.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                cellLayout.getViewTreeObserver().removeOnPreDrawListener(this);

                // ArrayList<ValueAnimator> animators = new ArrayList<ValueAnimator>();
                // for (ChildPair childPair : pairs) {
                // Rect child1New = getViewBounds(childPair.child1);
                // Rect child2New = getViewBounds(childPair.child2);
                //
                // animators.add(createAnimator(childPair.child1, childPair.child1Bounds, child1New));
                // animators.add(createAnimator(childPair.child2, childPair.child2Bounds, child2New));
                // }
                //
                // AnimatorSet as = new AnimatorSet();
                // as.playTogether(animators.toArray(new ValueAnimator[animators.size()]));
                // as.setDuration(300);
                // as.setInterpolator(new AccelerateDecelerateInterpolator());
                // as.start();
                return true;
            }
        });

    }


    private static Rect getViewBounds(View child) {
        return new Rect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
    }

    private static void swap(LayoutParams dst, LayoutParams src) {
        dst.width = src.width;
        dst.height = src.height;
        dst.top = src.top;
        dst.left = src.left;
    }

}
