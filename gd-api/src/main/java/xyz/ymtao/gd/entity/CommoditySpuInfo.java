package xyz.ymtao.gd.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

public class CommoditySpuInfo implements Serializable {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String productName;

    @Column
    private String description;

    @Column
    private  String catalog3Id;

    @Column
    private String brandId;

    @Transient
    private List<CommoditySpuSaleAttribute> spuSaleAttrList;

    @Transient
    private List<CommoditySpuImage> spuImageList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCatalog3Id() {
        return catalog3Id;
    }

    public void setCatalog3Id(String catalog3Id) {
        this.catalog3Id = catalog3Id;
    }

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public List<CommoditySpuSaleAttribute> getSpuSaleAttrList() {
        return spuSaleAttrList;
    }

    public void setSpuSaleAttrList(List<CommoditySpuSaleAttribute> spuSaleAttrList) {
        this.spuSaleAttrList = spuSaleAttrList;
    }

    public List<CommoditySpuImage> getSpuImageList() {
        return spuImageList;
    }

    public void setSpuImageList(List<CommoditySpuImage> spuImageList) {
        this.spuImageList = spuImageList;
    }
}
