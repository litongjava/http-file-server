package com.litongjava.http.file.server.config;

import com.litongjava.http.file.server.handler.FileChunkedUploadHandler;
import com.litongjava.http.file.server.handler.FileHandler;
import com.litongjava.http.file.server.handler.RepoHandler;
import com.litongjava.http.file.server.handler.WhoHandler;
import com.litongjava.tio.boot.admin.config.TioAdminDbConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminHandlerConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminInterceptorConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminMongoDbConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminRedisDbConfiguration;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;

public class AdminAppConfig {

  public void config() {
    String[] permitUrls = { "/who" };
    // 配置数据库相关
    new TioAdminDbConfiguration().config();
    new TioAdminRedisDbConfiguration().config();
    new TioAdminMongoDbConfiguration().config();
    new TioAdminInterceptorConfiguration(permitUrls).config();
    new TioAdminHandlerConfiguration().config();
    DbTables.init();

    // 获取 HTTP 请求路由器
    TioBootServer server = TioBootServer.me();
    HttpRequestRouter r = server.getRequestRouter();
    if (r != null) {
      FileHandler fileHandler = new FileHandler();
      r.add("/file/list", fileHandler::list);
      r.add("/file/upload", fileHandler::upload);
      r.add("/file/download", fileHandler::download);

      RepoHandler repoHandler = new RepoHandler();
      r.add("/repo/list", repoHandler::list);

      FileChunkedUploadHandler chunkedFileHandler = new FileChunkedUploadHandler();
      // 分片上传相关路由
      r.add("/file/upload/init", chunkedFileHandler::initUpload);
      r.add("/file/upload/chunk", chunkedFileHandler::uploadChunk);
      r.add("/file/upload/complete", chunkedFileHandler::completeUpload);

      WhoHandler whoHandler = new WhoHandler();
      r.add("/who", whoHandler::who);
    }
//    new TioAdminControllerConfiguration().config();
  }
}
