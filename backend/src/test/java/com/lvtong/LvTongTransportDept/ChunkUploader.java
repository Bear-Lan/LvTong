package com.lvtong.LvTongTransportDept;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.*;

/**
 * 分片上传工具类
 *
 * 使用示例：
 *   ChunkUploadResult result = ChunkUploader.upload("https://localhost:9090", "test_dir", "photo.jpg", "C:/test.jpg");
 *   if (result.isSuccess()) {
 *       System.out.println("上传成功: " + result.getSavedPath());
 *   }
 */
public class ChunkUploader {

    private static final int DEFAULT_CHUNK_SIZE = 512 * 1024; // 512KB

    // ================================================================
    // 对外接口
    // ================================================================

    /**
     * 分片上传文件（使用默认分片大小）
     *
     * @param serverUrl 服务器地址，如 https://localhost:9090
     * @param dirName   文件夹名称
     * @param fileName 原始文件名
     * @param filePath 本地文件路径
     * @return 上传结果
     */
    public static ChunkUploadResult upload(String serverUrl, String dirName, String fileName, String filePath) {
        return upload(serverUrl, dirName, fileName, filePath, DEFAULT_CHUNK_SIZE);
    }

    /**
     * 分片上传文件（自定义分片大小）
     *
     * @param serverUrl  服务器地址，如 https://localhost:9090
     * @param dirName   文件夹名称
     * @param fileName 原始文件名
     * @param filePath 本地文件路径
     * @param chunkSize 分片大小（字节），建议 512KB
     * @return 上传结果
     */
    public static ChunkUploadResult upload(String serverUrl, String dirName, String fileName,
            String filePath, int chunkSize) {
        ChunkUploadResult result = new ChunkUploadResult();
        result.setDirName(dirName);
        result.setFileName(fileName);

        long startTime = System.currentTimeMillis();

        // 1. 读取文件
        File file = new File(filePath);
        if (!file.exists()) {
            result.setSuccess(false);
            result.setError("文件不存在: " + filePath);
            return result;
        }

        byte[] fileData;
        try {
            fileData = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            result.setSuccess(false);
            result.setError("读取文件失败: " + e.getMessage());
            return result;
        }

        int totalChunks = (int) Math.ceil((double) fileData.length / chunkSize);
        result.setTotalChunks(totalChunks);
        result.setFileSize(fileData.length);

        // 2. 分片上传
        String actualDirName = dirName != null && !dirName.isEmpty()
                ? dirName
                : "chunk_" + System.currentTimeMillis();

        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int length = Math.min(chunkSize, fileData.length - start);
            byte[] chunkData = Arrays.copyOfRange(fileData, start, start + length);

            boolean success = uploadChunk(serverUrl, actualDirName, fileName, i, totalChunks, chunkData);
            if (!success) {
                result.setSuccess(false);
                result.setError("分片 " + i + " 上传失败");
                result.setMessage("已上传 " + i + "/" + totalChunks + " 个分片");
                return result;
            }
        }

        // 3. 合并分片
        boolean mergeSuccess = mergeChunks(serverUrl, actualDirName, fileName, totalChunks);
        if (!mergeSuccess) {
            result.setSuccess(false);
            result.setError("合并分片失败");
            return result;
        }

        long elapsedMs = System.currentTimeMillis() - startTime;
        result.setSuccess(true);
        result.setMessage("上传成功");
        result.setSavedPath(serverUrl + "/api/mobile/upload/" + actualDirName + "/" + fileName);
        result.setElapsedMs(elapsedMs);

        return result;
    }

    // ================================================================
    // 内部方法
    // ================================================================

    private static boolean uploadChunk(String serverUrl, String dirName, String fileName,
            int chunkIndex, int totalChunks, byte[] chunkData) {
        try {
            String urlStr = serverUrl + "/api/mobile/upload/chunk";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----Boundary");

            String boundary = "----Boundary";
            String ext = getExtension(fileName);

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"dirName\"\r\n\r\n");
                dos.writeBytes(dirName + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"fileName\"\r\n\r\n");
                dos.writeBytes(fileName + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"chunkIndex\"\r\n\r\n");
                dos.writeBytes(String.valueOf(chunkIndex) + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"totalChunks\"\r\n\r\n");
                dos.writeBytes(String.valueOf(totalChunks) + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"chunk"
                        + chunkIndex + ext + "\"\r\n");
                dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                dos.write(chunkData);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean mergeChunks(String serverUrl, String dirName, String fileName, int totalChunks) {
        try {
            String urlStr = serverUrl + "/api/mobile/upload/merge";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----Boundary");

            String boundary = "----Boundary";

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"dirName\"\r\n\r\n");
                dos.writeBytes(dirName + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"fileName\"\r\n\r\n");
                dos.writeBytes(fileName + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"totalChunks\"\r\n\r\n");
                dos.writeBytes(String.valueOf(totalChunks) + "\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot < 0 ? "" : fileName.substring(lastDot);
    }

    // ================================================================
    // Result 类
    // ================================================================

    /**
     * 分片上传结果
     */
    public static class ChunkUploadResult {
        private boolean success;
        private String message;
        private String dirName;
        private String fileName;
        private String savedPath;
        private int totalChunks;
        private long fileSize;
        private long elapsedMs;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDirName() {
            return dirName;
        }

        public void setDirName(String dirName) {
            this.dirName = dirName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getSavedPath() {
            return savedPath;
        }

        public void setSavedPath(String savedPath) {
            this.savedPath = savedPath;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        public void setTotalChunks(int totalChunks) {
            this.totalChunks = totalChunks;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public void setElapsedMs(long elapsedMs) {
            this.elapsedMs = elapsedMs;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return "ChunkUploadResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", dirName='" + dirName + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", savedPath='" + savedPath + '\'' +
                    ", totalChunks=" + totalChunks +
                    ", fileSize=" + fileSize +
                    ", elapsedMs=" + elapsedMs +
                    ", error='" + error + '\'' +
                    '}';
        }
    }

    // ================================================================
    // 主函数（测试用）
    // ================================================================

    public static void main(String[] args) {
        disableSSLVerification();

        // 初始化SSL
        disableSSLVerification();

        System.out.println("=== 分片上传测试 ===");
        System.out.println();

        ChunkUploadResult result = upload(
                "https://localhost:9090",
                "test_upload",
                "图片1.jpg",
                "C:\\Users\\53653\\Pictures\\图片1.jpg"
        );

        System.out.println(result);
    }

    private static void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}