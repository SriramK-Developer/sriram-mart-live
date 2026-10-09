package com.srirammart.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String username;
    @Column(nullable = false, unique = true, length = 120)
    private String email;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role = Role.BUYER;
    @Column(length = 20)
    private String phone;
    @Column(length = 120)
    private String storeName;
    private Double storeRating;
    private Integer storeRatings;
    private boolean enabled = true;
    private boolean approved = true;
    private int failedAttempts;
    private LocalDateTime lockUntil;
    @Column(length = 200)
    private String addressLine;
    @Column(length = 80)
    private String city;
    @Column(length = 80)
    private String stateName;
    @Column(length = 10)
    private String pincode;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isSeller() { return role == Role.SELLER; }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public String getFirstName() {
        String n = fullName == null ? "" : fullName.trim();
        int i = n.indexOf(' ');
        return i > 0 ? n.substring(0, i) : n;
    }
    public String getRoleLabel() {
        return role == Role.ADMIN ? "Admin" : role == Role.SELLER ? "Seller" : "Buyer";
    }

        public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Double getStoreRating() { return storeRating; }
    public void setStoreRating(Double storeRating) { this.storeRating = storeRating; }
    public Integer getStoreRatings() { return storeRatings; }
    public void setStoreRatings(Integer storeRatings) { this.storeRatings = storeRatings; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
