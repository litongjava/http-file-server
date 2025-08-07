package com.litongjava.http.file.server.handler;

import java.io.File;
import java.util.List;

import com.litongjava.http.file.server.model.FileMeta;
import com.litongjava.http.file.server.utils.HftpUtils;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.admin.services.AppUserService;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class FileHandler {

  public HttpResponse list(HttpRequest httpRequest) {
    String repo = httpRequest.getParam("repo");

    HttpResponse response = TioRequestContext.getResponse();
    String userId = httpRequest.getUserIdString();
    String username = Aop.get(AppUserService.class).getUsernameById(userId);
    File file = new File(username);
    if (!file.exists()) {
      RespBodyVo ok = RespBodyVo.ok();
      response.body(ok);
      return response;
    }

    file = new File(username, repo);
    if (!file.exists()) {
      RespBodyVo ok = RespBodyVo.ok();
      response.body(ok);
      return response;
    }

    List<FileMeta> list= HftpUtils.scanLocalFiles(file);
    RespBodyVo ok = RespBodyVo.ok(list);
    response.setJson(ok);
    return response;

  }
}
