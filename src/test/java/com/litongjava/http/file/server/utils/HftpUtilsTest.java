package com.litongjava.http.file.server.utils;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.litongjava.http.file.server.model.FileMeta;

public class HftpUtilsTest {

  @Test
  public void scanLocalFiles() {
    File file = new File("F:\\my_document");
    long start = System.currentTimeMillis();
    List<FileMeta> fileMetaList = HftpUtils.scanLocalFiles(file);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "(ms)");
    
  }

}
