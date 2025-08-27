package com.litongjava.http.file.server.handler;

import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;

public class WhoHandler {

  public HttpResponse who(HttpRequest httpRequest) {
    HttpResponse response = TioRequestContext.getResponse();
    response.body("http-file-server");
    return response;
  }
}
