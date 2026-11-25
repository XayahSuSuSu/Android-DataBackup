package top.weixiansen574.hybridfilexfer.jdkcore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VirtualPathResolver {
    private final Path baseDir;

    public VirtualPathResolver(Path baseDir) throws IOException {
        this.baseDir = baseDir.toAbsolutePath().normalize();
        if (!Files.exists(this.baseDir)) {
            Files.createDirectories(this.baseDir);
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public Path resolve(String virtualPath) throws IOException {
        if (virtualPath == null || virtualPath.isEmpty() || "/".equals(virtualPath)) {
            return baseDir;
        }
        String normalized = virtualPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return baseDir;
        }
        String[] parts = normalized.split("/");
        Path current = baseDir;
        for (String rawSeg : parts) {
            if (rawSeg.isEmpty()) continue;
            if (".".equals(rawSeg) || "..".equals(rawSeg)) {
                throw new SecurityException("Path traversal blocked: " + virtualPath);
            }
            String safeSeg = rawSeg.replaceAll("[<>:\"|?*]", "_");
            current = current.resolve(safeSeg);
        }
        Path resolved = current.toAbsolutePath().normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new SecurityException("Path traversal blocked: " + virtualPath);
        }
        return resolved;
    }

    public String toVirtualPath(Path realPath) {
        Path norm = realPath.toAbsolutePath().normalize();
        if (norm.equals(baseDir)) {
            return "/";
        }
        Path rel = baseDir.relativize(norm);
        List<String> segments = new ArrayList<>();
        for (Path p : rel) {
            segments.add(p.toString());
        }
        return "/" + String.join("/", segments);
    }

    public boolean isRoot(Path realPath) {
        return realPath.toAbsolutePath().normalize().equals(baseDir);
    }
}

