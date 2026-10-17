package com.wardclick.app.data.model;

import java.math.BigDecimal;

public class BookingDto {
    private String id;
    private String listingId;
    private String listingTitle;
    private String listingImageUrl;
    private String listingLocation;
    private String checkInDate; // yyyy-MM-dd
    private String checkOutDate; // yyyy-MM-dd
    private int guestsCount;
    private BigDecimal totalPrice;
    private String status; // PENDING, CONFIRMED, CANCELLED

    public BookingDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getListingTitle() { return listingTitle; }
    public void setListingTitle(String listingTitle) { this.listingTitle = listingTitle; }

    public String getListingImageUrl() { return listingImageUrl; }
    public void setListingImageUrl(String listingImageUrl) { this.listingImageUrl = listingImageUrl; }

    public String getListingLocation() { return listingLocation; }
    public void setListingLocation(String listingLocation) { this.listingLocation = listingLocation; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getGuestsCount() { return guestsCount; }
    public void setGuestsCount(int guestsCount) { this.guestsCount = guestsCount; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
