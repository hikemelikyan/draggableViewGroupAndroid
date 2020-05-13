package com.armboldmind.draggableviewgroup

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.app.FrameMetricsAggregator
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.atan2

private const val OPEN_START = 0
private const val OPEN_END = 1

class DraggableViewGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var viewDragHelper: ViewDragHelper
    private var initialWidth = 0
    private var startX: Float? = null
    private var startY: Float? = null
    private var draggableTargetMap = HashMap<Int, Int>()
    var dragging = false
    private var openingPlace = OPEN_END

    init {

        val a = context.obtainStyledAttributes(attrs, R.styleable.DraggableViewGroup)
        openingPlace = a.getInteger(R.styleable.DraggableViewGroup_openPlace, OPEN_END)
        a.recycle()

        val callback = object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                if (child.visibility == View.VISIBLE && child.tag != null && child.tag == "draggable") {
                    if (!draggableTargetMap.containsKey(child.id)) {
                        val targetView =
                            findViewById<View>((child.layoutParams as LayoutParams).targetId)
                        if (targetView != null)
                            draggableTargetMap[child.id] =
                                if (openingPlace == OPEN_START) targetView.right else targetView.left
                    }
                    initialWidth = child.width
                    return true
                }
                return false
            }

            override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
                dragging = true
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                dragging = false
                val rightLeftAnimator: ValueAnimator
                if (openingPlace == OPEN_END) {
                    rightLeftAnimator =
                        if (releasedChild.right < draggableTargetMap[releasedChild.id] ?: 0) {
                            ValueAnimator.ofInt(
                                releasedChild.right,
                                draggableTargetMap[releasedChild.id] ?: 0
                            )
                        } else {
                            ValueAnimator.ofInt(releasedChild.right, initialWidth + paddingStart)
                        }
                    rightLeftAnimator.addUpdateListener {
                        releasedChild.right = it.animatedValue as Int
                        releasedChild.left = releasedChild.right - initialWidth
                    }
                } else {
                    rightLeftAnimator =
                        if (releasedChild.left > draggableTargetMap[releasedChild.id] ?: 0) {
                            ValueAnimator.ofInt(
                                releasedChild.left,
                                draggableTargetMap[releasedChild.id] ?: 0
                            )
                        } else {
                            ValueAnimator.ofInt(releasedChild.left, paddingStart)
                        }
                    rightLeftAnimator.addUpdateListener {
                        releasedChild.left = it.animatedValue as Int
                        releasedChild.right = releasedChild.left + initialWidth
                    }
                }

                rightLeftAnimator.duration = FrameMetricsAggregator.ANIMATION_DURATION.toLong()
                rightLeftAnimator.start()
            }

            override fun onViewPositionChanged(
                changedView: View,
                left: Int,
                top: Int,
                dx: Int,
                dy: Int
            ) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
                if (openingPlace == OPEN_END) {
                    if (left > paddingStart) {
                        changedView.left = paddingStart
                        changedView.right = paddingStart + initialWidth
                    }
                } else {
                    if (left < paddingStart) {
                        changedView.left = paddingStart
                        changedView.right = paddingStart + initialWidth
                    }
                }
            }

            override fun getViewVerticalDragRange(child: View): Int {
                return 0
            }

            override fun getViewHorizontalDragRange(child: View): Int {
                return child.width
            }

            override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
                return left
            }

            override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
                return top - dy
            }
        }
        viewDragHelper = ViewDragHelper.create(this, callback)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return viewDragHelper.shouldInterceptTouchEvent(ev!!)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (startX == null)
                    startX = event.x
                if (startY == null)
                    startY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (atan2(
                        abs(event.y - startY!!).toDouble(),
                        abs(event.x - startX!!).toDouble()
                    ) > Math.toRadians(45.0) && viewDragHelper.viewDragState != ViewDragHelper.STATE_DRAGGING
                ) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                } else {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    viewDragHelper.processTouchEvent(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                startX = null
                startY = null
                viewDragHelper.processTouchEvent(event)
            }
            MotionEvent.ACTION_CANCEL -> {
                startX = null
                startY = null
                viewDragHelper.processTouchEvent(event)
            }
        }
        Log.d("EVENT", event?.action.toString())
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        for (i in 0 until childCount) {
            getChildAt(i).layoutParams as LayoutParams
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): RelativeLayout.LayoutParams? {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(context, null)
    }

    inner class LayoutParams(c: Context?, attrs: AttributeSet?) :
        RelativeLayout.LayoutParams(c, attrs) {

        var targetId: Int = 0

        init {
            if (c != null && attrs != null) {
                val a = c.obtainStyledAttributes(attrs, R.styleable.DraggableViewGroup_Layout)
                targetId = a.getResourceId(R.styleable.DraggableViewGroup_Layout_target_id, 0)
                a.recycle()
            }
        }
    }
}