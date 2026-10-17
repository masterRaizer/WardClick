package com.wardclick.app.data.model;

import java.util.List;

/** Заказ из нескольких особняков: каждая позиция — отдельная аренда со своими датами и VIP-услугами. */
public class CreateOrderRequest {
    private List<CreateBookingRequest> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(List<CreateBookingRequest> items) {
        this.items = items;
    }

    public List<CreateBookingRequest> getItems() { return items; }
    public void setItems(List<CreateBookingRequest> items) { this.items = items; }
}
