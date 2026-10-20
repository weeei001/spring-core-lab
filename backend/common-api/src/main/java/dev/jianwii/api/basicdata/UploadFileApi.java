package dev.jianwii.api.basicdata;

import dev.jianwii.entity.basicdata.UploadFile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * basic-data 的共用檔案 API；呼叫端用 @ImportHttpServices(group = "basic-data", types = UploadFileApi.class) 註冊。
 * 內部讀取不限制上傳者，供業務模組通過自己的存取檢查後取檔；不可直接對外公開。
 */
@HttpExchange("/api/basic-data/upload-file")
public interface UploadFileApi {

    @PostExchange(value = "/upload", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    UploadFile upload(@RequestPart("file") MultipartFile file);

    /** 業務模組專用上傳；通用前端入口不可讀取或刪除這類檔案。 */
    @PostExchange(value = "/upload-managed", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    UploadFile uploadManaged(@RequestPart("file") MultipartFile file);

    @GetExchange("/{id}")
    UploadFile get(@PathVariable("id") Long id);

    @GetExchange("/{id}/content")
    ResponseEntity<byte[]> content(@PathVariable("id") Long id);

    /** 只有上傳者能軟刪除；實體檔案保留。 */
    @DeleteExchange("/{id}")
    void delete(@PathVariable("id") Long id);
}
