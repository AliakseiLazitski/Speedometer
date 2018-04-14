package jork.juger.speedometer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Admin on 19.02.2018.
 */

public class Speedometer extends View {
    private static final float FULL_DEGREE = 360.f;
    private static final float FREE_ARC_DEGREE = 90.f;
    private static final float START_ROTATION = FREE_ARC_DEGREE * 1.5f;
    private static final float ARC_DRAWING_DEGREE = FULL_DEGREE - FREE_ARC_DEGREE;
    private static final float DEFAULT_MIN_SIZE = 80.F;
    private static final int[] DEFAULT_PROGRESS_GRADIENT_COLORS = new int[]{Color.BLUE, Color.WHITE, Color.GREEN, Color.YELLOW, Color.RED, Color.TRANSPARENT};

    private float mArcLargeRadius;
    private float mArcCenterRadius;
    private float mArcSmallRadius;
    private float mCenterCoord;

    private float mMaxValue = 100.f;
    private float mCurrentValue = 0.f;
    private float mCurrentValueInDegree = 0.f;
    private float mCountOfDrawingValues = 2.f;

    private boolean mIsAnimated = false;
    private long mAnimationDurationInMillis = 100L;
    private float mAnimationValue = 0.f;
    private float mOldValueInDegree = 0.f;
    private float dValueInDegree = 0.f;
    private float mRangeValuesAngle;

    private int[] mGradientColors;
    private float[] mGradientColorsPosition;

    private String mTitleText = "MEGABITS";
    private String mSubtitleText = "Ping";
    private String mInfoText = "0 ms";

    private Path mArrowPath;

    private RectF mColoredBlockRect;
    private RectF mColoredArcRect;

    private Paint mColoredBlockPaint;
    private Paint mColoredLinePaint;
    private Paint mUnactiveLinePaint;
    private Paint mArcLinesPaint;
    private Paint mArrowPaint;
    private Paint mTitleTextPaint;
    private Paint mSubtitlePaint;
    private Paint mInfoTextPaint;
    private Paint mValuesTextPaint;

    private ShapeDrawable.ShaderFactory mGradientShaderFactory = new ShapeDrawable.ShaderFactory() {
        @Override
        public Shader resize(int width, int height) {
            return new SweepGradient(width / 2.f, height / 2.f, mGradientColors, mGradientColorsPosition);
        }
    };

    private ValueAnimator mValueAnimator;

    public Speedometer(Context context) {
        super(context);
        init();
    }

    public Speedometer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        extractAttrs(attrs, 0);
    }

    public Speedometer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        extractAttrs(attrs, defStyleAttr);
    }

    private void extractAttrs(AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = getResources().obtainAttributes(attrs, R.styleable.Speedometer);
        try {
            mCountOfDrawingValues = (float) typedArray.getInt(R.styleable.Speedometer_countOfDrawingValues, 2);
            mMaxValue = typedArray.getFloat(R.styleable.Speedometer_maxValue, 100.f);
            int resourceId = typedArray.getResourceId(R.styleable.Speedometer_progressGradientColorArray, 0);
            setGradientColors(resourceId == 0 ? DEFAULT_PROGRESS_GRADIENT_COLORS : getResources().getIntArray(resourceId));
            setValue(typedArray.getFloat(R.styleable.Speedometer_value, 0.f));
            calculateRangeAngle();
        } finally {
            if (typedArray != null)
                typedArray.recycle();
        }
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        initSizes();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST)
            width = height = (int) (DEFAULT_MIN_SIZE * getResources().getDisplayMetrics().density);
        else if ((MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST || MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST)) {
            int minSize = Math.min(width, height);
            switch (MeasureSpec.getMode(widthMeasureSpec)) {
                case MeasureSpec.AT_MOST:
                    width = minSize;
                    break;
            }
            switch (MeasureSpec.getMode(heightMeasureSpec)) {
                case MeasureSpec.AT_MOST:
                    height = minSize;
                    break;
            }
        }
        setMeasuredDimension(width, height);
    }

    public void setTitleText(String titleText) {
        mTitleText = titleText;
        postInvalidate();
    }

    public void setSubtitleText(String subtitleText) {
        mSubtitleText = subtitleText;
        postInvalidate();
    }

    public void setInfoText(String infoText) {
        mInfoText = infoText;
        postInvalidate();
    }

    public void setValuesTextColor(int color) {
        mValuesTextPaint.setColor(color);
    }

    private void setMaxValue(float mMaxValue) {
        calculateRangeAngle();
    }

    public void setTitleTextColor(int color) {
        mTitleTextPaint.setColor(color);
        postInvalidate();
    }

    public void setSubitleTextColor(int color) {
        mSubtitlePaint.setColor(color);
        postInvalidate();
    }

    public void setInfoTextColor(int color) {
        mInfoTextPaint.setColor(color);
        postInvalidate();
    }

    public void setUnactiveColor(int color) {
        mUnactiveLinePaint.setColor(color);
        postInvalidate();
    }

    public void setSeparatorLineColor(int color) {
        mArcLinesPaint.setColor(color);
        postInvalidate();
    }

    public void setArrowColor(int color) {
        mArrowPaint.setColor(color);
        postInvalidate();
    }

    public void setGradientColors(int[] colors) {
        mGradientColors = new int[colors.length + 1];
        System.arraycopy(colors, 0, mGradientColors, 0, colors.length);
        mGradientColors[mGradientColors.length - 1] = Color.TRANSPARENT;
        float delta = (ARC_DRAWING_DEGREE / FULL_DEGREE) / (mGradientColors.length - 2);
        mGradientColorsPosition = new float[mGradientColors.length];
        for (int i = 0; i < mGradientColors.length - 1; i++) {
            mGradientColorsPosition[i] = delta * i;
        }
        mGradientColorsPosition[mGradientColors.length - 1] = 1.f;

        changeShaderSize();
        postInvalidate();
    }

    private void changeShaderSize() {
        mColoredBlockPaint.setShader(mGradientShaderFactory.resize(getMeasuredWidth(), getMeasuredHeight()));
        mColoredLinePaint.setShader(mGradientShaderFactory.resize(getMeasuredWidth(), getMeasuredHeight()));
    }

    private void init() {
        mArrowPath = new Path();

        mColoredBlockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColoredLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArcLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUnactiveLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTitleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSubtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInfoTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValuesTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTitleTextPaint.setTextAlign(Paint.Align.CENTER);
        mSubtitlePaint.setTextAlign(Paint.Align.CENTER);
        mInfoTextPaint.setTextAlign(Paint.Align.CENTER);
        mValuesTextPaint.setTextAlign(Paint.Align.CENTER);

        mColoredBlockPaint.setStyle(Paint.Style.STROKE);
        mColoredLinePaint.setStyle(Paint.Style.STROKE);
        mArcLinesPaint.setStyle(Paint.Style.STROKE);
        mUnactiveLinePaint.setStyle(Paint.Style.STROKE);
        mArrowPaint.setStyle(Paint.Style.FILL);

        mArcLinesPaint.setColor(Color.WHITE);
        mUnactiveLinePaint.setColor(Color.BLACK);
        mArrowPaint.setColor(mUnactiveLinePaint.getColor());
        mTitleTextPaint.setColor(Color.WHITE);
        mSubtitlePaint.setColor(Color.WHITE);
        mInfoTextPaint.setColor(Color.WHITE);
        mValuesTextPaint.setColor(Color.WHITE);

        mValueAnimator = new ValueAnimator();
        mValueAnimator.setDuration(mAnimationDurationInMillis);
        mValueAnimator.setFloatValues(1.f);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mIsAnimated = true;
                mAnimationValue = (float) animation.getAnimatedValue();
                if (animation.getDuration() <= animation.getCurrentPlayTime()) {
                    mAnimationValue = 0.f;
                    mIsAnimated = false;
                    mOldValueInDegree = 0.f;
                    dValueInDegree = 0.f;
                }
                postInvalidate();
            }
        });
        setCurrentValueInDegree();
    }

    private void initSizes() {
        mCenterCoord = Math.max(getMeasuredWidth(), getMeasuredHeight()) / 2.f;

        mArcLargeRadius = mCenterCoord;
        mArcCenterRadius = mCenterCoord * .9f;
        mArcSmallRadius = mArcCenterRadius - (mArcLargeRadius - mArcCenterRadius) / 2.f;

        float strokeWidth = getMeasuredWidth() * .0035f;
        mColoredLinePaint.setStrokeWidth(strokeWidth);
        mUnactiveLinePaint.setStrokeWidth(strokeWidth);
        mArcLinesPaint.setStrokeWidth(strokeWidth);
        strokeWidth = mArcCenterRadius - mArcSmallRadius;
        mColoredBlockPaint.setStrokeWidth(strokeWidth);
        mTitleTextPaint.setTextSize(strokeWidth * 2.f);
        mValuesTextPaint.setTextSize(strokeWidth * 1.5f);
        mSubtitlePaint.setTextSize(strokeWidth * 2.5f);
        mInfoTextPaint.setTextSize(strokeWidth * 2.f);

        float padding = mCenterCoord - mArcCenterRadius + strokeWidth / 2.f;
        mColoredBlockRect = new RectF(padding, padding, getMeasuredWidth() - padding, getMeasuredHeight() - padding);

        padding = mCenterCoord / 3.f;
        mColoredArcRect = new RectF(padding, padding, getMeasuredWidth() - padding, getMeasuredHeight() - padding);

        float arrowLargeRadius = strokeWidth * .75f;
        float arrowSmalRadius = arrowLargeRadius / 3.f;
        float arrowLength = mArcSmallRadius * .9f - arrowSmalRadius;

        float topLeftPadding = mCenterCoord - arrowLargeRadius;
        float rightBottomPadding = mCenterCoord + arrowLargeRadius;

        RectF arrowRectF = new RectF(topLeftPadding, topLeftPadding, rightBottomPadding, rightBottomPadding);
        mArrowPath.reset();
        mArrowPath.moveTo(mCenterCoord, topLeftPadding);
        mArrowPath.arcTo(arrowRectF, -90.f, 180.f);
        mArrowPath.lineTo(mCenterCoord - arrowLength, mCenterCoord + arrowSmalRadius);

        arrowRectF = new RectF(mCenterCoord - (arrowLength + arrowSmalRadius), mCenterCoord - arrowSmalRadius,
                mCenterCoord - (arrowLength - arrowSmalRadius), mCenterCoord + arrowSmalRadius);
        mArrowPath.arcTo(arrowRectF, 90.f, 180.f);
        mArrowPath.lineTo(mCenterCoord, mCenterCoord - arrowLargeRadius);

//        setGradientColors(new int[]{Color.BLUE, Color.WHITE, Color.GREEN, Color.YELLOW, Color.RED, Color.TRANSPARENT});
        calculateRangeAngle();
        changeShaderSize();
    }

    private void calculateRangeAngle() {
        mRangeValuesAngle = ARC_DRAWING_DEGREE / mCountOfDrawingValues;
    }

    private void setCurrentValueInDegree() {
        mCurrentValueInDegree = (mCurrentValue / mMaxValue) * ARC_DRAWING_DEGREE;
    }

    public void setValue(float value) {
        if (mMaxValue < mCurrentValue) {
            mCurrentValue = mMaxValue;
        } else if (mCurrentValue < 0.f) {
            mCurrentValue = 0.f;
        } else {
            mCurrentValue = value;
        }
        setCurrentValueInDegree();
        postInvalidate();
    }

    public void setValueWithAnimation(float value) {
        if (!mIsAnimated) {
            mIsAnimated = true;
            mOldValueInDegree = mCurrentValueInDegree;
            setValue(value);
            dValueInDegree = mCurrentValueInDegree - mOldValueInDegree;
            mValueAnimator.start();
        } else {
            mOldValueInDegree = mCurrentValueInDegree;
            setValue(value);
            dValueInDegree = mCurrentValueInDegree - mOldValueInDegree;
        }
    }

    private float getXPoint(float angle, float radius) {
        return mCenterCoord - (float) (Math.cos(Math.toRadians(angle)) * radius);
    }

    private float getYPoint(float angle, float radius) {
        return mCenterCoord - (float) (Math.sin(Math.toRadians(angle)) * radius);
    }

    private float getValueFromAngle(float angle) {
        return (angle / ARC_DRAWING_DEGREE) * mMaxValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTitleText != null && !mTitleText.isEmpty()) {
            float y = mCenterCoord - mColoredArcRect.height() / 4.f;
            canvas.drawText(mTitleText, mCenterCoord, y, mTitleTextPaint);
        }

        if (mSubtitleText != null && !mSubtitleText.isEmpty()) {
            float y = getYPoint((-FREE_ARC_DEGREE / 2.f), mColoredArcRect.height() / 2.f) + mSubtitlePaint.getTextSize() / 2.f;
            canvas.drawText(mSubtitleText, mCenterCoord, y, mSubtitlePaint);
        }

        if (mInfoText != null && !mInfoText.isEmpty()) {
            float y = getYPoint((-FREE_ARC_DEGREE / 2.f), mArcSmallRadius) + mInfoTextPaint.getTextSize() / 2.f;
            canvas.drawText(mInfoText, mCenterCoord, y, mInfoTextPaint);
        }

        float valueDegree;
        if (mIsAnimated) {
            valueDegree = mOldValueInDegree + dValueInDegree * mAnimationValue;
        } else {
            valueDegree = mCurrentValueInDegree;
        }

        int barCountInRange = 5;
        float dAngle = mRangeValuesAngle / (float) barCountInRange;
        float x;
        float y;
        String text;
        int value;
        float paddingDegree = -FREE_ARC_DEGREE / 2.f;
        float valuesRadius = mArcSmallRadius * .9f;

        float angle = 0.f;
        for (int i = 0; angle <= ARC_DRAWING_DEGREE; i++) {
            value = Math.round(getValueFromAngle(angle));
            if (angle % mRangeValuesAngle == 0) {
                text = String.valueOf(value);
                x = getXPoint(angle + paddingDegree, valuesRadius);
                y = getYPoint(angle + paddingDegree, valuesRadius);
                canvas.drawText(text, x, y + mValuesTextPaint.getTextSize() / 2.f, mValuesTextPaint);
            }
            angle = (i + 1) * dAngle;
        }

        canvas.save();
        canvas.rotate(-FREE_ARC_DEGREE / 2.f, mCenterCoord, mCenterCoord);
        angle = 0.f;
        for (int i = 0; angle <= ARC_DRAWING_DEGREE; i++) {
            if (angle % mRangeValuesAngle == 0) {
                canvas.drawLine(getXPoint(angle, mArcLargeRadius), getYPoint(angle, mArcLargeRadius), getXPoint(angle, mArcSmallRadius), getYPoint(angle, mArcSmallRadius), mArcLinesPaint);
            } else {
                canvas.drawLine(getXPoint(angle, mArcCenterRadius), getYPoint(angle, mArcCenterRadius), getXPoint(angle, mArcSmallRadius), getYPoint(angle, mArcSmallRadius), mArcLinesPaint);
            }
            angle = (i + 1) * dAngle;
        }
        canvas.rotate(valueDegree, mCenterCoord, mCenterCoord);
        canvas.drawPath(mArrowPath, mArrowPaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(START_ROTATION, mCenterCoord, mCenterCoord);
        paddingDegree = dAngle / 4.f;
        for (float i = paddingDegree; i < valueDegree; i += dAngle) {
            float sweepAngle = dAngle - paddingDegree * 2.f;
            if (i + sweepAngle > valueDegree) {
                sweepAngle -= ((i + sweepAngle) - valueDegree);
            }
            if (sweepAngle > 0)
                canvas.drawArc(mColoredBlockRect, i, sweepAngle, false, mColoredBlockPaint);
        }
        canvas.drawArc(mColoredArcRect, valueDegree, ARC_DRAWING_DEGREE - valueDegree, false, mUnactiveLinePaint);
        canvas.drawArc(mColoredArcRect, 0.f, valueDegree, false, mColoredLinePaint);
        canvas.restore();
    }
}
