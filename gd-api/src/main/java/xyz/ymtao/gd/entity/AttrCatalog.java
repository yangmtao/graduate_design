package xyz.ymtao.gd.entity;

import java.io.Serializable;

public class AttrCatalog implements Serializable {
    private Integer id;
    private Integer attrId;
    private Integer catalog3Id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttrId() {
        return attrId;
    }

    public void setAttrId(Integer attrId) {
        this.attrId = attrId;
    }

    public Integer getCatalog3Id() {
        return catalog3Id;
    }

    public void setCatalog3Id(Integer catalog3Id) {
        this.catalog3Id = catalog3Id;
    }
}
