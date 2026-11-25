package top.weixiansen574.hybridfilexfer.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile;

public class Utils {
    public static final String ILLEGAL_CHARACTERS = "<>:\"/\\|?*";

    public static void sortFiles(List<RemoteFile> fileList) {
        // 使用Comparator自定义比较规则
        fileList.sort((f1, f2) -> {
            // 文件夹优先于文件
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                // 如果类型相同，则按名称排序
                return f1.getName().compareTo(f2.getName());
            }
        });
    }

    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + "B/s";
        }
        if (bytesPerSecond < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2fKB/s", bytesPerSecond / 1024.0);
        }
        return String.format(Locale.getDefault(), "%.2fMB/s", bytesPerSecond / (1024.0 * 1024.0));
    }

    public static String formatDateTime(long milliseconds) {
        //格式化为年-月-日 时:分
        return new SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault()).format(new Date(milliseconds));
    }

    /**
     * 格式化毫秒数为指定的时间格式。
     *
     * @param milliseconds 输入的毫秒数
     * @return 格式化后的时间字符串
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("毫秒数不能为负数");
        }

        if (milliseconds < 1000) {
            return milliseconds + "ms";
        }

        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours == 0) {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        }
        if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2fKB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2fMB", size / (1024.0 * 1024.0));
        }
        return String.format(Locale.getDefault(), "%.2fGB", size / (1024.0 * 1024.0 * 1024.0));
    }

    public static boolean containsIllegalCharacters(String text) {
        for (char c : ILLEGAL_CHARACTERS.toCharArray()) {
            if (text.indexOf(c) >= 0) {
                return true; // 如果找到非法字符，返回 true
            }
        }
        return false; // 如果没有找到非法字符，返回 false
    }

    public static List<RemoteFile> listRemoteFiles(String path){
        File[] files = new File(path).listFiles();
        if (files == null) {
            return null;
        }
        ArrayList<RemoteFile> remoteFiles = new ArrayList<>(files.length);
        for (File file : files) {
            remoteFiles.add(new RemoteFile(file));
        }
        return remoteFiles;
    }

}
