package com.litongjava.http.file.server.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.List;

import com.litongjava.http.file.server.consts.FileConst;
import com.litongjava.http.file.server.model.FileMeta;
import com.litongjava.http.file.server.utils.HftpUtils;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.admin.services.AppUserService;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.utils.HttpFileDataUtils;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.UploadFile;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.utils.base64.Base64Utils;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.StrUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileHandler {

  public HttpResponse list(HttpRequest httpRequest) {
    String repo = httpRequest.getParam("repo");

    HttpResponse response = TioRequestContext.getResponse();
    String userId = httpRequest.getUserIdString();
    String username = Aop.get(AppUserService.class).getUsernameById(userId);

    String repoDir = FileConst.DATA_DIR + File.separator + username + File.separator + repo;
    File file = new File(repoDir);
    if (!file.exists()) {
      RespBodyVo ok = RespBodyVo.ok(new String[] {});
      response.body(ok);
      return response;
    }

    List<FileMeta> list = HftpUtils.scanLocalFiles(file);
    RespBodyVo ok = RespBodyVo.ok(list);
    response.setJson(ok);
    return response;
  }

  public HttpResponse upload(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();
    String repo = request.getParam("repo");
    String userIdString = request.getUserIdString();
    String username = Aop.get(AppUserService.class).getUsernameById(userIdString);

    String repoDirPath = FileConst.DATA_DIR + File.separator + username + File.separator + repo;
    File repoDir = new File(repoDirPath);
    if (!repoDir.exists()) {
      repoDir.mkdirs();
    }
    UploadFile uploadFile = request.getUploadFile("file");
    Long original_mod_time = request.getLong("original_mod_time");

    String name = uploadFile.getName();
    if (StrUtil.isBlank(name)) {
      String encodedPath = request.getParam("original_path_encoded");
      if (encodedPath != null) {
        name = Base64Utils.decodeToString(encodedPath);
      }
    }
    // windows
    name = name.replace("\\\\", "/");
    File file = new File(repoDirPath + File.separator + name);
    File parentFile = file.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
    try {
      Files.write(file.toPath(), uploadFile.getData());
      if (original_mod_time != null && original_mod_time > 0) {
        // 将 Unix 时间戳 (秒) 转换为 FileTime
        FileTime fileTime = FileTime.fromMillis(original_mod_time * 1000);
        // 设置文件的最后修改时间
        Files.setLastModifiedTime(file.toPath(), fileTime);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return response.error(e.getMessage());
    }
    return response;

  }

  public HttpResponse download(HttpRequest request) {
    HttpResponse response = TioRequestContext.getResponse();

    String repo = request.getParam("repo");
    String filePath = request.getParam("file");
    String userIdString = request.getUserIdString();
    String username = Aop.get(AppUserService.class).getUsernameById(userIdString);

    String repoDirPath = FileConst.DATA_DIR + File.separator + username + File.separator + repo;

    CORSUtils.enableCORS(response);

    File file = new File(repoDirPath + File.separator + filePath);
    String suffix = FilenameUtils.getSuffix(filePath);
    String contentType = ContentTypeUtils.getContentType(suffix);

    if (!file.exists()) {
      response.setStatus(404);
      return response;
    }

    long fileLength = file.length();
    long lastModified = file.lastModified();

    // 生成 ETag
    String etag = HttpFileDataUtils.generateETag(file, lastModified, fileLength);

    // 设置缓存相关头部
    HttpFileDataUtils.setCacheHeaders(response, lastModified, etag, contentType, suffix);

    // 检查客户端缓存
    if (HttpFileDataUtils.isClientCacheValid(request, lastModified, etag)) {
      response.setStatus(304); // Not Modified
      return response;
    }

    // 检查是否存在 Range 头信息
    String range = request.getHeader("range");
    if (range != null && range.startsWith("bytes=")) {
      return HttpFileDataUtils.handleRangeRequest(response, file, range, fileLength, contentType);
    } else {
      return HttpFileDataUtils.handleFullFileRequest(response, file, fileLength, contentType);
    }
  }
}
