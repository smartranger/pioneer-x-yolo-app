package com.tencent.yolov8ncnn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 用于在相机预览上绘制检测结果的覆盖视图
 */
public class DetectionOverlayView extends View {
    
    private static final String TAG = "DetectionOverlayView";
    
    // 框体颜色枚举
    public enum BoxColor {
        RED(Color.rgb(255, 217, 0)),  // rgba(255, 217, 0, 1)
        GREEN(Color.rgb(184, 251, 255)),  // rgba(184, 251, 255, 1)
        BLUE(Color.BLUE);
        
        private final int color;
        
        BoxColor(int color) {
            this.color = color;
        }
        
        public int getColor() {
            return color;
        }
    }
    
    // 当前框体颜色
    private BoxColor currentBoxColor = BoxColor.BLUE;
    
    // 检测结果是否正确
    private boolean isDetectionCorrect = false;
    
    // 绘制用的画笔
    private Paint boxPaint;
    private Paint textBgPaint;
    private Paint textPaint;
    
    // 检测到的对象数组
    private Yolov8Ncnn.DetectedObject[] objects;
    
    // 标签数组
    private String[] labels;
    
    // 边距调整
    private int marginLeft = 20;
    private int marginTop = 20;
    
    // 图像尺寸
    private int imageWidth = 0;
    private int imageHeight = 0;
    
    // 是否需要水平翻转（前置摄像头）
    private boolean flipHorizontal = false;
    
    public DetectionOverlayView(Context context) {
        super(context);
        init();
    }
    
    public DetectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public DetectionOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // 初始化画笔
        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5.0f);
        boxPaint.setColor(currentBoxColor.getColor());
        
        textBgPaint = new Paint();
        textBgPaint.setStyle(Paint.Style.FILL);
        textBgPaint.setColor(currentBoxColor.getColor());
        // 设置抗锯齿
        textBgPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setTextSize(48.0f);  // 增加文字大小
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        // 设置抗锯齿
        textPaint.setAntiAlias(true);
        // 设置粗体
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }
    
    /**
     * 设置框体颜色
     * @param color 颜色枚举值
     */
    public void setBoxColor(BoxColor color) {
        this.currentBoxColor = color;
        boxPaint.setColor(color.getColor());
        textBgPaint.setColor(color.getColor());
        invalidate(); // 请求重绘
    }
    
    /**
     * 获取当前框体颜色
     * @return 当前颜色枚举值
     */
    public BoxColor getBoxColor() {
        return currentBoxColor;
    }
    
    /**
     * 设置检测结果的正确性
     * @param isCorrect 检测结果是否正确
     */
    public void setDetectionCorrect(boolean isCorrect) {
        this.isDetectionCorrect = isCorrect;
        // 根据检测结果更新颜色
        currentBoxColor = isCorrect ? BoxColor.GREEN : BoxColor.RED;
        boxPaint.setColor(currentBoxColor.getColor());
        textBgPaint.setColor(currentBoxColor.getColor());
        invalidate(); // 请求重绘
    }
    
    /**
     * 获取当前检测结果是否正确
     * @return 检测结果是否正确
     */
    public boolean isDetectionCorrect() {
        return isDetectionCorrect;
    }
    
    /**
     * 设置检测到的对象
     */
    public void setDetectedObjects(Yolov8Ncnn.DetectedObject[] objects) {
        this.objects = objects;
        invalidate(); // 请求重绘
    }
    
    /**
     * 设置标签数组
     */
    public void setLabels(String[] labels) {
        this.labels = labels;
    }
    
    /**
     * 设置图像尺寸
     */
    public void setImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }
    
    /**
     * 设置是否水平翻转
     */
    public void setFlipHorizontal(boolean flip) {
        this.flipHorizontal = flip;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (objects == null || objects.length == 0 || labels == null) {
            return;
        }
        
        int viewWidth = getWidth() - 2 * marginLeft;
        int viewHeight = getHeight() - 2 * marginTop;
        
        for (Yolov8Ncnn.DetectedObject obj : objects) {
            // 确保标签索引不越界
            if (obj.label < 0 || obj.label >= labels.length) {
                continue;
            }
            
            // 检查是否有帧尺寸信息
            if (obj.frameWidth <= 0 || obj.frameHeight <= 0) {
                // 如果对象没有帧尺寸信息，使用之前设置的图像尺寸
                if (imageWidth <= 0 || imageHeight <= 0) {
                    continue; // 没有尺寸信息，无法绘制
                }
            }
            
            // 使用对象中的帧尺寸信息或者回退到之前设置的图像尺寸
            int frameWidth = (obj.frameWidth > 0) ? obj.frameWidth : imageWidth;
            int frameHeight = (obj.frameHeight > 0) ? obj.frameHeight : imageHeight;
            
            // 计算缩放比例
            float scaleX = (float) viewWidth / frameWidth;
            float scaleY = (float) viewHeight / frameHeight;
            
            Log.d(TAG, String.format("视图尺寸: viewWidth=%d, viewHeight=%d", viewWidth, viewHeight));
            Log.d(TAG, String.format("帧尺寸: frameWidth=%d, frameHeight=%d", frameWidth, frameHeight));
            Log.d(TAG, String.format("缩放比例: scaleX=%.2f, scaleY=%.2f", scaleX, scaleY));
            
            // 使用当前设置的颜色
            int color = currentBoxColor.getColor();
            boxPaint.setColor(color);
            textBgPaint.setColor(color);
            
            float objX = obj.x;
            float objWidth = obj.width;
            
            // 如果需要水平翻转（前置摄像头）
            if (flipHorizontal) {
                objX = frameWidth - obj.x - obj.width;
            }
            
            // 坐标转换 - 将图像坐标转换为视图坐标
            float left = objX * scaleX + marginLeft;
            float top = obj.y * scaleY + marginTop;
            float right = left + (objWidth * scaleX);
            float bottom = top + (obj.height * scaleY);
            
            // 输出日志用于调试
            Log.d(TAG, String.format("原始坐标: x=%.1f, y=%.1f, w=%.1f, h=%.1f", 
                obj.x, obj.y, obj.width, obj.height));
            Log.d(TAG, String.format("转换坐标: left=%.1f, top=%.1f, right=%.1f, bottom=%.1f", 
                left, top, right, bottom));
            
            // 绘制边框（标准样式用5.0粗细）
            boxPaint.setStrokeWidth(5.0f);
            canvas.drawRect(left, top, right, bottom, boxPaint);
            
            // 只有当检测结果正确时才显示标签
            if (isDetectionCorrect) {
                // 标准样式显示标签和概率
                String text = String.format("%s", labels[obj.label]);
                
                // 使用标准文本大小
                textPaint.setTextSize(48.0f);  // 增加文字大小
                
                // 计算文本尺寸
                float textWidth = textPaint.measureText(text);
                Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
                float textHeight = fontMetrics.descent - fontMetrics.ascent;
                
                // 计算标签位置（标准样式放在边框上方）
                float labelX = left + 8;
                float labelY = top - textHeight - 15;
                
                // 确保标签在视图内
                if (labelY < 0) {
                    labelY = 0;
                }
                if (labelX + textWidth > getWidth()) {
                    labelX = getWidth() - textWidth;
                }
                
                // 设置内边距
                float paddingX = 12.0f;  // 内边距大小
                float paddingY = 6.0f;
                float cornerRadius = 16.0f;  // 圆角半径
                
                // 绘制圆角矩形背景
                canvas.drawRoundRect(
                    labelX - paddingX, 
                    labelY - paddingY, 
                    labelX + textWidth + paddingX, 
                    labelY + textHeight + paddingY,
                    cornerRadius,
                    cornerRadius,
                    textBgPaint
                );
                
                // 根据背景色亮度选择文字颜色
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                int textColor = (r + g + b >= 381) ? Color.BLACK : Color.WHITE;
                textPaint.setColor(textColor);
                
                // 绘制文本
                canvas.drawText(text, labelX, labelY + textHeight - fontMetrics.descent, textPaint);
            }
        }
    }
    
    /**
     * 重置为默认颜色（蓝色）
     */
    public void resetToDefaultColor() {
        currentBoxColor = BoxColor.BLUE;
        boxPaint.setColor(currentBoxColor.getColor());
        textBgPaint.setColor(currentBoxColor.getColor());
        invalidate(); // 请求重绘
    }
} 