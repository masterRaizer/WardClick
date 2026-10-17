package com.wardclick.app.data.model;

import java.math.BigDecimal;
import java.util.List;

public class CreateBookingRequest {
    private String listingId;
    private String checkInDate; // yyyy-MM-dd
    private String checkOutDate; // yyyy-MM-dd
    private int guestsCount;
    private BigDecimal totalPrice;
    private List<String> vipServiceIds; // выбранные VIP-услуги

    public CreateBookingRequest() {}

    public CreateBookingRequest(String listingId, String checkInDate, String checkOutDate, int guestsCount, BigDecimal totalPrice) {
        this.listingId = listingId;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestsCount = guestsCount;
        this.totalPrice = totalPrice;
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getGuestsCount() { return guestsCount; }
    public void setGuestsCount(int guestsCount) { this.guestsCount = guestsCount; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public List<String> getVipServiceIds() { return vipServiceIds; }
    public void setVipServiceIds(List<String> vipServiceIds) { this.vipServiceIds = vipServiceIds; }
}
