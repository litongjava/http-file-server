package com.litongjava.http.file.server;

import com.litongjava.http.file.server.config.HttpFileAppBootConfig;
import com.litongjava.tio.boot.TioApplication;

public class HttpFileServer {
  public static void main(String[] args) {
    HttpFileAppBootConfig httpFileAppBootConfig = new HttpFileAppBootConfig();
    TioApplication.run(HttpFileServer.class, httpFileAppBootConfig, args);
  }
}