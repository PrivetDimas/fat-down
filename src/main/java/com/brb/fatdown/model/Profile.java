package com.brb.fatdown.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Profile {
    private String id;
    private String name;
    private Sex sex;
    private int age;
    private double weightKg;
    private double heightCm;
    private double waistCm;
    private double neckCm;
    private double hip;

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static Profile sample() {
        return new Profile(newId(), "New user", Sex.MALE, 30, 80.0, 180.0, 90.0, 40.0, 80.0);
    }

    @Override
    public String toString() {
        return name + " (" + id.substring(0, 4) + ")";
    }
}
