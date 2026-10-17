package com.wardclick.app.data.model;

import java.math.BigDecimal;
import java.util.List;

public class ListingDetailsDto {
    private String id;
    private String title;
    private String description;
    private CityDto city;
    private String address;
    private PropertyTypeDto propertyType;
    private List<String> imageUrls;
    private BigDecimal pricePerNight;
    private double rating;
    private int reviewsCount;
    private int maxGuests;
    private int bedrooms;
    private int beds;
    private int bathrooms;
    private List<AmenityDto> amenities;
    private String hostId;
    private String hostName;
    private String hostAvatarUrl;
    private boolean isSuperhost;

    public ListingDetailsDto() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CityDto getCity() { return city; }
    public void setCity(CityDto city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public PropertyTypeDto getPropertyType() { return propertyType; }
    public void setPropertyType(PropertyTypeDto propertyType) { this.propertyType = propertyType; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }

    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }

    public int getBeds() { return beds; }
    public void setBeds(int beds) { this.beds = beds; }

    public int getBathrooms() { return bathrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }

    public List<AmenityDto> getAmenities() { return amenities; }
    public void setAmenities(List<AmenityDto> amenities) { this.amenities = amenities; }

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostAvatarUrl() { return hostAvatarUrl; }
    public void setHostAvatarUrl(String hostAvatarUrl) { this.hostAvatarUrl = hostAvatarUrl; }

    public boolean isSuperhost() { return isSuperhost; }
    public void setSuperhost(boolean superhost) { this.isSuperhost = superhost; }
}
