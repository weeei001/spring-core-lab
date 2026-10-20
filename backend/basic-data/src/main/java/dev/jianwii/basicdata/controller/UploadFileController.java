package dev.jianwii.basicdata.controller;

import dev.jianwii.api.basicdata.UploadFileApi;
import dev.jianwii.basicdata.service.UploadFileService;
import dev.jianwii.entity.basicdata.UploadFile;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class UploadFileController implements UploadFileApi {
    private final UploadFileService uploadFileService;

    @Override
    public UploadFile upload(MultipartFile file) {
        return uploadFileService.upload(file, false);
    }

    @Override
    public UploadFile uploadManaged(MultipartFile file) {
        return uploadFileService.upload(file, true);
    }

    @Override
    public UploadFile get(Long id) {
        return uploadFileService.get(id);
    }

    @Override
    public ResponseEntity<byte[]> content(Long id) {
        return uploadFileService.content(id);
    }

    @Override
    public void delete(Long id) {
        uploadFileService.delete(id);
    }
}
