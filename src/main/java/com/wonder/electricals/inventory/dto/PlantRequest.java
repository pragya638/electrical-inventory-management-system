package com.wonder.electricals.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PlantRequest {

    @NotBlank(message = "Plant name is required")
    @Size(max = 120, message = "Plant name must not exceed 120 characters")
    private String name;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;

    @Size(max = 100, message = "Contact person must not exceed 100 characters")
    private String contactPerson;

    @Pattern(regexp = "^$|^[0-9+\\- ]{6,20}$",
            message = "Contact number must be 6 to 20 digits and may contain +, - or spaces")
    private String contactNumber;

    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
