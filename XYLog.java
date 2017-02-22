package xylog;

import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 日志功能目前用于 android:
 *  1. 格式化打印 json 数据
 *  2. 将日志存储在本地硬盘
 *  3. 日志删除等功能
 *  4. 包含普通日志常用的几个功能
 */

public class XYLog {

    /**
     * 该函数最好能在 Application.onCreate 中调用
     * @param isDebug
     * @param path
     */
    public static void init(boolean isDebug, String path) {
        Log.d("xylog", path);

        XYLog.debug = isDebug;
        XYLog.logPath = path;
    }

    private static boolean debug;

    private static final String FILE_FORMAT = "yyyyMMdd";
    private static final String MESSAGE_FORMAT = "MM-dd HH:mm:ss.ms";
    private static final String SUFFIX = "log";
    private static final String SEPARATOR = ".";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int JSON_INDENT = 4;
    private static String logPath;

    private static final DateFormat messageFormat = new SimpleDateFormat(MESSAGE_FORMAT, Locale.getDefault());
    private static final DateFormat fileNameFormat = new SimpleDateFormat(FILE_FORMAT, Locale.getDefault());
    private static final Executor logger = Executors.newSingleThreadExecutor();

    public static void e(String tag, String string) {
        printLog(Log.ERROR, tag, string);
    }

    public static void w(String tag, String string) {
        printLog(Log.WARN, tag, string);
    }

    public static void i(String tag, String string) {
        printLog(Log.INFO, tag, string);
    }

    public static void d(String tag, String string) {
        if (debug) printLog(Log.DEBUG, tag, string);
    }

    public static void v(String tag, String string) {
        if (debug) printLog(Log.VERBOSE, tag, string);
    }

    private static void printLog(int priority, String tag, String msg) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int index = 4;
        String className = stackTrace[index].getFileName();
        String methodName = stackTrace[index].getMethodName();
        int lineNumber = stackTrace[index].getLineNumber();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ (").append(className).append(":").append(lineNumber).append(")#").append(methodName).append(" ] ");
        String logStr = stringBuilder.toString();

        if (TextUtils.isEmpty(msg)) {
            printLine(priority, tag, "null string or zero length");
            return;
        }

        boolean isJson = false;
        try {
            if (msg.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(msg);
                msg = jsonObject.toString(JSON_INDENT);
                isJson = true;
            } else if (msg.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(msg);
                msg = jsonArray.toString(JSON_INDENT);
                isJson = true;
            }

            if (isJson) {
                printLine(priority, tag, true);
                msg = logStr + LINE_SEPARATOR + msg;
                String[] lines = msg.split(LINE_SEPARATOR);
                for (String line : lines) {
                    printLine(priority, tag, "║ " + line);
                }
                printLine(priority, tag, false);
                return;
            }
        } catch (JSONException e) {
            printLine(Log.ERROR, tag, e.getCause().getMessage() + "\n" + msg);
        } finally {
            if (!isJson) {
                printLine(priority, tag, logStr + msg);
            }
        }
    }

    private static void printLine(int priority, String tag, boolean isTop) {
        if (isTop) {
            printLine(priority, tag, "╔═══════════════════════════════════════════════════════════════════════════════════════");
        } else {
            printLine(priority, tag, "╚═══════════════════════════════════════════════════════════════════════════════════════");
        }
    }

    private static void printLine(int priority, String tag, String line) {
        Log.println(priority, tag, line);
        printFile(priority, tag, line);
    }

    private static void printFile(final int priority, final String tag, final String line) {
        if (priority < Log.INFO) return;

        final long time = System.currentTimeMillis();

        logger.execute(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(logPath)) {
                    return;
                }

                String timeStr = messageFormat.format(new Date(time));
                outputToFile(formatMessage(tag, timeStr, line), getLogFilePath());
            }
        });
    }

    private static String formatMessage(String tag, String time, String msg) {
        StringBuilder sb = new StringBuilder();

        // time
        sb.append(time);
        sb.append(": ");

        // process and thread
        sb.append(Process.myPid());
        sb.append("-");
        sb.append(Process.myTid());
        sb.append(": ");

        // tag
        sb.append(tag);
        sb.append(": ");

        // message
        sb.append(msg);
        sb.append("\n");

        return sb.toString();
    }

    private static boolean outputToFile(String message, String path) {
        if (TextUtils.isEmpty(message)) {
            return false;
        }

        if (TextUtils.isEmpty(path)) {
            return false;
        }

        boolean written = false;
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(path, true));
            fw.write(message);
            fw.flush();
            fw.close();

            written = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return written;
    }

    private static String getLogFilePath() {
        File dir = new File(logPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return logPath + "/" + getLogFileName();
    }

    /**
     * 日志名称可以根据不同的要求自己修改
     * @return
     */
    private static String getLogFileName() {
        StringBuilder sb = new StringBuilder();

        sb.append(fileNameFormat.format(new Date()));

        return sb.toString();
    }
}
