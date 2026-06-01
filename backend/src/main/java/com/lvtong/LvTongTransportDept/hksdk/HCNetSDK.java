package com.lvtong.LvTongTransportDept.hksdk;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.Structure.FieldOrder;

/**
 * 海康威视 HCNetSDK JNA接口
 * 参考官方ClientDemo的HCNetSDK.java结构体定义
 */
public interface HCNetSDK extends Library {

    // 回调函数定义
    interface FPlayDataCallBack extends Callback {}

    // ========== 基础结构体 ==========

    // NET_DVR_TIME - 时间结构
    @FieldOrder({"dwYear", "dwMonth", "dwDay", "dwHour", "dwMinute", "dwSecond"})
    class NET_DVR_TIME extends Structure {
        public int dwYear;
        public int dwMonth;
        public int dwDay;
        public int dwHour;
        public int dwMinute;
        public int dwSecond;
    }

    // NET_DVR_STREAM_INFO - 流信息结构（用于回放）
    @FieldOrder({"dwSize", "byID", "dwChannel", "byRes"})
    class NET_DVR_STREAM_INFO extends Structure {
        public int dwSize;
        public byte[] byID = new byte[32];
        public int dwChannel;
        public byte[] byRes = new byte[32];
    }

    // NET_DVR_DEVICEINFO_V30 - 设备信息结构
    @FieldOrder({"sSerialNumber", "byAlarmInPortNum", "byAlarmOutPortNum", "byDiskNum", "byDVRType", "byChanNum", "byStartChan", "byAudioChanNum", "byIPChanNum", "byZeroChanNum", "byMainProto", "bySubProto", "bySupport", "bySupport1", "bySupport2", "wDevType", "bySupport3", "byMultiStreamProto", "byStartDChan", "byStartDTalkChan", "byRes"})
    class NET_DVR_DEVICEINFO_V30 extends Structure {
        public byte[] sSerialNumber = new byte[48];
        public byte byAlarmInPortNum;
        public byte byAlarmOutPortNum;
        public byte byDiskNum;
        public byte byDVRType;
        public byte byChanNum;
        public byte byStartChan;
        public byte byAudioChanNum;
        public byte byIPChanNum;
        public byte[] byZeroChanNum = new byte[64];
        public byte[] byMainProto = new byte[32];
        public byte[] bySubProto = new byte[32];
        public byte bySupport;
        public byte bySupport1;
        public byte bySupport2;
        public short wDevType;
        public byte bySupport3;
        public byte byMultiStreamProto;
        public byte byStartDChan;
        public byte byStartDTalkChan;
        public byte[] byRes = new byte[32];
    }

    // NET_DVR_VOD_PARA - 回放参数结构（官方完整定义）
    @FieldOrder({"dwSize", "struIDInfo", "struBeginTime", "struEndTime", "hWnd", "byDrawFrame", "byVolumeType", "byVolumeNum", "byStreamType", "dwFileIndex", "byAudioFile", "byCourseFile", "byDownload", "byOptimalStreamType", "byRes2"})
    class NET_DVR_VOD_PARA extends Structure {
        public int dwSize;
        public NET_DVR_STREAM_INFO struIDInfo = new NET_DVR_STREAM_INFO();
        public NET_DVR_TIME struBeginTime = new NET_DVR_TIME();
        public NET_DVR_TIME struEndTime = new NET_DVR_TIME();
        public Pointer hWnd;
        public byte byDrawFrame;
        public byte byVolumeType;
        public byte byVolumeNum;
        public byte byStreamType;
        public int dwFileIndex;
        public byte byAudioFile;
        public byte byCourseFile;
        public byte byDownload;
        public byte byOptimalStreamType;
        public byte[] byRes2 = new byte[20];
    }

    // ========== 常量定义 ==========
    int NET_DVR_PLAYSTART = 1;
    int NET_DVR_PLAYSTOP = 2;
    int NET_DVR_PLAYPAUSE = 3;
    int NET_DVR_PLAYRESTART = 4;
    int NET_DVR_PLAYFAST = 5;
    int NET_DVR_PLAYSLOW = 6;
    int NET_DVR_PLAYNORMAL = 7;
    int NET_DVR_PLAYFRAME = 8;
    int NET_DVR_PLAYSTARTAUDIO = 9;
    int NET_DVR_PLAYSTOPAUDIO = 10;
    int NET_DVR_PLAYGETPOS = 104;
    int NET_DVR_PLAYGETBUF = 105;
    int NET_DVR_PLAYSETPOS = 106;

    // ========== SDK基础接口 ==========

    // 初始化SDK
    boolean NET_DVR_Init();

    // 释放SDK
    boolean NET_DVR_Cleanup();

    // 设置连接超时
    boolean NET_DVR_SetConnectTime(int dwWaitTime, int dwTryTimes);

    // 设置重连
    boolean NET_DVR_SetReconnect(int dwInterval, boolean bEnableReconnect);

    // 登录V30
    NativeLong NET_DVR_Login_V30(String sDVRIP, short wDVRPort, String sUserName, String sPassword, NET_DVR_DEVICEINFO_V30 lpDeviceInfo);

    // 退出登录
    int NET_DVR_Logout(NativeLong lUserID);

    // 获取last错误
    int NET_DVR_GetLastError();

    // ========== 回放相关接口 ==========

    // 按时间回放V40
    NativeLong NET_DVR_PlayBackByTime_V40(NativeLong lUserID, NET_DVR_VOD_PARA lpVodPara);

    // 回放控制V40
    boolean NET_DVR_PlayBackControl_V40(NativeLong lPlayHandle, int dwControlCode, Pointer lpInBuffer, int dwInBufferLen, Pointer lpOutBuffer, IntByReference lpOutBufferLen);

    // 设置回放数据回调
    boolean NET_DVR_SetPlayDataCallBack(NativeLong lPlayHandle, FPlayDataCallBack fPlayDataCallBack, Pointer pUser);

    // 停止回放
    int NET_DVR_StopPlayBack(NativeLong lPlayHandle);

    // 获取回放进度
    boolean NET_DVR_PlayBackGetCube(NativeLong lPlayHandle, IntByReference pCube);
}