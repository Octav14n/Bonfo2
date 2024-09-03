package eu.schnuff.bonfo2

import android.graphics.Rect;
import android.os.Build
import android.view.View;
import android.view.WindowInsets;

import me.zhanghai.android.fastscroll.FastScroller;

class ScrollingViewOnApplyWindowInsetsListener(
    view: View,
    val mFastScroller: FastScroller
) : View.OnApplyWindowInsetsListener {

    private val mPadding = Rect(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)

    override fun onApplyWindowInsets(view: View, insets: WindowInsets) :WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mInsets = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(mInsets.left, mInsets.top, mInsets.right, mInsets.bottom)
            mFastScroller.setPadding(mInsets.left, 0, mInsets.right, mInsets.bottom)
        } else {
            view.setPadding(
                mPadding.left + insets.getSystemWindowInsetLeft(), mPadding.top,
                mPadding.right + insets.getSystemWindowInsetRight(),
                mPadding.bottom + insets.getSystemWindowInsetBottom()
            )
            mFastScroller.setPadding(
                insets.getSystemWindowInsetLeft(), 0,
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()
            )
        }
        return insets;
    }
}