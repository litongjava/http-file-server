package com.litongjava.http.file.server.handler;

import java.io.File;

import com.litongjava.http.file.server.consts.FileConst;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.admin.services.AppUserService;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class RepoHandler {
  public HttpResponse list(HttpRequest httpRequest) {
    HttpResponse response = TioRequestContext.getResponse();
    String userId = httpRequest.getUserIdString();
    String username = Aop.get(AppUserService.class).getUsernameById(userId);

    String userDir = FileConst.DATA_DIR + File.separator + username + File.separator;
    String[] list = new File(userDir).list();
    RespBodyVo respBoddVo = null;
    if (list != null) {
      respBoddVo = RespBodyVo.ok(list);
    } else {
      respBoddVo = RespBodyVo.ok(new String[] {});
    }
    return response.body(respBoddVo);

  }
}
