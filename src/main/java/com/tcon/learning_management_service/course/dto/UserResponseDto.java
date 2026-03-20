package com.tcon.learning_management_service.course.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private String id;
    private String firstName;
    private String lastName;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    private String profilePicture;

    public String getName() {
        if (firstName != null && lastName != null) {
            return (firstName + " " + lastName).trim();
        }
        if (firstName != null) return firstName;
        if (email != null) return email.split("@")[0];
        return "Expert Instructor";
    }
}