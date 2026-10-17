package com.wardclick.app.data.model;

import java.math.BigDecimal;
import java.util.List;

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
    private String status; // PENDING, CONFIRMED, CANCELLED (COMPLETED вычисляется на клиенте)
    private String userId;
    private String guestName;
    private String guestEmail;
    private boolean reviewed; // отзыв по этой брони уже оставлен
    private List<String> vipServiceNames; // заказанные VIP-услуги
    private BigDecimal vipTotal;          // стоимость VIP-услуг (входит в totalPrice)
    private BigDecimal discount;          // сумма скидки (уже вычтена из totalPrice)
    private boolean allInclusive;         // выбран пакет «Всё включено»
    private String orderId;               // общий заказ, если арендовано несколько особняков сразу

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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }

    public List<String> getVipServiceNames() { return vipServiceNames; }
    public void setVipServiceNames(List<String> vipServiceNames) { this.vipServiceNames = vipServiceNames; }

    public BigDecimal getVipTotal() { return vipTotal; }
    public void setVipTotal(BigDecimal vipTotal) { this.vipTotal = vipTotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public boolean isAllInclusive() { return allInclusive; }
    public void setAllInclusive(boolean allInclusive) { this.allInclusive = allInclusive; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
