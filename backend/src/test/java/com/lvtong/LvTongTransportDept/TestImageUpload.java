package com.lvtong.LvTongTransportDept;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * 图片上传测试 - 单独运行
 */
public class TestImageUpload {

    private static final String BASE_URL = "https://localhost:8090";

    public static void main(String[] args) {
        // 忽略SSL证书验证
        disableSSLVerification();

        System.out.println("=== 图片上传测试 ===");
        try {
            String urlStr = BASE_URL + "/api/mobile/upload/image";
            String dirName = "test_photo_20260401";
            String filePath = "C:\\Users\\53653\\Pictures\\图片1.jpg";

            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("测试文件不存在: " + filePath);
                return;
            }

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----Boundary");

            byte[] fileContent = Files.readAllBytes(file.toPath());
            String boundary = "----Boundary";

            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"dirName\"\r\n\r\n");
                dos.writeBytes(dirName + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(fileContent);
                dos.writeBytes("\r\n");

                dos.writeBytes("--" + boundary + "--\r\n");
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            String response = readResponse(conn);
            System.out.println("Response Body: " + response);

            conn.disconnect();
        } catch (Exception e) {
            System.out.println("请求失败: " + e.getMessage());
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
