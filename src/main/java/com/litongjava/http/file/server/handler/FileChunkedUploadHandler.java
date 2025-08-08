package com.litongjava.http.file.server.handler;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson2.JSONObject;
import com.litongjava.http.file.server.consts.FileConst;
import com.litongjava.http.file.server.model.ChunkedUploadInfo;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.admin.services.AppUserService;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.UploadFile;
import com.litongjava.tio.utils.json.FastJson2Utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileChunkedUploadHandler {

  // 临时存储上传信息，实际生产环境建议使用Redis等持久化存储
  private static final Map<String, ChunkedUploadInfo> uploadMap = new ConcurrentHashMap<>();

  /**
   * 初始化分片上传
   */
  public HttpResponse initUpload(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    String bodyString = request.getBodyString();
    JSONObject jsonObject = FastJson2Utils.parseObject(bodyString);
    try {
      String repo = jsonObject.getString("repo");
      String fileName = jsonObject.getString("file_name");
      Long fileSize = jsonObject.getLong("file_size");
      Integer totalParts = jsonObject.getInteger("total_parts");
      Long originalModTime = jsonObject.getLong("original_mod_time");

      if (repo == null || fileName == null || fileSize == null || totalParts == null) {
        return response.setJson(RespBodyVo.fail("Missing required parameters"));
      }

      String userIdString = request.getUserIdString();
      String username = Aop.get(AppUserService.class).getUsernameById(userIdString);

      // 生成唯一的上传ID
      String uploadId = UUID.randomUUID().toString().replace("-", "");

      // 创建临时目录
      String tempDirPath = FileConst.DATA_DIR + File.separator + username + File.separator + ".temp";
      File tempDir = new File(tempDirPath);
      if (!tempDir.exists()) {
        tempDir.mkdirs();
      }

      // 保存上传信息
      ChunkedUploadInfo uploadInfo = new ChunkedUploadInfo();
      uploadInfo.setUploadId(uploadId);
      uploadInfo.setFileName(fileName);
      uploadInfo.setFileSize(fileSize);
      uploadInfo.setTotalParts(totalParts);
      uploadInfo.setRepo(repo);
      uploadInfo.setUsername(username);
      uploadInfo.setOriginalModTime(originalModTime);
      uploadInfo.setTempDirPath(tempDirPath);
      uploadInfo.setReceivedParts(new boolean[totalParts]);

      uploadMap.put(uploadId, uploadInfo);

      Map<String, Object> data = new HashMap<>();
      data.put("upload_id", uploadId);

      return response.setJson(RespBodyVo.ok(data));

    } catch (Exception e) {
      log.error("Init chunked upload failed", e);
      return response.setJson(RespBodyVo.fail("Init chunked upload failed: " + e.getMessage()));
    }
  }

  /**
   * 上传分片
   */
  public HttpResponse uploadChunk(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();

    try {
      String uploadId = request.getParam("upload_id");
      Integer partIndex = request.getInt("part_index");

      if (uploadId == null || partIndex == null) {
        return response.setJson(RespBodyVo.fail("Missing required parameters"));
      }

      // 获取上传信息
      ChunkedUploadInfo uploadInfo = uploadMap.get(uploadId);
      if (uploadInfo == null) {
        return response.setJson(RespBodyVo.fail("Invalid upload_id"));
      }

      // 检查分片索引是否有效
      if (partIndex < 0 || partIndex >= uploadInfo.getTotalParts()) {
        return response.setJson(RespBodyVo.fail("Invalid part_index"));
      }

      // 获取上传的分片文件
      UploadFile uploadFile = request.getUploadFile("file");
      if (uploadFile == null) {
        return response.setJson(RespBodyVo.fail("Missing file data"));
      }

      // 保存分片到临时文件
      String chunkFileName = uploadId + "_part_" + partIndex;
      File chunkFile = new File(uploadInfo.getTempDirPath() + File.separator + chunkFileName);

      Files.write(chunkFile.toPath(), uploadFile.getData());

      // 标记该分片已接收
      uploadInfo.getReceivedParts()[partIndex] = true;

      Map<String, Object> data = new HashMap<>();
      data.put("part_index", partIndex);
      data.put("upload_id", uploadId);

      return response.setJson(RespBodyVo.ok(data));

    } catch (Exception e) {
      log.error("Upload chunk failed", e);
      return response.setJson(RespBodyVo.fail("Upload chunk failed: " + e.getMessage()));
    }
  }

  /**
   * 完成分片上传
   */
  public HttpResponse completeUpload(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    String bodyString = request.getBodyString();
    JSONObject jsonObject = FastJson2Utils.parseObject(bodyString);

    try {
      String uploadId = jsonObject.getString("upload_id");

      if (uploadId == null) {
        return response.setJson(RespBodyVo.fail("Missing upload_id"));
      }

      // 获取上传信息
      ChunkedUploadInfo uploadInfo = uploadMap.get(uploadId);
      if (uploadInfo == null) {
        return response.setJson(RespBodyVo.fail("Invalid upload_id"));
      }

      // 检查所有分片是否都已上传
      boolean allReceived = true;
      for (boolean received : uploadInfo.getReceivedParts()) {
        if (!received) {
          allReceived = false;
          break;
        }
      }

      if (!allReceived) {
        return response.setJson(RespBodyVo.fail("Not all chunks have been uploaded"));
      }

      // 合并所有分片
      String finalFilePath = FileConst.DATA_DIR + File.separator + uploadInfo.getUsername() + File.separator
          + uploadInfo.getRepo() + File.separator + uploadInfo.getFileName();

      File finalFile = new File(finalFilePath);
      File parentFile = finalFile.getParentFile();
      if (!parentFile.exists()) {
        parentFile.mkdirs();
      }

      // 使用RandomAccessFile进行文件合并
      try (RandomAccessFile raf = new RandomAccessFile(finalFile, "rw")) {
        for (int i = 0; i < uploadInfo.getTotalParts(); i++) {
          String chunkFileName = uploadId + "_part_" + i;
          File chunkFile = new File(uploadInfo.getTempDirPath() + File.separator + chunkFileName);

          if (chunkFile.exists()) {
            byte[] chunkData = Files.readAllBytes(chunkFile.toPath());
            raf.write(chunkData);
            // 删除临时分片文件
            chunkFile.delete();
          }
        }
      }

      // 设置文件的最后修改时间
      if (uploadInfo.getOriginalModTime() != null && uploadInfo.getOriginalModTime() > 0) {
        FileTime fileTime = FileTime.fromMillis(uploadInfo.getOriginalModTime() * 1000);
        Files.setLastModifiedTime(finalFile.toPath(), fileTime);
      }

      // 清理上传信息
      uploadMap.remove(uploadId);

      // 删除临时目录（如果为空）
      File tempDir = new File(uploadInfo.getTempDirPath());
      if (tempDir.exists() && tempDir.list().length == 0) {
        tempDir.delete();
      }

      Map<String, Object> data = new HashMap<>();
      data.put("file_path", finalFilePath);
      data.put("file_name", uploadInfo.getFileName());

      return response.setJson(RespBodyVo.ok(data));

    } catch (Exception e) {
      log.error("Complete chunked upload failed", e);
      return response.setJson(RespBodyVo.fail("Complete chunked upload failed: " + e.getMessage()));
    }
  }

}