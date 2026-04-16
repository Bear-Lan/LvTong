package com.lvtong.LvTongTransportDept.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片水印工具类
 * 使用 Graphics2D 在图片右下角绘制水印
 */
public class ImageWatermarkUtil {

    // 中文字体
    private static Font chineseFont;

    static {
        try {
            chineseFont = new Font("SimSun", Font.PLAIN, 25);
            if (!chineseFont.canDisplay('测')) {
                String[] fontNames = {"Microsoft YaHei", "SimHei", "Arial Unicode MS", "Dialog"};
                for (String name : fontNames) {
                    Font f = new Font(name, Font.PLAIN, 25);
                    if (f.canDisplay('测')) {
                        chineseFont = f;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            chineseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 25);
        }
    }

    /**
     * 在图片上绘制水印并覆盖原文件
     *
     * @param imagePath 图片文件路径
     * @param plateNumber 车牌号码
     * @param plateColorName 车牌颜色名称
     * @param vehicleType 车辆类型
     * @param vehicleContainerType 车厢类型
     * @param goodsType 货物类型
     * @param loadRate 装载率
     * @param detectDate 检测日期
     * @return 是否成功
     */
    public static boolean drawWatermarkAndOverwrite(
            String imagePath,
            String plateNumber,
            String plateColorName,
            String vehicleType,
            String vehicleContainerType,
            String goodsType,
            String loadRate,
            String detectDate) {

        if (imagePath == null || imagePath.isBlank()) {
            return false;
        }

        // 推断原始图片路径（数据库图片名 + _level）
        String originalImagePath = imagePath.replace(".png", "_level.png").replace(".jpg", "_level.jpg");

        File originalFile = new File(originalImagePath);
        File outputFile = new File(imagePath);

        if (!originalFile.exists()) {
            System.err.println("原始图片不存在: " + originalImagePath);
            return false;
        }

        if (!outputFile.canWrite()) {
            System.err.println("无法写入图片: " + imagePath);
            return false;
        }

        try {
            // 从原始图片读取并绘制水印，输出到数据库存放的图片路径
            drawWatermark(originalFile, plateNumber, plateColorName, vehicleType, vehicleContainerType,
                    goodsType, loadRate, detectDate, outputFile.getName(), outputFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 在图片右下角绘制水印
     */
    private static void drawWatermark(
            File sourceFile,
            String plateNumber,
            String plateColorName,
            String vehicleType,
            String vehicleContainerType,
            String goodsType,
            String loadRate,
            String detectDate,
            String filename,
            File outputFile) throws IOException {

        BufferedImage originalImage = ImageIO.read(sourceFile);
        if (originalImage == null) {
            return;
        }

        int imgWidth = originalImage.getWidth();
        int imgHeight = originalImage.getHeight();

        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int nFontHeight = 25;
        int nYoffset = 40;

        // 水印内容
        String[] lines = new String[6];
        lines[0] = "车牌:" + (plateNumber != null ? plateNumber : "-")
                + "(" + plateColorName + ")";
        lines[1] = "车辆类型:" + (vehicleType != null ? vehicleType : "-");
        lines[2] = "车厢类型:" + (vehicleContainerType != null ? vehicleContainerType : "-");
        lines[3] = "货物类型:" + (goodsType != null ? goodsType : "-");
        lines[4] = "装载率:" + (loadRate != null ? loadRate : "-");
        lines[5] = "检测日期:" + (detectDate != null ? detectDate : "-");

        // 测量最长的一行文字宽度，确保水印在右下角
        Font measureFont = chineseFont.deriveFont(Font.BOLD, nFontHeight);
        g2d.setFont(measureFont);
        FontMetrics fm = g2d.getFontMetrics();
        int maxLineWidth = 0;
        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            if (lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }

        // 根据图片大小动态设置字体和边距
        // 水印总高度
        int watermarkHeight = lines.length * nYoffset;
        int rightMargin;
        int bottomMargin;

        if (imgWidth >= 1500 && imgHeight >= 800) {
            // 大图片：使用较大边距
            rightMargin = 30;
            bottomMargin = 30;
        } else if (imgWidth >= 800 && imgHeight >= 400) {
            // 中等图片：使用较小边距，缩小字体
            rightMargin = 15;
            bottomMargin = 15;
            nFontHeight = 18;
            nYoffset = 28;
            watermarkHeight = lines.length * nYoffset;
            // 重新测量文字宽度（使用新字体）
            measureFont = chineseFont.deriveFont(Font.BOLD, nFontHeight);
            g2d.setFont(measureFont);
            fm = g2d.getFontMetrics();
            maxLineWidth = 0;
            for (String line : lines) {
                int lineWidth = fm.stringWidth(line);
                if (lineWidth > maxLineWidth) {
                    maxLineWidth = lineWidth;
                }
            }
        } else {
            // 小图片：使用最小边距，进一步缩小字体
            rightMargin = 10;
            bottomMargin = 10;
            nFontHeight = 14;
            nYoffset = 22;
            watermarkHeight = lines.length * nYoffset;
            // 重新测量文字宽度（使用新字体）
            measureFont = chineseFont.deriveFont(Font.BOLD, nFontHeight);
            g2d.setFont(measureFont);
            fm = g2d.getFontMetrics();
            maxLineWidth = 0;
            for (String line : lines) {
                int lineWidth = fm.stringWidth(line);
                if (lineWidth > maxLineWidth) {
                    maxLineWidth = lineWidth;
                }
            }
        }

        // 计算水印起始位置：右下角，保留适当边距
        int nXStartPos = imgWidth - rightMargin - maxLineWidth;
        int nYStartPos = imgHeight - bottomMargin - watermarkHeight;

        Color textColor =  new Color(209, 122, 199);
        for (int i = 0; i < lines.length; i++) {
            int y = nYStartPos + nFontHeight + i * nYoffset;
            // 绘制文字
            g2d.setColor(textColor);
            g2d.setFont(chineseFont.deriveFont(Font.BOLD, nFontHeight));
            g2d.drawString(lines[i], nXStartPos, y);
        }

        g2d.dispose();

        // 覆盖写入输出文件
        String ext = getFileExtension(outputFile.getName());
        String format = "png".equalsIgnoreCase(ext) ? "png" : "jpeg";
        ImageIO.write(image, format, outputFile);
    }


    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "jpg";
    }
}