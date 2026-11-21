package top.weixiansen574.hybridfilexfer.core.bean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

//声明：大部分代码由ChatGPT生成

public class Directory {
    public static final int FILE_SYSTEM_UNIX = 0;
    public static final int FILE_SYSTEM_WINDOWS = 1;
    public final String path;
    public final int fileSystem;

    public Directory(String path, int fileSystem) {
        this.fileSystem = fileSystem;
        this.path = normalizePath(path);
    }

    // 规范化路径：保证使用统一分隔符，并确保非根目录路径以分隔符结尾
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        if (path.equals("/")) return "/";
        // 判断当前系统使用的路径分隔符
        String separator = (fileSystem == FILE_SYSTEM_UNIX) ? "/" : "\\";

        // 只有当分隔符不等时才做替换
        if (fileSystem == FILE_SYSTEM_WINDOWS) {
            path = path.replace("/", separator);  // 将所有“/”替换为Windows分隔符
        }

        // 对于Windows盘符，如果路径形如 "C:" 则补上分隔符
        if (fileSystem == FILE_SYSTEM_WINDOWS && path.matches("^[A-Za-z]:$")) {
            path += separator;
        }

        // 保证非根目录的路径以分隔符结尾
        if (!path.equals(separator) && !path.endsWith(separator)) {
            path += separator;
        }

        return path;
    }

    public Directory parent() {
        if (fileSystem == FILE_SYSTEM_UNIX) {
            if ("/".equals(path)) return null; // UNIX根目录无上级
            String trimmed = path.substring(0, path.length() - 1); // 去掉末尾的"/"
            int idx = trimmed.lastIndexOf("/");
            String parentPath = (idx <= 0) ? "/" : trimmed.substring(0, idx + 1);
            return new Directory(parentPath, fileSystem);
        } else {
            if ("/".equals(path)) return null;
            String norm = path.substring(0, path.length() - 1); // 去掉末尾的"\"或"/"
            int idx = norm.lastIndexOf("\\");
            if (idx <= 2) return new Directory("/", fileSystem); // 盘符目录 "C:\" 的上级为"/"
            return new Directory(norm.substring(0, idx + 1), fileSystem);
        }
    }

    public Directory append(String child) {
        if (child == null || child.isEmpty()) return this;
        // 去除child前面的分隔符，防止重复
        while (child.startsWith("/") || child.startsWith("\\")) {
            child = child.substring(1);
        }
        return new Directory(path + child, fileSystem);
    }

    /*
    | 本地系统   | 远程系统  | 文件绝对路径                    | 本地文件夹   | 远程文件夹   | 结果                            |
    | -------- | -------- | ----------------------------- | ---------- | ---------- | ------------------------------- |
    | Linux    | Windows  | /sdcard/1.jpg                 | /sdcard    | C:\\pics   | C:\\pics\\1.jpg                 |
    | Linux    | Windows  | /sdcard/\\.jpg                | /sdcard    | C:\\pics   | C:\\pics\\_.jpg                 |
    | Linux    | Windows  | /sdcard/2025-2-8 17:10:25.jpg | /sdcard    | C:\\pics   | C:\\pics\\2025-2-8 17_10_25.jpg |
    | Linux    | Linux    | /sdcard/2025-2-8 17:10:25.jpg | /sdcard    | /mnt/ssd   | /mnt/ssd/2025-2-8 17_10_25.jpg  |
    | Windows  | Linux    | C:\\pics\\1.jpg               | C:\\pics   | /sdcard    | /sdcard/1.jpg                   |

    由于Linux文件系统(Ext)的独特性，例如 : * ? " < > | 甚至 \ 的特殊符号都是允许的，最常见的就是冒号时间例如“2025-2-8 17:20:09”的文件名出现。
    故在传输给对方时需要替换成下划线_。
    当然，Linux也可以挂载ExFat的硬盘，这些字符在此文件系统不被允许，为保兼容，即使是Linux传Linux还是要替换成下划线。
     */
    /**
     * 根据本地文件夹（当前对象）和输入的本地文件绝对路径，生成传输到远程系统后的文件绝对路径。
     * <p>
     * 实现思路：
     * <ol>
     *   <li>根据本地系统类型确定本地分隔符，远程系统确定远程分隔符。</li>
     *   <li>计算文件相对于本地文件夹（this.path）的相对路径。</li>
     *   <li>将相对路径按本地分隔符拆分成各个段，然后对每个段中的非法字符（[\\ : * ? " < > |]）替换为下划线 "_"。</li>
     *   <li>最后将远程文件夹（remote.path）与替换后的相对路径段用远程分隔符拼接生成结果。</li>
     * </ol>
     *
     * <p>例如：
     * <br>Linux -> Windows: file "/sdcard/2025-2-8 17:10:25.jpg"（相对于 "/sdcard"）生成 "C:\pics\2025-2-8 17_10_25.jpg"
     * <br>Windows -> Linux: file "C:\pics\1.jpg"（相对于 "C:\pics"）生成 "/sdcard/1.jpg"
     * </p>
     *
     * @param file   本地文件的绝对路径
     * @param remote 远程文件夹目录（目标系统）
     * @return 生成的远程文件绝对路径
     */
    public String generateTransferPath(String file, Directory remote) {
        // 根据本地系统类型确定分隔符
        String localSep = (this.fileSystem == FILE_SYSTEM_UNIX) ? "/" : "\\";
        String remoteSep = (remote.fileSystem == FILE_SYSTEM_UNIX) ? "/" : "\\";

        // 规范化输入文件路径（注意：Linux允许 "\" 为普通字符，不替换）
        String normalizedFile;
        if (this.fileSystem == FILE_SYSTEM_UNIX) {
            normalizedFile = file;
        } else {
            normalizedFile = file.replace("/", localSep);
        }

        // 计算相对于本地目录的相对路径
        String localFolder = this.path; // 已经规范化且以localSep结尾
        String relativePath;
        if (normalizedFile.startsWith(localFolder)) {
            relativePath = normalizedFile.substring(localFolder.length());
        } else {
            // 如果 file 不以本地目录开头，则去掉前导分隔符（如果有）
            if (normalizedFile.startsWith(localSep)) {
                relativePath = normalizedFile.substring(1);
            } else {
                relativePath = normalizedFile;
            }
        }

        // 将相对路径按本地分隔符拆分成各个段（注意Linux下只用 "/" 拆分）
        String[] segments = relativePath.split(Pattern.quote(localSep));
        List<String> sanitizedSegments = new ArrayList<>();
        // 对每个段替换非法字符：\ : * ? " < > |
        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            String sanitized = seg.replaceAll("[\\\\:*?\"<>|]", "_");
            sanitizedSegments.add(sanitized);
        }
        // 用远程系统的分隔符拼接
        String sanitizedRelative = String.join(remoteSep, sanitizedSegments);

        // 组合远程文件夹与经过非法字符替换后的相对路径，注意 remote.path 已规范化（结尾包含分隔符）
        if (sanitizedRelative.isEmpty()) {
            return remote.path;
        } else {
            return remote.path + sanitizedRelative;
        }
    }

    public static int getCurrentFileSystem(){
        if (File.separator.equals("\\")){
            return FILE_SYSTEM_WINDOWS;
        } else {
            return FILE_SYSTEM_UNIX;
        }
    }

    @Override
    public String toString() {
        return "Directory{" +
                "path='" + path + '\'' +
                ", fileSystem=" + (fileSystem == FILE_SYSTEM_UNIX ? "UNIX" : "WINDOWS") +
                '}';
    }

}
