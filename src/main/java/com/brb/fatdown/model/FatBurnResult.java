package com.brb.fatdown.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FatBurnResult {
    private double fatKgToLose;
    private double kcalTotal;
    private double fatHoursNeat;
    private double fatHoursLiss;
    private double fatHoursMedium;
    private double fatNeatPerDay;
    private double fatLissPerDay;
    private double fatMediumPerDay;
    private double carbNeatPerHour;
    private double carbLissPerHour;
    private double carbMediumPerHour;
    private double carbNeatPerDay;
    private double carbLissPerDay;
    private double carbMediumPerDay;
}
