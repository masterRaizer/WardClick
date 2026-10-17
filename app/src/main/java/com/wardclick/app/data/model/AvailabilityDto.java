package com.wardclick.app.data.model;

import java.util.List;

public class AvailabilityDto {
    private String listingId;
    private List<String> bookedDates; // yyyy-MM-dd

    public AvailabilityDto() {}

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public List<String> getBookedDates() { return bookedDates; }
    public void setBookedDates(List<String> bookedDates) { this.bookedDates = bookedDates; }
}
