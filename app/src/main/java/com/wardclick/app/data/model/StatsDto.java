package com.wardclick.app.data.model;

import java.math.BigDecimal;

public class StatsDto {
    private int totalUsers;
    private int totalListings;
    private int totalBookings;
    private BigDecimal totalRevenue;

    public StatsDto() {}

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getTotalListings() { return totalListings; }
    public void setTotalListings(int totalListings) { this.totalListings = totalListings; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
}
