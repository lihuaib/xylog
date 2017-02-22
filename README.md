# XYLog

自定义的 android 日志辅助文件

Android LogCat 工具类，目前功能：

- 1. 格式化打印 json 数据
- 2. 将日志存储在本地硬盘
- 3. 日志删除等功能
- 4. 包含普通日志常用的几个功能

---

XYLog 的使用
-----------------------
1. 在Application 的类的onCreate 去初始化
    
    ``` Java
    public void onCreate() {
        XYLog.init(isDebug, fileStorePath); // 如果是多个进程，可以考虑加变量去控制只初始化一次
    }

2. 调用
    XYLog.d(TAG, string) // 跟 Log.d(TAG, string) 的方法一样

