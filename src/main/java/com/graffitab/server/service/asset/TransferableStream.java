package com.graffitab.server.service.asset;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 27/06/2016.
 */
public interface TransferableStream {
    void transferTo(File file) throws IOException;
}
