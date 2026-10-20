package dev.jianwii.web.basicdata;

import dev.jianwii.api.basicdata.UploadFileApi;
import dev.jianwii.common.exception.ForbiddenException;
import dev.jianwii.common.web.Operator;
import dev.jianwii.common.web.Result;
import dev.jianwii.entity.basicdata.UploadFile;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用入口由 SecurityConfig 要求登入，且只允許存取自己的檔案。 業務模組若要共用檔案，應另經該模組的權限檢查後呼叫內部 UploadFileApi。
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/basic-data/upload-file")
public class WebUploadFileController {
    private final UploadFileApi uploadFileApi;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadFile> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(uploadFileApi.upload(file));
    }

    @GetMapping("/{id}")
    public Result<UploadFile> get(@PathVariable Long id) {
        return Result.success(requireOwner(id));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long id) {
        requireOwner(id);
        return uploadFileApi.content(id);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireOwner(id);
        uploadFileApi.delete(id);
        return Result.success();
    }

    private UploadFile requireOwner(Long id) {
        UploadFile file = uploadFileApi.get(id);
        if (file.isBusinessManaged()) {
            throw new ForbiddenException("請透過所屬功能存取檔案");
        }
        if (!Operator.current().equals(file.getCreateId())) {
            throw new ForbiddenException("只能存取自己上傳的檔案");
        }
        return file;
    }
}
