package com.graffitab.server.service.asset;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 27/06/2016.
 */
public class MultipartFileTransferableStream implements TransferableStream {

    private MultipartFile multipartFile;

    public MultipartFileTransferableStream(MultipartFile multipartFile) {
        this.multipartFile = multipartFile;
    }

    @Override
    public void transferTo(File destinationFile) throws IOException {
        multipartFile.transferTo(destinationFile);
    }
}
