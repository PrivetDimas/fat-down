package com.brb.fatdown.service;

import com.brb.fatdown.model.FatBurnResult;
import com.brb.fatdown.model.Profile;
import com.brb.fatdown.model.Sex;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


public class FatDownService {

    //todo на будущее пригодится; +- мои значения для тестов
    private static final double KCAL_NEAT_PER_MINUTE = 3.0;
    private static final double KCAL_LISS_PER_MINUTE = 4.5;
    private static final double KCAL_MED_PER_MINUTE = 6.0;
    private static final double MET_NEAT = 2.06;
    public static final double MET_LISS = 4.5;
    public static final double MET_MED = 6.0;

    public double calculateBodyFat(final Profile p) {
        double bf;
        if (Sex.MALE.equals(p.getSex())) {
            bf = 495 / (1.0324 - 0.19077 * Math.log10(p.getWaistCm() - p.getNeckCm())
                    + 0.15456 * Math.log10(p.getHeightCm())) - 450;
        } else {
            bf = 495 / (1.29579 - 0.35004 * Math.log10(p.getWaistCm() + p.getHip() - p.getNeckCm())
                    + 0.22100 * Math.log10(p.getHeightCm())) - 450;
        }
        return bf;
    }

    public FatBurnResult analyze(Profile p, double targetBfPercent, final LocalDate targetDate,
                                 final boolean neatBoxSelected, final boolean lissBoxSelected, final boolean mediumBoxSelected) {
        double currentBf = calculateBodyFat(p);
        double fatKgNow = p.getWeightKg() * (currentBf / 100.0);
        double fatKgTarget = p.getWeightKg() * (targetBfPercent / 100.0);
        double toLose = Math.max(0.0, fatKgNow - fatKgTarget);
        double kcalTotal = toLose * 7700.0; // approximate

        // kcal per hour approximated as MET * weightKg.
        // 1.05 — эмпирическая поправка
        double kcalPerHourNeat = MET_NEAT * p.getWeightKg() * 1.05;
        double kcalPerHourLiss = MET_LISS * p.getWeightKg() * 1.05;
        double kcalPerHourMed = MET_MED * p.getWeightKg() * 1.05;

        double hoursNeat = kcalPerHourNeat > 0 ? kcalTotal / kcalPerHourNeat : Double.POSITIVE_INFINITY;
        double hoursLiss = kcalPerHourLiss > 0 ? kcalTotal / kcalPerHourLiss : Double.POSITIVE_INFINITY;
        double hoursMed = kcalPerHourMed > 0 ? kcalTotal / kcalPerHourMed : Double.POSITIVE_INFINITY;

        long days = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        if (days <= 0) days = 1;

        double neatPerDay = neatBoxSelected ? hoursNeat / days : 0;
        double lissPerDay = lissBoxSelected ? hoursLiss / days : 0;
        double mediumPerDay = mediumBoxSelected ? hoursMed / days : 0;

        return new FatBurnResult(toLose, kcalTotal, hoursNeat, hoursLiss, hoursMed,
                neatPerDay, lissPerDay, mediumPerDay);
    }
}
