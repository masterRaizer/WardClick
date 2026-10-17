package com.wardclick.app.data.model;

import java.math.BigDecimal;

/** VIP-услуга: вертолёт, личный водитель, консьерж, персонал и т.п. */
public class VipServiceDto {
    public static final String UNIT_PER_DAY = "PER_DAY";   // цена за сутки (умножается на число ночей)
    public static final String UNIT_PER_TRIP = "PER_TRIP"; // цена за один полёт / поездку
    public static final String UNIT_ONCE = "ONCE";         // разовая услуга

    private String id;
    private String name;
    private String description;
    private String iconCode;
    private BigDecimal price;
    private String unit;
    private boolean requiresHelipad; // доступна только для особняков с вертолётной площадкой

    public VipServiceDto() {}

    public VipServiceDto(String id, String name, String description, String iconCode,
                         BigDecimal price, String unit, boolean requiresHelipad) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconCode = iconCode;
        this.price = price;
        this.unit = unit;
        this.requiresHelipad = requiresHelipad;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconCode() { return iconCode; }
    public void setIconCode(String iconCode) { this.iconCode = iconCode; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public boolean isRequiresHelipad() { return requiresHelipad; }
    public void setRequiresHelipad(boolean requiresHelipad) { this.requiresHelipad = requiresHelipad; }
}
