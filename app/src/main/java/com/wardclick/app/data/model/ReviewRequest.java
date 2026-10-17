package com.wardclick.app.data.model;

public class ReviewRequest {
    private String listingId;
    private double rating;
    private String comment;

    public ReviewRequest() {}

    public ReviewRequest(String listingId, double rating, String comment) {
        this.listingId = listingId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
