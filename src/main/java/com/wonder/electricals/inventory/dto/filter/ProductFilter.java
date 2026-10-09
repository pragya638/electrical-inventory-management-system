package com.wonder.electricals.inventory.dto.filter;

/** Query parameters accepted by the product list page and {@code GET /api/products}. */
public class ProductFilter {

    private String search;
    private Long categoryId;
    private String status;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
