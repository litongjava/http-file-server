# http-file-server

`http-file-server` 是一个基于 [Tio Boot](https://gitee.com/litongjava/tio-boot) 框架构建的轻量级、高性能 HTTP 文件服务器。它旨在提供简单易用的文件上传、下载和管理功能，特别支持大文件的分片上传，以增强网络不稳定环境下的上传可靠性。

## 功能特性

*   **文件上传**：
    *   支持标准 HTTP 表单文件上传。
    *   **支持大文件分片上传**：将大文件切分为小块进行上传，有效应对网络中断等问题。
*   **文件下载**：
    *   支持标准 HTTP 文件下载。
    *   **支持断点续传**：通过 `Range` 请求头实现，客户端可从中断处继续下载。
    *   **HTTP 缓存支持**：利用 `Last-Modified` 和 `ETag` 头部，减少不必要的数据传输。
*   **文件管理**：
    *   列出用户仓库下的文件列表。
    *   列出用户拥有的所有仓库（目录）。
*   **用户隔离**：基于用户身份（通过 `Authorization` 头传递的 Token 解析）实现文件存储隔离。
*   **时间戳保留**：上传文件时可携带原始修改时间，服务器将尝试保留该时间戳。

## 技术栈

*   **核心框架**: [Tio Boot](https://gitee.com/litongjava/tio-boot) 
*   **语言**: Java
*   **构建工具**: Maven
*   **依赖注入**: JFinal AOP
*   **JSON 处理**: Fastjson2
*   **日志**: SLF4J

## API 接口

### 1. 仓库管理

*   **列出用户仓库**
    *   **URL**: `GET /repo/list`
    *   **描述**: 获取当前用户拥有的所有仓库（目录）列表。
    *   **响应**: `application/json`
        ```json
        {
          "ok": true,
          "data": ["repo1", "repo2", "my_files"]
        }
        ```

### 2. 文件管理

*   **列出仓库文件**
    *   **URL**: `GET /file/list?repo={repo_name}`
    *   **描述**: 获取指定仓库下的文件列表及其元信息。
    *   **参数**:
        *   `repo` (query): 仓库名称。
    *   **响应**: `application/json`
        ```json
        {
          "ok": true,
          "data": [
            {
              "name": "document.pdf",
              "size": 1048576,
              "lastModified": 1700000000000
            },
            {
              "name": "image.jpg",
              "size": 512000,
              "lastModified": 1700000050000
            }
          ]
        }
        ```

### 3. 文件上传

#### 标准上传

*   **上传单个文件**
    *   **URL**: `POST /file/upload?repo={repo_name}`
    *   **描述**: 通过标准 HTTP POST 表单上传单个文件到指定仓库。
    *   **参数**:
        *   `repo` (query): 目标仓库名称。
        *   `file` (form-data): 要上传的文件。
        *   `original_mod_time` (form-data, optional): 文件的原始 Unix 时间戳（秒），用于设置服务器上的文件修改时间。
    *   **请求体**: `multipart/form-data`
    *   **响应**: `application/json` (成功时通常返回 200 OK，无具体数据体)

#### 分片上传

分片上传分为三个步骤：

1.  **初始化上传**
    *   **URL**: `POST /file/upload/init?repo={repo_name}`
    *   **描述**: 启动一个新的分片上传任务，获取唯一的 `upload_id`。
    *   **参数**:
        *   `repo` (query): 目标仓库名称。
    *   **请求体**: `application/json`
        ```json
        {
          "repo": "my_repo",
          "file_name": "large_video.mp4",
          "file_size": 1073741824,
          "total_parts": 100,
          "original_mod_time": 1700000000
        }
        ```
    *   **响应**: `application/json`
        ```json
        {
          "ok": true,
          "data": {
            "upload_id": "abc123def456..."
          }
        }
        ```

2.  **上传分片**
    *   **URL**: `POST /file/upload/chunk?repo={repo_name}`
    *   **描述**: 上传一个文件分片。
    *   **参数**:
        *   `repo` (query): 目标仓库名称。
    *   **请求体**: `multipart/form-data`
        *   `file`: 分片的二进制数据。
        *   `upload_id`: 从初始化步骤获取的 `upload_id`。
        *   `part_index`: 当前分片的索引（从 0 开始）。
    *   **响应**: `application/json`
        ```json
        {
          "ok": true,
          "data": {
            "part_index": 5,
            "upload_id": "abc123def456..."
          }
        }
        ```

3.  **完成上传**
    *   **URL**: `POST /file/upload/complete?repo={repo_name}`
    *   **描述**: 通知服务器所有分片已上传，请求合并分片为完整文件。
    *   **参数**:
        *   `repo` (query): 目标仓库名称。
    *   **请求体**: `application/json`
        ```json
        {
          "upload_id": "abc123def456..."
        }
        ```
    *   **响应**: `application/json`
        ```json
        {
          "ok": true,
          "data": {
            "file_path": "/path/to/data/username/my_repo/large_video.mp4",
            "file_name": "large_video.mp4"
          }
        }
        ```

### 4. 文件下载

*   **下载文件**
    *   **URL**: `GET /file/download?repo={repo_name}&file={file_path}`
    *   **描述**: 从指定仓库下载文件。支持断点续传和 HTTP 缓存。
    *   **参数**:
        *   `repo` (query): 仓库名称。
        *   `file` (query): 要下载的文件路径（相对于仓库根目录）。
    *   **请求头** (可选):
        *   `Range`: 用于断点续传，例如 `Range: bytes=1000-2000`。
        *   `If-None-Match`: 用于缓存验证，值为之前响应中的 `ETag`。
        *   `If-Modified-Since`: 用于缓存验证，值为之前响应中的 `Last-Modified`。
    *   **响应**:
        *   `200 OK`: 完整文件内容。
        *   `206 Partial Content`: 断点续传返回的部分内容。
        *   `304 Not Modified`: 文件未修改，使用缓存。
        *   `404 Not Found`: 文件不存在。
    *   **响应头**:
        *   `Content-Type`: 文件的 MIME 类型。
        *   `Content-Length`: 文件或部分内容的大小。
        *   `Content-Range`: (206 响应) 指明返回的字节范围。
        *   `Last-Modified`: 文件最后修改时间。
        *   `ETag`: 文件内容的唯一标识符。

## 快速开始

### 1. 环境准备

*   JDK 8 或更高版本
*   Maven 3.x

### 2. 构建项目

```bash
git clone https://github.com/litongjava/http-file-server.git
cd http-file-server
mvn clean package -DskipTests -Pproduction
```

### 3. 运行

```bash
# 假设打包后生成了 fat jar
java -jar target/http-file-server-1.0.0.jar --server.port=10050
```
*   服务器默认启动在 `http://localhost:10050` (具体端口取决于你的 `tio-boot` 配置)。

### 4. 配置

*   项目配置通常位于 `src/main/resources/` 目录下，`app.properties`。
*   关键配置项（如端口、数据存储目录 `FileConst.DATA_DIR`）可在配置文件中定义。

### 5.使用supers启动
创建服务描述目录：

sudo mkdir -p /etc/super
在 /etc/super 下添加你的 .service 文件。例如：

```
vi /etc/super/http-file-server.service
```

```
[Unit]
Description=http-file-server Java Web Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/data/apps/http-file-server
ExecStart=/usr/java/jdk1.8.0_211/bin/java -jar target/http-file-server-1.0.0.jar --server.port=10050
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=default.target
```

### 配置nginx

```
server {
  listen 80;
  server_name hfile.litong.xyz;

  location / {
    proxy_pass http://127.0.0.1:10050;
    proxy_buffering off;
    proxy_pass_header Set-Cookie;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    error_log  /var/log/nginx/backend.error.log;
    access_log  /var/log/nginx/backend.access.log;
  }
}

```

## 客户端示例

项目中包含了 Golang 客户端示例 (`client.UploadInChunks`) 实现分片上传逻辑，可作为参考。

## 注意事项

*   **存储**: 文件默认存储在 `FileConst.DATA_DIR` 指定的目录下，结构为 `DATA_DIR/username/repo_name/file_name`。
*   **分片上传状态**: 当前服务端实现使用内存 (`ConcurrentHashMap`) 存储分片上传的中间状态 (`uploadMap`)。**在生产环境中，强烈建议替换为 Redis 或数据库等持久化存储，以防止服务重启导致上传任务丢失。**
*   **用户认证**: 服务端通过 `request.getUserIdString()` 和 `AppUserService` 获取用户名，你需要确保 `tio-boot` 的认证机制已正确配置，能够从 `Authorization` 头解析出用户 ID。
*   **安全性**: 确保部署环境下的安全性，例如验证文件类型、限制上传大小、保护 API 端点等。