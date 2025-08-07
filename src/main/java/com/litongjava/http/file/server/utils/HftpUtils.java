package com.litongjava.http.file.server.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.http.file.server.model.FileMeta;

public class HftpUtils {

  public static List<FileMeta> scanLocalFiles(File repoDir) {
    List<FileMeta> fileMetas = new ArrayList<>();

    if (!repoDir.exists() || !repoDir.isDirectory()) {
      return fileMetas;
    }

    scanDirectory(repoDir, repoDir, fileMetas);
    return fileMetas;
  }

  private static void scanDirectory(File rootDir, File currentDir, List<FileMeta> fileMetas) {
    File[] files = currentDir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (file.isDirectory()) {
        // 递归处理子目录，但跳过 .hftp 目录
        if (!file.getName().equals(".hftp")) {
          scanDirectory(rootDir, file, fileMetas);
        }
      } else {
        // 处理文件
        try {
          String relativePath = getRelativePath(rootDir, file);
          String hash = calculateSHA256(file);
          long modTime = file.lastModified() / 1000; // 转换为秒级时间戳
          fileMetas.add(new FileMeta().setPath(relativePath).setHash(hash).setMod_time(modTime));
        } catch (Exception e) {
          // 记录错误但继续处理其他文件
          e.printStackTrace();
        }
      }
    }
  }

  private static String getRelativePath(File rootDir, File file) {
    String rootPath = rootDir.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    
    if (filePath.startsWith(rootPath)) {
      String relative = filePath.substring(rootPath.length());
      if (relative.startsWith(File.separator)) {
        relative = relative.substring(1);
      }
      return relative.replace(File.separator, "/");
    }
    
    return file.getName();
  }

  /**
   * 计算文件的 SHA256 哈希值
   */
  public static String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }
    }

    byte[] hashBytes = digest.digest();
    StringBuilder hexString = new StringBuilder();
    for (byte b : hashBytes) {
      String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }

    return hexString.toString();
  }
}