package com.litongjava.http.file.server.config;

import com.litongjava.tio.boot.admin.config.TioAdminDbConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminHandlerConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminInterceptorConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminMongoDbConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminRedisDbConfiguration;

public class AdminAppConfig {

  public void config() {
    // 配置数据库相关
    new TioAdminDbConfiguration().config();
    new TioAdminRedisDbConfiguration().config();
    new TioAdminMongoDbConfiguration().config();
    new TioAdminInterceptorConfiguration().config();
    new TioAdminHandlerConfiguration().config();
    DbTables.init();

    // 获取 HTTP 请求路由器
//    TioBootServer server = TioBootServer.me();
//    HttpRequestRouter r = server.getRequestRouter();
//    if (r != null) {
//      SystemFileTencentCosHandler systemUploadHandler = Aop.get(SystemFileTencentCosHandler.class);
//      r.add("/api/system/file/upload", systemUploadHandler::upload);
//      r.add("/api/system/file/url", systemUploadHandler::getUrl);
//    }
//    new TioAdminControllerConfiguration().config();
  }
}
