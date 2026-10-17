package com.wardclick.app.data.model;

public class CityDto {
    private String id;
    private String name;

    public CityDto() {}

    public CityDto(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
