package com.graffitab.server.service.asset;

import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by david on 27/06/2016.
 */
public class InputStreamTransferableStream implements TransferableStream {
    private InputStream assetInputStream;

    public InputStreamTransferableStream(InputStream assetInputStream) {
        this.assetInputStream = assetInputStream;
    }

    @Override
    public void transferTo(File destinationFile) throws IOException {
        BufferedOutputStream destination = new BufferedOutputStream(new FileOutputStream(destinationFile));
        IOUtils.copy(assetInputStream, destination);
    }
}
