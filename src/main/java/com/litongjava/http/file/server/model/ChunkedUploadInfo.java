package com.litongjava.http.file.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分片上传信息类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkedUploadInfo {
  private String uploadId;
  private String fileName;
  private Long fileSize;
  private Integer totalParts;
  private String repo;
  private String username;
  private Long originalModTime;
  private String tempDirPath;
  private boolean[] receivedParts;


}