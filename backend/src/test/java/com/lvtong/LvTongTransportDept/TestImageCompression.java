package com.lvtong.LvTongTransportDept;

import com.lvtong.LvTongTransportDept.converter.TransportDeptConverter;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 图片压缩功能测试
 *
 * 通过反射调用 TransportDeptConverter.readImageAsBase64 验证压缩逻辑：
 * 1. 缩放：宽度超过1280时等比缩放
 * 2. JPEG编码：使用ImageIO默认质量
 * 3. Base64编码
 */
public class TestImageCompression {

    private static final int MAX_IMAGE_WIDTH = 1280;
    private static final String TEST_IMAGE_DIR = "D:\\LvTongTransportDept\\LvTongTransportDept\\";

    public static void main(String[] args) {
        System.out.println("=== 图片压缩测试 ===\n");
            String imageName="test.jpg";

            String imagePath = TEST_IMAGE_DIR + imageName;
            File file = new File(imagePath);

            if (!file.exists()) {
                System.out.println("跳过不存在: " + imagePath);
            }

            System.out.println("测试图片: " + imageName);
            System.out.println("-".repeat(50));

            try {
                testCompression(imagePath);
            } catch (Exception e) {
                System.out.println("压缩失败: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println();


        System.out.println("=== 测试完成 ===");
    }

    private static void testCompression(String imagePath) throws IOException {
        // 1. 读取原图信息
        File file = new File(imagePath);
        BufferedImage original = ImageIO.read(file);
        if (original == null) {
            System.out.println("无法读取图片");
            return;
        }

        long originalSize = file.length();
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        System.out.println("原图信息:");
        System.out.println("  尺寸: " + originalWidth + " x " + originalHeight);
        System.out.println("  文件大小: " + originalSize / 1024 + " KB");

        // 2. 执行压缩（模拟TransportDeptConverter中的逻辑）
        long startTime = System.currentTimeMillis();
        String base64Result = compressImage(imagePath);
        long compressTime = System.currentTimeMillis() - startTime;

        if (base64Result == null) {
            System.out.println("压缩失败: 返回null");
            return;
        }

        // 3. 验证压缩结果
        byte[] decoded = Base64.getDecoder().decode(base64Result);

        System.out.println("\n压缩后:");
        System.out.println("  Base64长度: " + base64Result.length() + " 字符");
        System.out.println("  解码后大小: " + decoded.length / 1024 + " KB");
        System.out.println("  压缩比: " + String.format("%.1f%%", decoded.length * 100.0 / originalSize));

        // 4. 分析是否执行了缩放
        System.out.println("\n缩放分析:");
        if (originalWidth > MAX_IMAGE_WIDTH) {
            double ratio = (double) MAX_IMAGE_WIDTH / originalWidth;
            int expectedHeight = (int) (originalHeight * ratio);
            System.out.println("  原图宽度 " + originalWidth + " > " + MAX_IMAGE_WIDTH + "，应缩放");
            System.out.println("  预期高度: " + originalHeight + " -> " + expectedHeight + " (比例: " + String.format("%.2f", ratio) + ")");
        } else {
            System.out.println("  原图宽度 " + originalWidth + " <= " + MAX_IMAGE_WIDTH + "，无需缩放");
        }

        // 5. 将压缩后的图片写入临时文件以便验证
        Path outputPath = file.getParentFile().toPath().resolve("compressed_" + file.getName());
        Files.write(outputPath, decoded);
        System.out.println("\n压缩后图片已保存: " + outputPath);

        // 验证压缩后的尺寸
        BufferedImage compressedImage = ImageIO.read(outputPath.toFile());
        if (compressedImage != null) {
            System.out.println("压缩后实际尺寸: " + compressedImage.getWidth() + " x " + compressedImage.getHeight());
        }

        System.out.println("\n压缩耗时: " + compressTime + " ms");
    }

    /**
     * 通过反射调用 TransportDeptConverter.readImageAsBase64
     */
    private static String compressImage(String imagePath) {
        File file = new File(imagePath);
        if (!file.exists()) {
            System.out.println("文件不存在: " + imagePath);
            return null;
        }

        try {
            // 创建 TransportDeptConverter 实例
            TransportDeptConverter converter = TransportDeptConverter.class.getDeclaredConstructor().newInstance();

            // 获取 private 方法 readImageAsBase64
            java.lang.reflect.Method method = TransportDeptConverter.class.getDeclaredMethod(
                    "readImageAsBase64", String.class, List.class);
            method.setAccessible(true);  // 允许访问 private 方法

            // 调用方法
            List<String> errors = new ArrayList<>();
            String result = (String) method.invoke(converter, imagePath, errors);

            // 打印错误信息
            if (!errors.isEmpty()) {
                System.out.println("压缩过程中的错误:");
                for (String err : errors) {
                    System.out.println("  - " + err);
                }
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}