package jork.juger.speedometer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.ShapeDrawable;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

/**
 * Created by Admin on 19.02.2018.
 */

public class SpeedometerWithRangedValues extends View {
    private static final float FULL_DEGREE = 360.f;
    private static final float FREE_ARC_DEGREE = 90.f;
    private static final float START_ROTATION = FREE_ARC_DEGREE * 1.5f;
    private static final float ARC_DRAWING_DEGREE = FULL_DEGREE - FREE_ARC_DEGREE;
    private static final float DEFAULT_MIN_SIZE = 80.F;
    private static final int[] DEFAULT_PROGRESS_GRADIENT_COLORS = new int[]{Color.BLUE, Color.WHITE, Color.GREEN, Color.YELLOW, Color.RED, Color.TRANSPARENT};
    private static final float[] DEFAULT_DRAWING_VALUE_ARRAY = new float[]{0.f, 1.f, 5.f, 10.f, 20.f, 30.f, 50.f, 75.f, 100.f};

    private float mArcLargeRadius;
    private float mArcCenterRadius;
    private float mArcSmallRadius;
    private float mCenterCoord;
    private float mBitmapSize;

    private float mCurrentValue = 0.f;
    private float mCurrentValueInDegree = 0.f;

    private boolean mIsAnimated = false;
    private boolean mWithIllumination = false;
    private float mAnimationValue = 0.f;
    private float mOldValueInDegree = 0.f;
    private float dValueInDegree = 0.f;
    private float[] mDrawingValuesArray = DEFAULT_DRAWING_VALUE_ARRAY;
    private float[] mRangeValuesArray = Arrays.copyOfRange(mDrawingValuesArray, 1, mDrawingValuesArray.length);
    private float mRangeValuesAngle;

    private int[] mGradientColors = DEFAULT_PROGRESS_GRADIENT_COLORS;
    private float[] mGradientColorsPosition;

    private Point mTitleTextPoint;
    private Point mSubtitleTextPoint = new Point();
    private Point mInfoTextPoint;

    private String mTitleText = "MEGABITS";
    private String mSubtitleText = "Ping";
    private String mInfoText = "0 ms";

    private Path mArrowPath;
    private Path mArcPath;

    private RectF mColoredBlockRect;
    private RectF mColoredArcRect;
    private RectF mBitmapRect;

    private Bitmap mDrawingBitmap = null;

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

    public SpeedometerWithRangedValues(Context context) {
        super(context);
        init();
    }

    public SpeedometerWithRangedValues(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        extractAttrs(attrs, 0);
    }

    public SpeedometerWithRangedValues(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        extractAttrs(attrs, defStyleAttr);
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

    public void setRanges(int[] ranges) {
        float[] floatRanges = new float[ranges.length];
        for (int i = 0; i < floatRanges.length; i++) {
            floatRanges[i] = ranges[i];
        }
        setRanges(floatRanges);
    }

    public void setRanges(float[] ranges) {
        mDrawingValuesArray = ranges;
        mRangeValuesArray = Arrays.copyOfRange(mDrawingValuesArray, 1, mDrawingValuesArray.length);
        initSpeedometerArc();
        setCurrentValueInDegree();
        postInvalidate();
    }

    /**
     * Метод для установки текста в верхней части
     */
    public void setTitleText(String titleText) {
        mTitleText = titleText;
        postInvalidate();
    }

    /**
     * Метод для установки текста в средней части
     */
    public void setSubtitleText(String subtitleText) {
        mSubtitleText = subtitleText;
        postInvalidate();
    }

    /**
     * Метод для установки текста в нижней части
     */
    public void setInfoText(String infoText) {
        mInfoText = infoText;
        postInvalidate();
    }

    /**
     * Метод для установки цвета текста значений (у окружности)
     */
    public void setValuesTextColor(int color) {
        mValuesTextPaint.setColor(color);
    }

    /**
     * Метод для установки цвета верхнего текста
     */
    public void setTitleTextColor(int color) {
        mTitleTextPaint.setColor(color);
        postInvalidate();
    }

    /**
     * Метод для установки цвета среднего текста
     */
    public void setSubitleTextColor(int color) {
        mSubtitlePaint.setColor(color);
        postInvalidate();
    }

    /**
     * Метод для установки цвета нижнего текста
     */
    public void setInfoTextColor(int color) {
        mInfoTextPaint.setColor(color);
        postInvalidate();
    }

    /**
     * Метод для установки цвета незаполненной дуги
     */
    public void setUnactiveColor(int color) {
        mUnactiveLinePaint.setColor(color);
        postInvalidate();
    }

    /**
     * Метод для установки цвета разделительных линий
     */
    public void setSeparatorLineColor(int color) {
        mArcLinesPaint.setColor(color);
        postInvalidate();
    }

    /**
     * Метод для установки цвета стрелки
     */
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

    /**
     * Метод для установки показа подсветки (true - если нужно подсвечивать, инчае false)
     */
    public void setIllumination(boolean withIllumination) {
        mWithIllumination = withIllumination;
        if (mWithIllumination) {
            mColoredBlockPaint.setMaskFilter(new BlurMaskFilter(4.5f, BlurMaskFilter.Blur.SOLID));
            mColoredLinePaint.setMaskFilter(new BlurMaskFilter(4.5f, BlurMaskFilter.Blur.SOLID));
        } else {
            mColoredBlockPaint.setMaskFilter(null);
            mColoredLinePaint.setMaskFilter(null);
        }
        postInvalidate();
    }

    private void extractAttrs(AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = getResources().obtainAttributes(attrs, R.styleable.SpeedometerWithRangedValues);
        try {
            int resId = typedArray.getResourceId(R.styleable.SpeedometerWithRangedValues_rangeArray, 0);
            if (resId != 0)
                setRanges(getResources().getIntArray(resId));
            setValue(typedArray.getFloat(R.styleable.SpeedometerWithRangedValues_progress, 0.f));
            int resourceId = typedArray.getResourceId(R.styleable.SpeedometerWithRangedValues_gradientColorArray, 0);
            setGradientColors(resourceId == 0 ? DEFAULT_PROGRESS_GRADIENT_COLORS : getResources().getIntArray(resourceId));
            resId = typedArray.getResourceId(R.styleable.SpeedometerWithRangedValues_drawable, 0);
            setDrawable(resId);
        } finally {
            if (typedArray != null)
                typedArray.recycle();
        }
        postInvalidate();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mArrowPath = new Path();
        mArcPath = new Path();

        mColoredBlockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColoredLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArcLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mUnactiveLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTitleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSubtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInfoTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mValuesTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setIllumination(mWithIllumination);

        mColoredLinePaint.setStrokeCap(Paint.Cap.ROUND);

        mTitleTextPaint.setTextAlign(Paint.Align.CENTER);
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
        long animationDurationInMillis = 100L;
        mValueAnimator.setDuration(animationDurationInMillis);
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

        float strokeWidth = mCenterCoord * .007f;
        mColoredLinePaint.setStrokeWidth(strokeWidth);
        mUnactiveLinePaint.setStrokeWidth(strokeWidth);
        mArcLinesPaint.setStrokeWidth(strokeWidth);
        strokeWidth = mArcCenterRadius - mArcSmallRadius;
        mColoredBlockPaint.setStrokeWidth(strokeWidth);
        mTitleTextPaint.setTextSize(strokeWidth * 2.f);
        mValuesTextPaint.setTextSize(strokeWidth * 1.5f);
        mBitmapSize = strokeWidth * 2.5f;
        mSubtitlePaint.setTextSize(strokeWidth * 2.5f);
        mBitmapSize *= .75f; //Для нормализации (учет отступов текста)
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

        mSubtitleTextPoint = new Point((int) mCenterCoord, (int) (getYPoint((-FREE_ARC_DEGREE / 2.f), mArcSmallRadius)));
        mInfoTextPoint = new Point((int) mCenterCoord, (int) (mSubtitleTextPoint.y + mSubtitlePaint.getTextSize() * 1.25f));
        mTitleTextPoint = new Point((int) mCenterCoord, (int) (mCenterCoord - mArcSmallRadius * .5f + mTitleTextPaint.getTextSize()));

        calculateBitmapPadding();

        initSpeedometerArc();
        changeShaderSize();
    }

    private void calculateBitmapPadding() {
        if (mDrawingBitmap != null) {
            float xPadding = mSubtitlePaint.measureText("  ");
            mBitmapRect = new RectF(mSubtitleTextPoint.x - (xPadding + mBitmapSize), mSubtitleTextPoint.y - mBitmapSize,
                    mSubtitleTextPoint.x - xPadding, mSubtitleTextPoint.y);
            mSubtitleTextPoint.x = (int) (mBitmapRect.right + xPadding / 2.f);
            if (mBitmapSize > 0)
                mDrawingBitmap = Bitmap.createScaledBitmap(mDrawingBitmap, (int) mBitmapSize, (int) mBitmapSize, true);
        } else mSubtitleTextPoint.x = (int) mCenterCoord;

    }

    protected void setDrawable(@DrawableRes int drawableId) {
        if (drawableId != 0) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableId);
            if (bmp != null)
                setDrawingBitmap(bmp.copy(bmp.getConfig(), true));
        } else setDrawingBitmap(null);
    }

    /**
     * Метод для установки изображения для отрисовываего изображения
     */
    public void setDrawingBitmap(Bitmap bitmap) {
        mDrawingBitmap = bitmap;
        if (mDrawingBitmap != null)
            calculateBitmapPadding();
        postInvalidate();
    }

    private void initSpeedometerArc() {
        mRangeValuesAngle = ARC_DRAWING_DEGREE / mRangeValuesArray.length;
        mArcPath.reset();
        int barCountInRange = 5;
        float angle;
        float paddingDegree = -FREE_ARC_DEGREE / 2.f;
        float deltaAngle = mRangeValuesAngle / (float) barCountInRange;
        for (int i = 0; i < mDrawingValuesArray.length; i++) {
            angle = i * mRangeValuesAngle + paddingDegree;
            mArcPath.moveTo(getXPoint(angle, mArcLargeRadius), getYPoint(angle, mArcLargeRadius));
            mArcPath.lineTo(getXPoint(angle, mArcSmallRadius), getYPoint(angle, mArcSmallRadius));
            if (mDrawingValuesArray.length - 1 > i) {
                for (int j = 1; j < barCountInRange; j++) {
                    angle += deltaAngle;
                    mArcPath.moveTo(getXPoint(angle, mArcCenterRadius), getYPoint(angle, mArcCenterRadius));
                    mArcPath.lineTo(getXPoint(angle, mArcSmallRadius), getYPoint(angle, mArcSmallRadius));
                }
            }
        }

    }

    private void setCurrentValueInDegree() {
        boolean isValid = true;
        float angle;
        for (int i = 1; i < mDrawingValuesArray.length && isValid; i++) {
            angle = (i - 1) * mRangeValuesAngle;
            if (mCurrentValue <= mDrawingValuesArray[i]) {
                float dRange = mDrawingValuesArray[i] - mDrawingValuesArray[i - 1];
                float valueInRange = mCurrentValue - mDrawingValuesArray[i - 1];
                mCurrentValueInDegree = valueInRange / dRange;
                mCurrentValueInDegree *= mRangeValuesAngle;
                mCurrentValueInDegree += angle;
                isValid = false;
            }
        }
    }

    /**
     * Метод для установки тек. значения
     */
    public void setValue(float value) {
        if (mRangeValuesArray[mRangeValuesArray.length - 1] < mCurrentValue) {
            mCurrentValue = mRangeValuesArray[mRangeValuesArray.length - 1];
        } else if (mCurrentValue < 0.f) {
            mCurrentValue = 0.f;
        } else {
            mCurrentValue = value;
        }
        setCurrentValueInDegree();
        invalidate();
    }

    /**
     * Метод для установки тек. значения и запуска анимации на изменение
     */
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
        return (angle / ARC_DRAWING_DEGREE) * 100.f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTitleText != null && !mTitleText.isEmpty()) {
            canvas.drawText(mTitleText, mTitleTextPoint.x, mTitleTextPoint.y, mTitleTextPaint);
        }

        if (mDrawingBitmap != null) {
            canvas.drawBitmap(mDrawingBitmap, null, mBitmapRect, null);
        } else {
            mSubtitleTextPoint.x = (int) mCenterCoord;
            mSubtitlePaint.setTextAlign(Paint.Align.CENTER);
        }

        if (mSubtitleText != null && !mSubtitleText.isEmpty()) {
            canvas.drawText(mSubtitleText, mSubtitleTextPoint.x, mSubtitleTextPoint.y, mSubtitlePaint);
        }

        if (mInfoText != null && !mInfoText.isEmpty()) {
            canvas.drawText(mInfoText, mInfoTextPoint.x, mInfoTextPoint.y, mInfoTextPaint);
        }

        float valueDegree;
        if (mIsAnimated) {
            valueDegree = mOldValueInDegree + dValueInDegree * mAnimationValue;
        } else {
            valueDegree = mCurrentValueInDegree;
        }
        int barCountInRange = 5;
        float deltaAngle = mRangeValuesAngle / (float) barCountInRange;
        float angle;
        float paddingDegree = -FREE_ARC_DEGREE / 2.f;
        float valuesRadius = mArcSmallRadius * .9f;
        float sweepAngle = deltaAngle / 2.f;
        float paddingAngle = sweepAngle / 2.f;
        canvas.drawPath(mArcPath, mArcLinesPaint);
        for (int i = 0; i < mDrawingValuesArray.length; i++) {
            angle = i * mRangeValuesAngle + paddingDegree;
            String text = String.valueOf((int) mDrawingValuesArray[i]);
            canvas.drawText(text, getXPoint(angle, valuesRadius), getYPoint(angle, valuesRadius) + mValuesTextPaint.getTextSize() / 2.f, mValuesTextPaint);
        }

        canvas.save();
        canvas.rotate(START_ROTATION, mCenterCoord, mCenterCoord);
        for (int i = 0; i < mDrawingValuesArray.length; i++) {
            for (int j = 0; j < barCountInRange; j++) {
                angle = i * mRangeValuesAngle + j * deltaAngle;
                if ((angle + paddingAngle + sweepAngle) < valueDegree) {
                    canvas.drawArc(mColoredBlockRect, angle + paddingAngle, sweepAngle, false, mColoredBlockPaint);
                } else if ((angle + paddingAngle + sweepAngle) >= valueDegree && (angle + paddingAngle) < valueDegree) {
                    float temp = valueDegree - (angle + paddingAngle);
                    canvas.drawArc(mColoredBlockRect, angle + paddingAngle, temp, false, mColoredBlockPaint);
                }
            }
        }
        canvas.restore();

        canvas.save();
        canvas.rotate(START_ROTATION, mCenterCoord, mCenterCoord);
        canvas.drawArc(mColoredArcRect, valueDegree, ARC_DRAWING_DEGREE - valueDegree, false, mUnactiveLinePaint);
        canvas.drawArc(mColoredArcRect, 0.f, valueDegree, false, mColoredLinePaint);
        canvas.restore();
        canvas.save();
        canvas.rotate(-FREE_ARC_DEGREE / 2.f + valueDegree, mCenterCoord, mCenterCoord);
        canvas.drawPath(mArrowPath, mArrowPaint);
        canvas.restore();
    }
}
