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
    private double hoursNeat;
    private double hoursLiss;
    private double hoursMedium;
    private double neatPerDay;
    private double lissPerDay;
    private double mediumPerDay;
}
