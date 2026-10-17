package com.wardclick.app.data.model;

import java.math.BigDecimal;
import java.util.List;

public class ListingRequest {
    private String title;
    private String description;
    private String cityId;
    private String address;
    private String propertyTypeId;
    private List<String> imageUrls;
    private BigDecimal pricePerNight;
    private int maxGuests;
    private int bedrooms;
    private int beds;
    private int bathrooms;
    private List<String> amenityIds;

    public ListingRequest() {}

    // Add getters and setters for all fields
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCityId() { return cityId; }
    public void setCityId(String cityId) { this.cityId = cityId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPropertyTypeId() { return propertyTypeId; }
    public void setPropertyTypeId(String propertyTypeId) { this.propertyTypeId = propertyTypeId; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }

    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }

    public int getBeds() { return beds; }
    public void setBeds(int beds) { this.beds = beds; }

    public int getBathrooms() { return bathrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }

    public List<String> getAmenityIds() { return amenityIds; }
    public void setAmenityIds(List<String> amenityIds) { this.amenityIds = amenityIds; }
}
