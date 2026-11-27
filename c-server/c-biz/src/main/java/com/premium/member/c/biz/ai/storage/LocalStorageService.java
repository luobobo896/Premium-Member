package com.premium.member.c.biz.ai.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * dev 本地目录存储。
 */
public class LocalStorageService implements StorageService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final Path root;

    public LocalStorageService(String storageDir) {
        this.root = Path.of(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建 AI 存储目录: " + root, ex);
        }
    }

    @Override
    public StoredFile store(byte[] content, String extension) {
        String key = LocalDate.now().format(DATE_PATH) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + "." + extension.toLowerCase(Locale.ROOT);
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
        return new StoredFile(key);
    }

    @Override
    public Resource load(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException ignored) {
            // 文件已不存在或暂时无法删除时，数据库状态仍以 DELETED 为准。
        }
    }

    private Path resolve(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return path;
    }
}
