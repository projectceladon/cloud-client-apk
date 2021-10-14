package com.commonlibrary.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.commonlibrary.R;
import com.commonlibrary.utils.DensityUtils;

/**
 * 带有刻度的圆形进度条
 * Created by littlejie on 2017/2/26.
 */

public class DialProgress extends View {

    public static final int DEFAULT_SIZE = 150;
    public static final int DEFAULT_START_ANGLE = 270;
    public static final int DEFAULT_SWEEP_ANGLE = 360;
    public static final int DEFAULT_ANIM_TIME = 1000;
    public static final int DEFAULT_MAX_VALUE = 100;
    public static final int DEFAULT_VALUE = 50;
    public static final int DEFAULT_HINT_SIZE = 15;
    public static final int DEFAULT_UNIT_SIZE = 30;
    public static final int DEFAULT_VALUE_SIZE = 15;
    public static final int DEFAULT_ARC_WIDTH = 15;
    private static final String TAG = DialProgress.class.getSimpleName();

    //圆心坐标
    private Point mCenterPoint;
    private float mRadius;
    private float mTextOffsetPercentInRadius;

    private boolean antiAlias;
    //绘制提示
    private TextPaint mHintPaint;
    private CharSequence mHint;
    private int mHintColor;
    private float mHintSize;
    private float mHintOffset;

    //绘制数值
    private Paint mValuePaint;
    private int mValueColor;
    private float mMaxValue;
    private float mValue;
    private float mValueSize;
    private float mValueOffset;
    private String mPrecisionFormat;

    //绘制单位
    private Paint mUnitPaint;
    private float mUnitSize;
    private int mUnitColor;
    private float mUnitOffset;
    private CharSequence mUnit;
    //前景圆弧
    private Paint mArcPaint;
    private float mArcWidth;
    //刻度之间的间隔
    private int mDialIntervalDegree;
    private float mStartAngle, mSweepAngle;
    private RectF mRectF;
    //渐变
    private int[] mGradientColors = {Color.GREEN, Color.YELLOW, Color.RED};
    //当前进度，[0.0f,1.0f]
    private float mPercent;
    //动画时间
    private long mAnimTime;

    //背景圆弧
    private Paint mBgArcPaint;
    private int mBgArcColor;

    //刻度线颜色
    private Paint mDialPaint;
    private float mDialWidth;
    private int mDialColor;

    private int mDefaultSize;

    public DialProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDefaultSize = DensityUtils.dp2px(DialProgress.DEFAULT_SIZE);
        mRectF = new RectF();
        mCenterPoint = new Point();
        initConfig(context, attrs);
        initPaint();
        setValue(mValue);
    }

    private void initConfig(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DialProgress);

        antiAlias = typedArray.getBoolean(R.styleable.DialProgress_antiAlias, true);
        mMaxValue = typedArray.getFloat(R.styleable.DialProgress_maxValue, DialProgress.DEFAULT_MAX_VALUE);
        mValue = typedArray.getFloat(R.styleable.DialProgress_value, DialProgress.DEFAULT_VALUE);
        mValueSize = typedArray.getDimension(R.styleable.DialProgress_valueSize, DialProgress.DEFAULT_VALUE_SIZE);
        mValueColor = typedArray.getColor(R.styleable.DialProgress_valueColor, Color.BLACK);
        mDialIntervalDegree = typedArray.getInt(R.styleable.DialProgress_dialIntervalDegree, 10);
        int precision = typedArray.getInt(R.styleable.DialProgress_precision, 0);
        mPrecisionFormat = DensityUtils.getPrecisionFormat(precision);

        mUnit = typedArray.getString(R.styleable.DialProgress_unit);
        mUnitColor = typedArray.getColor(R.styleable.DialProgress_unitColor, Color.BLACK);
        mUnitSize = typedArray.getDimension(R.styleable.DialProgress_unitSize, DialProgress.DEFAULT_UNIT_SIZE);

        mHint = typedArray.getString(R.styleable.DialProgress_hint);
        mHintColor = typedArray.getColor(R.styleable.DialProgress_hintColor, Color.BLACK);
        mHintSize = typedArray.getDimension(R.styleable.DialProgress_hintSize, DialProgress.DEFAULT_HINT_SIZE);

        mArcWidth = typedArray.getDimension(R.styleable.DialProgress_arcWidth, DialProgress.DEFAULT_ARC_WIDTH);

        mStartAngle = typedArray.getFloat(R.styleable.DialProgress_startAngle, DialProgress.DEFAULT_START_ANGLE);
        mSweepAngle = typedArray.getFloat(R.styleable.DialProgress_sweepAngle, DialProgress.DEFAULT_SWEEP_ANGLE);

        mAnimTime = typedArray.getInt(R.styleable.DialProgress_animTime, DialProgress.DEFAULT_ANIM_TIME);

        mBgArcColor = typedArray.getColor(R.styleable.DialProgress_bgArcColor, Color.GRAY);
        mDialWidth = typedArray.getDimension(R.styleable.DialProgress_dialWidth, 2);
        mDialColor = typedArray.getColor(R.styleable.DialProgress_dialColor, Color.WHITE);

        mTextOffsetPercentInRadius = typedArray.getFloat(R.styleable.DialProgress_textOffsetPercentInRadius, 0.33f);

        int gradientArcColors = typedArray.getResourceId(R.styleable.DialProgress_arcColors, 0);
        if (gradientArcColors != 0) {
            try {
                int[] gradientColors = getResources().getIntArray(gradientArcColors);
                if (gradientColors.length == 0) {
                    int color = getResources().getColor(gradientArcColors);
                    mGradientColors = new int[2];
                    mGradientColors[0] = color;
                    mGradientColors[1] = color;
                } else if (gradientColors.length == 1) {
                    mGradientColors = new int[2];
                    mGradientColors[0] = gradientColors[0];
                    mGradientColors[1] = gradientColors[0];
                } else {
                    mGradientColors = gradientColors;
                }
            } catch (Resources.NotFoundException e) {
                throw new Resources.NotFoundException("the give resource not found.");
            }
        }
        typedArray.recycle();
    }

    private void initPaint() {
        mHintPaint = new TextPaint();
        // 设置抗锯齿,会消耗较大资源，绘制图形速度会变慢。
        mHintPaint.setAntiAlias(antiAlias);
        // 设置绘制文字大小
        mHintPaint.setTextSize(mHintSize);
        // 设置画笔颜色
        mHintPaint.setColor(mHintColor);
        // 从中间向两边绘制，不需要再次计算文字
        mHintPaint.setTextAlign(Paint.Align.CENTER);

        mValuePaint = new Paint();
        mValuePaint.setAntiAlias(antiAlias);
        mValuePaint.setTextSize(mValueSize);
        mValuePaint.setColor(mValueColor);
        mValuePaint.setTypeface(Typeface.DEFAULT_BOLD);
        mValuePaint.setTextAlign(Paint.Align.CENTER);

        mUnitPaint = new Paint();
        mUnitPaint.setAntiAlias(antiAlias);
        mUnitPaint.setTextSize(mUnitSize);
        mUnitPaint.setColor(mUnitColor);
        mUnitPaint.setTextAlign(Paint.Align.CENTER);

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(antiAlias);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.BUTT);

        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(antiAlias);
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.BUTT);
        mBgArcPaint.setColor(mBgArcColor);

        mDialPaint = new Paint();
        mDialPaint.setAntiAlias(antiAlias);
        mDialPaint.setColor(mDialColor);
        mDialPaint.setStrokeWidth(mDialWidth);
    }

    /**
     * 更新圆弧画笔
     */
    private void updateArcPaint() {
        // 设置渐变
        // 渐变的颜色是360度，如果只显示270，那么则会缺失部分颜色
        SweepGradient sweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mGradientColors, null);
        mArcPaint.setShader(sweepGradient);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(DensityUtils.measure(widthMeasureSpec, mDefaultSize),
                DensityUtils.measure(heightMeasureSpec, mDefaultSize));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: w = " + w + "; h = " + h + "; oldw = " + oldw + "; oldh = " + oldh);
        int minSize = Math.min(getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - 2 * (int) mArcWidth,
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - 2 * (int) mArcWidth);
        mRadius = minSize / 2;
        mCenterPoint.x = getMeasuredWidth() / 2;
        mCenterPoint.y = getMeasuredHeight() / 2;
        //绘制圆弧的边界
        mRectF.left = mCenterPoint.x - mRadius - mArcWidth / 2;
        mRectF.top = mCenterPoint.y - mRadius - mArcWidth / 2;
        mRectF.right = mCenterPoint.x + mRadius + mArcWidth / 2;
        mRectF.bottom = mCenterPoint.y + mRadius + mArcWidth / 2;

        mValueOffset = mCenterPoint.y + getBaselineOffsetFromY(mValuePaint);
        mHintOffset = mCenterPoint.y - mRadius * mTextOffsetPercentInRadius + getBaselineOffsetFromY(mHintPaint);
        mUnitOffset = mCenterPoint.y + mRadius * mTextOffsetPercentInRadius + getBaselineOffsetFromY(mUnitPaint);

        updateArcPaint();
        Log.d(TAG, "onMeasure: 控件大小 = " + "(" + getMeasuredWidth() + ", " + getMeasuredHeight() + ")"
                + ";圆心坐标 = " + mCenterPoint.toString()
                + ";圆半径 = " + mRadius
                + ";圆的外接矩形 = " + mRectF.toString());
    }

    private float getBaselineOffsetFromY(Paint paint) {
        return DensityUtils.measureTextHeight(paint) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
        drawDial(canvas);
        drawText(canvas);
    }

    private void drawArc(Canvas canvas) {
        // 绘制背景圆弧
        // 从进度圆弧结束的地方开始重新绘制，优化性能
        float currentAngle = mSweepAngle * mPercent;
        canvas.save();
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        canvas.drawArc(mRectF, currentAngle, mSweepAngle - currentAngle, false, mBgArcPaint);
        // 第一个参数 oval 为 RectF 类型，即圆弧显示区域
        // startAngle 和 sweepAngle  均为 float 类型，分别表示圆弧起始角度和圆弧度数
        // 3点钟方向为0度，顺时针递增
        // 如果 startAngle < 0 或者 > 360,则相当于 startAngle % 360
        // useCenter:如果为True时，在绘制圆弧时将圆心包括在内，通常用来绘制扇形
        canvas.drawArc(mRectF, 0, currentAngle, false, mArcPaint);
        canvas.restore();
    }

    /**
     * 绘制刻度
     *
     * @param canvas
     */
    private void drawDial(Canvas canvas) {
        //获取分成多少个间隔
        int total = (int) (mSweepAngle / mDialIntervalDegree);
        canvas.save();
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);

        for (int i = 0; i <= total; i++) {
            //这一点可能比较难理解点:drawLine(...)从表面看画的是圆最右边的一条白线(白色小矩形),但是由于在drawArc()中已经将canvas顺时针旋转了135度,一次刻度间隔的白线也就从圆弧起点开始了
            canvas.drawLine(mCenterPoint.x + mRadius, mCenterPoint.y, mCenterPoint.x + mRadius + mArcWidth, mCenterPoint.y, mDialPaint);
            canvas.rotate(mDialIntervalDegree, mCenterPoint.x, mCenterPoint.y);
        }
        canvas.restore();
    }

    private void drawText(Canvas canvas) {
        canvas.drawText(String.format(mPrecisionFormat, mValue), mCenterPoint.x, mValueOffset, mValuePaint);

        if (mUnit != null) {
            canvas.drawText(mUnit.toString(), mCenterPoint.x, mUnitOffset, mUnitPaint);
        }

        if (mHint != null) {
            canvas.drawText(mHint.toString(), mCenterPoint.x, mHintOffset, mHintPaint);
        }
    }

    public float getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(float maxValue) {
        mMaxValue = maxValue;
    }

    /**
     * 设置当前值
     *
     * @param value
     */
    public void setValue(float value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        float start = mPercent;
        float end = value / mMaxValue;
        startAnimator(start, end, mAnimTime);
    }

    private void startAnimator(float start, float end, long animTime) {
        //属性动画
        ValueAnimator mAnimator = ValueAnimator.ofFloat(start, end);
        mAnimator.setDuration(animTime);
        mAnimator.addUpdateListener(animation -> {
            mPercent = (float) animation.getAnimatedValue();
            mValue = mPercent * mMaxValue;
            invalidate();
        });
        mAnimator.start();
    }

    public int[] getGradientColors() {
        return mGradientColors;
    }

    public void setGradientColors(int[] gradientColors) {
        mGradientColors = gradientColors;
        updateArcPaint();
    }

    public void reset() {
        startAnimator(mPercent, 0.0f, 1000L);
    }
}