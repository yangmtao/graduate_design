package xyz.ymtao.gd.entity;

import java.awt.image.BufferedImage;
import java.io.Serializable;

public class ImageCodeInfo implements Serializable {
    private String codeStr;
    private byte[] imageBytes;

    public String getCodeStr() {
        return codeStr;
    }

    public void setCodeStr(String codeStr) {
        this.codeStr = codeStr;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }
}
