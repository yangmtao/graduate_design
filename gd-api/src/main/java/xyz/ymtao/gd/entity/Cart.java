package xyz.ymtao.gd.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;

public class Cart implements Serializable {
    @Id
    private String id;
    @Column
    private String commodityId;
    @Column
    private String commoditySkuId;
    @Column
    private String userId;
    @Column
    private BigDecimal quantity;
    // 商品价格
    @Column
    private BigDecimal price;
    @Column
    private String commodityPicture;
    @Column
    private String commodityName;
    @Column
    private String commoditySaleAttr;
    @Column
    private String isChecked;

    @Transient
    private BigDecimal totalPrice;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCommodityId() {
        return commodityId;
    }

    public void setCommodityId(String commodityId) {
        this.commodityId = commodityId;
    }

    public String getCommoditySkuId() {
        return commoditySkuId;
    }

    public void setCommoditySkuId(String commoditySkuId) {
        this.commoditySkuId = commoditySkuId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCommodityPicture() {
        return commodityPicture;
    }

    public void setCommodityPicture(String commodityPicture) {
        this.commodityPicture = commodityPicture;
    }

    public String getCommodityName() {
        return commodityName;
    }

    public void setCommodityName(String commodityName) {
        this.commodityName = commodityName;
    }

    public String getCommoditySaleAttr() {
        return commoditySaleAttr;
    }

    public void setCommoditySaleAttr(String commoditySaleAttr) {
        this.commoditySaleAttr = commoditySaleAttr;
    }

    public String getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(String isChecked) {
        this.isChecked = isChecked;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
