package com.litongjava.http.file.server.config;

import java.net.URL;
import java.util.List;

import com.litongjava.db.activerecord.Db;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbTables {

  public static void init() {
    String userTableName = "tio_boot_admin_system_users";

    boolean created = createTable(userTableName);
    if (created) {
      URL url = ResourceUtil.getResource("schema/tio_boot_admin_system_users_init.sql");
      String initSql = FileUtil.readString(url);
      int update = Db.update(initSql);
      log.info("已添加用户到 {}: {}", userTableName, update);
    }

    createTable("app_users");
  }

  private static boolean createTable(String userTableName) {
    String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
    List<String> tables = Db.queryListString(sql, userTableName);
    int size = tables.size();
    if (size < 1) {
      URL url = ResourceUtil.getResource("schema/" + userTableName + ".sql");
      String createSql = FileUtil.readString(url);
      int update = Db.update(createSql);
      log.info("已创建表 {}: {}", userTableName, update);
      return true;
    }
    return false;
  }
}
