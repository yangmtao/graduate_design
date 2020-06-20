package xyz.ymtao.gd.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public class CommoditySpuImage implements Serializable {
    @Column
    @Id
    private String id;
    @Column
    private Integer commodityId;
    @Column
    private String imgName;
    @Column
    private String imgUrl;
    @Column
    private Integer imgType;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getCommodityId() {
        return commodityId;
    }

    public void setCommodityId(Integer commodityId) {
        this.commodityId = commodityId;
    }

    public String getImgName() {
        return imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public Integer getImgType() {
        return imgType;
    }

    public void setImgType(Integer imgType) {
        this.imgType = imgType;
    }
}
