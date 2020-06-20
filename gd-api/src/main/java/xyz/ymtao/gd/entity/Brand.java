package xyz.ymtao.gd.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

public class Brand implements Serializable {
    @Id
    private Integer id;
    @Column
    private String name;
    @Column
    private String logo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
