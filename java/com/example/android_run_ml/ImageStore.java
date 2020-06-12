package com.chainvideoandroid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ImageStore {
    String Save(String imageType, ByteArrayOutputStream imageData) throws IOException;
}
