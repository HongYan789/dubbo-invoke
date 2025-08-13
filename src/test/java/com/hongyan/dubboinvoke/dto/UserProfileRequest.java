package com.hongyan.dubboinvoke.dto;

/**
 * 用户档案请求对象，包含嵌套的地址信息
 */
public class UserProfileRequest {
    private Long id;
    private String name;
    private String email;
    private AddressInfo address;
    private ContactInfo contact;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public AddressInfo getAddress() {
        return address;
    }
    
    public void setAddress(AddressInfo address) {
        this.address = address;
    }
    
    public ContactInfo getContact() {
        return contact;
    }
    
    public void setContact(ContactInfo contact) {
        this.contact = contact;
    }
}