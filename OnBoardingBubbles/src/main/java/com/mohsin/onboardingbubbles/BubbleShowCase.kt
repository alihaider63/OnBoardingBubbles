package com.mohsin.onboardingbubbles

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.mohsin.onboardingbubbles.ScreenUtils.dpToPx
import java.lang.ref.WeakReference

class BubbleShowCase(builder: BubbleShowCaseBuilder) {

    companion object {
        private const val SHARED_PREFS_NAME = "BubbleShowCasePrefs"
        private const val FOREGROUND_LAYOUT_ID = 731
        private const val DURATION_SHOW_CASE_ANIMATION = 200 //ms
        private const val DURATION_BACKGROUND_ANIMATION = 700 //ms
        private const val DURATION_BEATING_ANIMATION = 700 //ms
        private const val MAX_WIDTH_MESSAGE_VIEW_TABLET = 420 //dp
    }

    /**
     * Enum class which corresponds to each valid position for the BubbleMessageView arrow
     */
    enum class ArrowPosition {
        TOP, BOTTOM, LEFT, RIGHT
    }

    /**
     * Highlight mode. It represents the way that the target view will be highlighted
     * - VIEW_LAYOUT: Default value. All the view box is highlighted (the rectangle where the view is contained). Example: For a TextView, all the element is highlighted (characters and background)
     * - VIEW_SURFACE: Only the view surface is highlighted, but not the background. Example: For a TextView, only the characters will be highlighted
     */
    enum class HighlightMode {
        VIEW_LAYOUT, VIEW_SURFACE
    }


    private val mActivity: WeakReference<Activity> = builder.mActivity!!

    //BubbleMessageView params
    private val mImage: Drawable? = builder.mImage
    private val mTitle: String? = builder.mTitle
    private val mSubtitle: String? = builder.mSubtitle
    private val mCloseAction: Drawable? = builder.mCloseAction
    private val mBackgroundColor: Int? = builder.mBackgroundColor
    private val mTextColor: Int? = builder.mTextColor
    private val mTitleTextSize: Int? = builder.mTitleTextSize
    private val mSubtitleTextSize: Int? = builder.mSubtitleTextSize
    private val mShowOnce: String? = builder.mShowOnce
    private val mNextButtonText: String? = builder.mNextButtonText
    private val mLabelButtonText: String? = builder.mLabelButtonText
    private val mDisableTargetClick: Boolean = builder.mDisableTargetClick
    private val mDisableCloseAction: Boolean = builder.mDisableCloseAction
    private val mHighlightMode: HighlightMode? = builder.mHighlightMode
    private val mArrowPositionList: MutableList<ArrowPosition> = builder.mArrowPositionList
    private val mTargetView: WeakReference<View>? = builder.mTargetView
    private val mBubbleShowCaseListener: BubbleShowCaseListener? = builder.mBubbleShowCaseListener

    //Sequence params
    private val mSequenceListener: SequenceShowCaseListener? = builder.mSequenceShowCaseListener
    private val isFirstOfSequence: Boolean = builder.mIsFirstOfSequence!!
    private val isLastOfSequence: Boolean = builder.mIsLastOfSequence!!

    //References
    private var backgroundDimLayout: RelativeLayout? = null
    private var bubbleMessageViewBuilder: BubbleMessageView.Builder? = null

    fun show() {
        if (mShowOnce != null) {
            if (isBubbleShowCaseHasBeenShowedPreviously(mShowOnce)) {
                notifyDismissToSequenceListener()
                return
            } else {
                registerBubbleShowCaseInPreferences(mShowOnce)
            }
        }

        val rootView = getViewRoot(mActivity.get()!!)
        backgroundDimLayout = getBackgroundDimLayout()
        setBackgroundDimListener(backgroundDimLayout)
        bubbleMessageViewBuilder = getBubbleMessageViewBuilder()

        if (mTargetView != null && mArrowPositionList.size <= 1) {
            //Wait until the end of the layout animation, to avoid problems with pending scrolls or view movements
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val target = mTargetView.get()!!
                //If the arrow list is empty, the arrow position is set by default depending on the targetView position on the screen
                if (mArrowPositionList.isEmpty()) {
                    if (ScreenUtils.isViewLocatedAtHalfTopOfTheScreen(
                            mActivity.get()!!,
                            target
                        )
                    ) mArrowPositionList.add(
                        ArrowPosition.TOP
                    ) else mArrowPositionList.add(ArrowPosition.BOTTOM)
                    bubbleMessageViewBuilder = getBubbleMessageViewBuilder()
                }

                if (isVisibleOnScreen(target)) {
                    addTargetViewAtBackgroundDimLayout(target, backgroundDimLayout)
                    addBubbleMessageViewDependingOnTargetView(
                        target,
                        bubbleMessageViewBuilder!!,
                        backgroundDimLayout
                    )
                } else {
                    dismiss()
                }
            }, DURATION_BACKGROUND_ANIMATION.toLong())
        } else {
            addBubbleMessageViewOnScreenCenter(bubbleMessageViewBuilder!!, backgroundDimLayout)
        }
        if (isFirstOfSequence) {
            //Add the background dim layout above the root view
            val animation = AnimationUtils.getFadeInAnimation(0, DURATION_BACKGROUND_ANIMATION)
            backgroundDimLayout?.let {
                val childView = AnimationUtils.setAnimationToView(backgroundDimLayout!!, animation)
                if (childView.parent == null) { //if the parent of childView is null then will add in rootView.
                    rootView.addView(childView)
                }
            }
        }
    }

    fun dismiss() {
        if (backgroundDimLayout != null && isLastOfSequence) {
            //Remove background dim layout if the BubbleShowCase is the last of the sequence
            finishSequence()
        } else {
            //Remove all the views created over the background dim layout waiting for the next BubbleShowCase in the sequence
            backgroundDimLayout?.removeAllViews()
        }
        notifyDismissToSequenceListener()
    }

    private fun finishSequence() {
        val rootView = getViewRoot(mActivity.get()!!)
        rootView.removeView(backgroundDimLayout)
        backgroundDimLayout = null
    }

    private fun notifyDismissToSequenceListener() {
        mSequenceListener?.let { mSequenceListener.onDismiss() }
    }

    private fun getViewRoot(activity: Activity): ViewGroup {
        val androidContent = activity.findViewById<ViewGroup>(android.R.id.content)
        return androidContent.parent.parent as ViewGroup
    }

    private fun getBackgroundDimLayout(): RelativeLayout {
        if (mActivity.get()!!.findViewById<RelativeLayout>(FOREGROUND_LAYOUT_ID) != null)
            return mActivity.get()!!.findViewById(FOREGROUND_LAYOUT_ID)
        val backgroundLayout = RelativeLayout(mActivity.get()!!)
        backgroundLayout.id = FOREGROUND_LAYOUT_ID
        backgroundLayout.layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        backgroundLayout.setBackgroundColor(
            ContextCompat.getColor(
                mActivity.get()!!,
                R.color.transparent_grey
            )
        )
        backgroundLayout.isClickable = true
        backgroundLayout.isFocusable = true
        return backgroundLayout
    }

    private fun setBackgroundDimListener(backgroundDimLayout: RelativeLayout?) {
        backgroundDimLayout?.setOnClickListener { mBubbleShowCaseListener?.onBackgroundDimClick(this) }
    }

    private fun getBubbleMessageViewBuilder(): BubbleMessageView.Builder {
        return BubbleMessageView.Builder()
            .from(mActivity.get()!!)
            .arrowPosition(mArrowPositionList)
            .backgroundColor(mBackgroundColor)
            .textColor(mTextColor)
            .titleTextSize(mTitleTextSize)
            .subtitleTextSize(mSubtitleTextSize)
            .title(mTitle)
            .subtitle(mSubtitle)
            .image(mImage)
            .nextButtonText(mNextButtonText)
            .labelButtonText(mLabelButtonText)
            .closeActionImage(mCloseAction)
            .disableCloseAction(mDisableCloseAction)
            .listener(object : OnBubbleMessageViewListener {
                override fun onBubbleClick() {
                    mBubbleShowCaseListener?.onBubbleClick(this@BubbleShowCase)
                }

                override fun onCloseActionImageClick() {
                    dismiss()
                    mBubbleShowCaseListener?.onCloseActionImageClick(this@BubbleShowCase)
                }

                override fun onNextButtonClick() {
                    dismiss()
                    mBubbleShowCaseListener?.onNextButtonClick()
                }

                override fun onLabelButtonClick() {
                    dismiss()
                    mBubbleShowCaseListener?.onLabelButtonClick()
                }
            })
    }

    private fun isBubbleShowCaseHasBeenShowedPreviously(id: String): Boolean {
        val mPrefs = mActivity.get()!!.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        return getString(mPrefs, id) != null
    }

    private fun registerBubbleShowCaseInPreferences(id: String) {
        val mPrefs = mActivity.get()!!.getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE)
        setString(mPrefs, id, id)
    }

    private fun getString(mPrefs: SharedPreferences, key: String): String? {
        return mPrefs.getString(key, null)
    }

    private fun setString(mPrefs: SharedPreferences, key: String, value: String) {
        val editor = mPrefs.edit()
        editor.putString(key, value)
        editor.apply()
    }


    /**
     * This function takes a screenshot of the targetView, creating an ImageView from it. This new ImageView is also set on the layout passed by param
     */
    private fun addTargetViewAtBackgroundDimLayout(
        targetView: View?,
        backgroundDimLayout: RelativeLayout?
    ) {
        if (targetView == null) return

        val targetScreenshot = takeScreenshot(targetView, mHighlightMode)
        val targetScreenshotView = ImageView(mActivity.get()!!)
        targetScreenshotView.setImageBitmap(targetScreenshot)
        targetScreenshotView.setOnClickListener {
            if (!mDisableTargetClick)
                dismiss()
            mBubbleShowCaseListener?.onTargetClick(this)
        }

        val targetViewParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        targetViewParams.setMargins(
            getXPosition(targetView),
            getYPosition(targetView),
            getScreenWidth(mActivity.get()!!) - (getXPosition(targetView) + targetView.width),
            0
        )
        backgroundDimLayout?.addView(
            AnimationUtils.setBouncingAnimation(
                targetScreenshotView,
                0,
                DURATION_BEATING_ANIMATION
            ), targetViewParams
        )
    }

    /**
     * This function creates the BubbleMessageView depending the position of the target and the desired arrow position. This new view is also set on the layout passed by param
     */
    private fun addBubbleMessageViewDependingOnTargetView(
        targetView: View?,
        bubbleMessageViewBuilder: BubbleMessageView.Builder,
        backgroundDimLayout: RelativeLayout?
    ) {
        if (targetView == null) return
        val showCaseParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )

        when (bubbleMessageViewBuilder.mArrowPosition[0]) {
            ArrowPosition.LEFT -> {
                showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                if (ScreenUtils.isViewLocatedAtHalfTopOfTheScreen(mActivity.get()!!, targetView)) {
                    showCaseParams.setMargins(
                        getXPosition(targetView) + targetView.width,
                        getYPosition(targetView),
                        if (isTablet()) getScreenWidth(mActivity.get()!!) - (getXPosition(targetView) + targetView.width) - getMessageViewWidthOnTablet(
                            getScreenWidth(mActivity.get()!!) - (getXPosition(targetView) + targetView.width)
                        ) else 0,
                        0
                    )
                    showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                } else {
                    showCaseParams.setMargins(
                        getXPosition(targetView) + targetView.width,
                        0,
                        if (isTablet()) getScreenWidth(mActivity.get()!!) - (getXPosition(targetView) + targetView.width) - getMessageViewWidthOnTablet(
                            getScreenWidth(mActivity.get()!!) - (getXPosition(targetView) + targetView.width)
                        ) else 0,
                        getScreenHeight(mActivity.get()!!) - getYPosition(targetView) - targetView.height
                    )
                    showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                }
            }

            ArrowPosition.RIGHT -> {
                showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                if (ScreenUtils.isViewLocatedAtHalfTopOfTheScreen(mActivity.get()!!, targetView)) {
                    showCaseParams.setMargins(
                        if (isTablet()) getXPosition(targetView) - getMessageViewWidthOnTablet(
                            getXPosition(targetView)
                        ) else 0,
                        getYPosition(targetView),
                        getScreenWidth(mActivity.get()!!) - getXPosition(targetView),
                        0
                    )
                    showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                } else {
                    showCaseParams.setMargins(
                        if (isTablet()) getXPosition(targetView) - getMessageViewWidthOnTablet(
                            getXPosition(targetView)
                        ) else 0,
                        0,
                        getScreenWidth(mActivity.get()!!) - getXPosition(targetView),
                        getScreenHeight(mActivity.get()!!) - getYPosition(targetView) - targetView.height
                    )
                    showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                }
            }

            ArrowPosition.TOP -> {
                showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                if (ScreenUtils.isViewLocatedAtHalfLeftOfTheScreen(mActivity.get()!!, targetView)) {
                    showCaseParams.setMargins(
                        if (isTablet()) getXPosition(targetView) else 0,
                        getYPosition(targetView) + targetView.height,
                        if (isTablet()) getScreenWidth(mActivity.get()!!) - getXPosition(targetView) - getMessageViewWidthOnTablet(
                            getScreenWidth(mActivity.get()!!) - getXPosition(targetView)
                        ) else 0,
                        0
                    )
                } else {
                    showCaseParams.setMargins(
                        if (isTablet()) getXPosition(targetView) + targetView.width - getMessageViewWidthOnTablet(
                            getXPosition(targetView)
                        ) else 0,
                        getYPosition(targetView) + targetView.height,
                        if (isTablet()) getScreenWidth(mActivity.get()!!) - getXPosition(targetView) - targetView.width else 0,
                        0
                    )
                }
            }

            ArrowPosition.BOTTOM -> {
                showCaseParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                if (ScreenUtils.isViewLocatedAtHalfLeftOfTheScreen(mActivity.get()!!, targetView)) {
                    showCaseParams.setMargins(
                        if (isTablet()) getXPosition(targetView) else 0,
                        0,
                        if (isTablet()) getScreenWidth(mActivity.get()!!) - getXPosition(targetView) - getMessageViewWidthOnTablet(
                            getScreenWidth(mActivity.get()!!) - getXPosition(targetView)
                        ) else 0,
                        getScreenHeight(mActivity.get()!!) - getYPosition(targetView)
                    )
                } else {
                    showCaseParams.setMargins(
                        if (isTablet()) getXPosition(targetView) + targetView.width - getMessageViewWidthOnTablet(
                            getXPosition(targetView)
                        ) else 0,
                        0,
                        if (isTablet()) getScreenWidth(mActivity.get()!!) - getXPosition(targetView) - targetView.width else 0,
                        getScreenHeight(mActivity.get()!!) - getYPosition(targetView)
                    )
                }
            }
        }

        val bubbleMessageView = bubbleMessageViewBuilder.targetViewScreenLocation(
            RectF(
                getXPosition(targetView).toFloat(),
                getYPosition(targetView).toFloat(),
                getXPosition(targetView).toFloat() + targetView.width,
                getYPosition(targetView).toFloat() + targetView.height
            )
        )
            .build()

        bubbleMessageView.id = createViewId()
        val animation = AnimationUtils.getScaleAnimation(0, DURATION_SHOW_CASE_ANIMATION)
        backgroundDimLayout?.addView(
            AnimationUtils.setAnimationToView(
                bubbleMessageView,
                animation
            ), showCaseParams
        )
    }

    /**
     * This function creates a BubbleMessageView and it is set on the center of the layout passed by param
     */
    private fun addBubbleMessageViewOnScreenCenter(
        bubbleMessageViewBuilder: BubbleMessageView.Builder,
        backgroundDimLayout: RelativeLayout?
    ) {
        val showCaseParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        showCaseParams.addRule(RelativeLayout.CENTER_VERTICAL)
        val bubbleMessageView: BubbleMessageView = bubbleMessageViewBuilder.build()
        bubbleMessageView.id = createViewId()
        if (isTablet()) showCaseParams.setMargins(
            if (isTablet()) getScreenWidth(mActivity.get()!!) / 2 - dpToPx(
                MAX_WIDTH_MESSAGE_VIEW_TABLET
            ) / 2 else 0,
            0,
            if (isTablet()) getScreenWidth(mActivity.get()!!) / 2 - dpToPx(
                MAX_WIDTH_MESSAGE_VIEW_TABLET
            ) / 2 else 0,
            0
        )
        val animation = AnimationUtils.getScaleAnimation(0, DURATION_SHOW_CASE_ANIMATION)
        backgroundDimLayout?.addView(
            AnimationUtils.setAnimationToView(
                bubbleMessageView,
                animation
            ), showCaseParams
        )
    }

    private fun createViewId(): Int {
        return View.generateViewId()
    }

    private fun takeScreenshot(targetView: View, highlightMode: HighlightMode?): Bitmap? {
        if (highlightMode == null || highlightMode == HighlightMode.VIEW_LAYOUT)
            return takeScreenshotOfLayoutView(targetView)
        return takeScreenshotOfSurfaceView(targetView)
    }

    private fun takeScreenshotOfLayoutView(targetView: View): Bitmap? {
        if (targetView.width == 0 || targetView.height == 0) {
            return null
        }

        val rootView = getViewRoot(mActivity.get()!!)
        val currentScreenView = rootView.getChildAt(0)
        currentScreenView.buildDrawingCache()
        val bitmap: Bitmap = Bitmap.createBitmap(
            currentScreenView.drawingCache,
            getXPosition(targetView),
            getYPosition(targetView),
            targetView.width,
            targetView.height
        )
        currentScreenView.isDrawingCacheEnabled = false
        currentScreenView.destroyDrawingCache()
        return bitmap
    }

    private fun takeScreenshotOfSurfaceView(targetView: View): Bitmap? {
        if (targetView.width == 0 || targetView.height == 0) {
            return null
        }

        targetView.isDrawingCacheEnabled = true
        val bitmap: Bitmap = Bitmap.createBitmap(targetView.drawingCache)
        targetView.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun isVisibleOnScreen(targetView: View?): Boolean {
        if (targetView != null) {
            if (getXPosition(targetView) >= 0 && getYPosition(targetView) >= 0) {
                return getXPosition(targetView) != 0 || getYPosition(targetView) != 0
            }
        }
        return false
    }

    private fun getXPosition(targetView: View): Int {
        return ScreenUtils.getAxisXPositionOfViewOnScreen(targetView) - getScreenHorizontalOffset()
    }

    private fun getYPosition(targetView: View): Int {
        return ScreenUtils.getAxisYPositionOfViewOnScreen(targetView) - getScreenVerticalOffset()
    }

    private fun getScreenHeight(context: Context): Int {
        return ScreenUtils.getScreenHeight(context) - getScreenVerticalOffset()
    }

    private fun getScreenWidth(context: Context): Int {
        return ScreenUtils.getScreenWidth(context) - getScreenHorizontalOffset()
    }

    private fun getScreenVerticalOffset(): Int {
        return if (backgroundDimLayout != null) ScreenUtils.getAxisYPositionOfViewOnScreen(
            backgroundDimLayout!!
        ) else 0
    }

    private fun getScreenHorizontalOffset(): Int {
        return if (backgroundDimLayout != null) ScreenUtils.getAxisXPositionOfViewOnScreen(
            backgroundDimLayout!!
        ) else 0
    }

    private fun getMessageViewWidthOnTablet(availableSpace: Int): Int {
        return if (availableSpace > dpToPx(MAX_WIDTH_MESSAGE_VIEW_TABLET)) dpToPx(
            MAX_WIDTH_MESSAGE_VIEW_TABLET
        ) else availableSpace
    }

    private fun isTablet(): Boolean = mActivity.get()!!.resources.getBoolean(R.bool.isTablet)

}