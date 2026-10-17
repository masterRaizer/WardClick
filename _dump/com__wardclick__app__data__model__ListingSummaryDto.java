package com.wardclick.app.data.model;

import java.math.BigDecimal;
import java.util.List;

public class ListingSummaryDto {
    private String id;
    private String title;
    private String location; // format: "City, address"
    private String imageUrl; // single main image
    private BigDecimal pricePerNight;
    private double rating;
    private int reviewsCount;
    private List<String> tags; // e.g. "Суперхозяин", "Новинка"

    public ListingSummaryDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
