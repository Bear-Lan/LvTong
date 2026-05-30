package com.lvtong.LvTongTransportDept;

import com.lvtong.LvTongTransportDept.hksdk.HCNetSDK;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.*;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

public class HikTest {
    public static void main(String[] args) throws Exception {
        // 用 JNA 直接加载，library name 不要加.dll后缀，JNA会自动找
        // 但需要确保工作目录是 hikLib/windows，或者把 DLL 所在目录加到 PATH
        File dllDir = new File("D:/LvTongTransportDept/LvTongTransportDept/backend/src/main/resources/static/hikLib/windows");
        System.setProperty("jna.library.path", dllDir.getAbsolutePath());
        HCNetSDK sdk = (HCNetSDK) Native.loadLibrary("HCNetSDK", HCNetSDK.class);

        sdk.NET_DVR_Init();
        sdk.NET_DVR_SetConnectTime(2000, 1);
        sdk.NET_DVR_SetReconnect(100000, true);

        HCNetSDK.NET_DVR_DEVICEINFO_V30 dev = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        dev.write();
        NativeLong uid = sdk.NET_DVR_Login_V30("192.168.218.250", (short) 8000, "admin", "whds@2025", dev);
        if (uid.intValue() == -1) {
            System.out.println("登录失败: " + sdk.NET_DVR_GetLastError());
            return;
        }
        System.out.println("登录成功 uid=" + uid);

        FileOutputStream fos = new FileOutputStream("D:/hik_test_raw.dat");
        long[] bytes = {0};
        long[] pkts = {0, 0, 0, 0, 0};

        HCNetSDK.FPlayDataCallBack cb = new HCNetSDK.FPlayDataCallBack() {
            public void invoke(int h, int type, Pointer buf, int size, int user) {
                if (size <= 0) return;
                try {
                    ByteBuffer b = buf.getByteBuffer(0, size);
                    byte[] d = new byte[size];
                    b.get(d);
                    fos.write(d);
                    bytes[0] += size;
                    pkts[0]++;
                    if (type == 1) pkts[1]++;
                    else if (type == 2) pkts[2]++;
                    else if (type == 3) pkts[3]++;
                    else if (type == 4) pkts[4]++;
                    System.out.printf("pkt=%d size=%d type=%d total=%.2fMB%n", pkts[0], size, type, bytes[0]/1024.0/1024.0);
                } catch (Exception e) { e.printStackTrace(); }
            }
        };

        LocalDateTime s = LocalDateTime.of(2026, 5, 29, 14, 39, 42);
        LocalDateTime e = LocalDateTime.of(2026, 5, 29, 14, 50, 59);

        HCNetSDK.NET_DVR_VOD_PARA p = new HCNetSDK.NET_DVR_VOD_PARA();
        p.dwSize = p.size();
        p.struIDInfo.dwSize = p.struIDInfo.size();
        p.struIDInfo.dwChannel = 33; // channel 3 -> NVR chan 35
        p.struBeginTime.dwYear = s.getYear(); p.struBeginTime.dwMonth = s.getMonthValue();
        p.struBeginTime.dwDay = s.getDayOfMonth(); p.struBeginTime.dwHour = s.getHour();
        p.struBeginTime.dwMinute = s.getMinute(); p.struBeginTime.dwSecond = s.getSecond();
        p.struEndTime.dwYear = e.getYear(); p.struEndTime.dwMonth = e.getMonthValue();
        p.struEndTime.dwDay = e.getDayOfMonth(); p.struEndTime.dwHour = e.getHour();
        p.struEndTime.dwMinute = e.getMinute(); p.struEndTime.dwSecond = e.getSecond();
        p.hWnd = null;
        p.write();

        NativeLong ph = sdk.NET_DVR_PlayBackByTime_V40(uid, p);
        if (ph.intValue() == -1) {
            System.out.println("回放失败: " + sdk.NET_DVR_GetLastError());
            fos.close(); sdk.NET_DVR_Logout(uid);
            return;
        }
        System.out.println("回放句柄=" + ph);

        sdk.NET_DVR_PlayBackControl_V40(ph, 1, Pointer.NULL, 0, Pointer.NULL, new IntByReference(0));
        sdk.NET_DVR_SetPlayDataCallBack(ph, cb, Pointer.NULL);
        System.out.println("开始采集中，每行代表一个数据包，按 Ctrl+C 停止...");

        Thread.sleep(30000); // 30秒后自动停止

        sdk.NET_DVR_StopPlayBack(ph);
        sdk.NET_DVR_Logout(uid);
        fos.flush(); fos.close();

        System.out.println("========== 最终结果 ==========");
        System.out.println("总字节: " + bytes[0] + " (" + String.format("%.2f", bytes[0]/1024.0/1024.0) + " MB)");
        System.out.println("总数据包: " + pkts[0]);
        System.out.println("type1(视频): " + pkts[1]);
        System.out.println("type2(音频): " + pkts[2]);
        System.out.println("type3: " + pkts[3]);
        System.out.println("type4: " + pkts[4]);
        System.out.println("输出文件: D:/hik_test_raw.dat (" + new File("D:/hik_test_raw.dat").length() + " bytes)");
    }
}