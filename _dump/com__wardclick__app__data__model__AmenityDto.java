package com.wardclick.app.data.model;

public class AmenityDto {
    private String id;
    private String name;
    private String iconCode;

    public AmenityDto() {}

    public AmenityDto(String id, String name, String iconCode) {
        this.id = id;
        this.name = name;
        this.iconCode = iconCode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconCode() { return iconCode; }
    public void setIconCode(String iconCode) { this.iconCode = iconCode; }
}
