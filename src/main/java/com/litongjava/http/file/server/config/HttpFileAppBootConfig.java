package com.litongjava.http.file.server.config;

import com.litongjava.context.BootConfiguration;

public class HttpFileAppBootConfig implements BootConfiguration{

  @Override
  public void config() throws Exception {
    new AdminAppConfig().config();
  }

}
