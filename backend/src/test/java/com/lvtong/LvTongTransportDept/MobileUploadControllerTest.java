package com.lvtong.LvTongTransportDept;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.*;

/**
 * 分片上传测试 - 单独运行
 *
 * 测试步骤：
 * 1. 分片上传：把大文件切成多个小块上传
 * 2. 合并分片：上传完成后调用合并接口
 */
public class MobileUploadControllerTest {

    private static final String BASE_URL = "https://localhost:9090";
    private static final int CHUNK_SIZE = 512 * 1024; // 512KB

    public static void main(String[] args) {
        disableSSLVerification();

        System.out.println("=== 分片上传测试 ===");

        String dirName = "test_chunk_" + System.currentTimeMillis();
        String fileName = "20260414_184240_76464.jpg";
        String filePath = "D:\\LvTongTransportDept\\20260414_184240_76464.png";

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("测试文件不存在: " + filePath);
            return;
        }

        try {
            byte[] fileData = Files.readAllBytes(file.toPath());
            int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);

            System.out.println("文件大小: " + fileData.length + " bytes");
            System.out.println("分片大小: " + CHUNK_SIZE + " bytes");
            System.out.println("总分片数: " + totalChunks);
            System.out.println("dirName: " + dirName);

            // 1. 分片上传
            boolean allUploaded = true;
            for (int i = 0; i < totalChunks; i++) {
                int start = i * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, fileData.length - start);
                byte[] chunkData = Arrays.copyOfRange(fileData, start, start + length);

                System.out.println("\n上传分片 " + (i + 1) + "/" + totalChunks + " ("
                        + start + "-" + (start + length) + ")...");

                boolean success = uploadChunk(dirName, fileName, i, totalChunks, chunkData);
                if (!success) {
                    allUploaded = false;
                    System.out.println("分片 " + i + " 上传失败！");
                    break;
                }
                System.out.println("分片 " + i + " 上传成功");
            }

            if (!allUploaded) {
                System.out.println("分片上传失败，终止测试");
                return;
            }

            // 2. 合并分片
            System.out.println("\n开始合并分片...");
            boolean mergeSuccess = mergeChunks(dirName, fileName, totalChunks);
            if (mergeSuccess) {
                System.out.println("合并成功！");
            } else {
                System.out.println("合并失败！");
            }

        } catch (Exception e) {
            System.out.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean uploadChunk(String dirName, String fileName, int chunkIndex,
            int totalChunks, byte[] chunkData) {
        try {
            String urlStr = BASE_URL + "/api/mobile/upload/chunk";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----Boundary");

            String boundary = "----Boundary";
            String ext = fileName.substring(fileName.lastIndexOf('.'));

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
            String response = readResponse(conn);
            conn.disconnect();

            System.out.println("Response Code: " + responseCode);
            System.out.println("Response: " + response);

            return responseCode == 200;
        } catch (Exception e) {
            System.out.println("上传分片失败: " + e.getMessage());
            return false;
        }
    }

    private static boolean mergeChunks(String dirName, String fileName, int totalChunks) {
        try {
            String urlStr = BASE_URL + "/api/mobile/upload/merge";
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
            String response = readResponse(conn);
            conn.disconnect();

            System.out.println("Response Code: " + responseCode);
            System.out.println("Response: " + response);

            return responseCode == 200;
        } catch (Exception e) {
            System.out.println("合并失败: " + e.getMessage());
            return false;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
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