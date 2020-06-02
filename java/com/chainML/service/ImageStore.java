package com.chainML.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface ImageStore {
    String Save(String imageType, ByteArrayOutputStream imageData) throws IOException;
}
