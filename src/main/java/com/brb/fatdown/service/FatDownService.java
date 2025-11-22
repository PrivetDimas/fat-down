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

    // Доли энергии, приходящиеся на жир при данной интенсивности (эмпирические)
    private static final double FAT_SHARE_NEAT = 0.70;   // ~70% при очень лёгкой активности
    private static final double FAT_SHARE_LISS = 0.55;   // ~55% при LISS
    private static final double FAT_SHARE_MED = 0.40;    // ~40% при средней интенсивности

    public static final double CARB_SHARE_NEAT = 0.30;
    public static final double CARB_SHARE_LISS = 0.45;
    public static final double CARB_SHARE_MED = 0.60;

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
        double kcalTotal = toLose * 7700.0; //

        //кол-во ккал с поправкой на % сжигаемого жира и гликогена:
        // https://journals.physiology.org/doi/full/10.1152/jappl.2000.88.5.1707?utm_source=chatgpt.com
        //Например: поработали 1 час при 95 BPM, браслет показывает, что сожгли 180 ккал, но это общее число ккал.
        //Общее число ккал = энергия из жира + энергия из гликогена; Какой процент при 95 BPM? Из статьи ~70%.
        //Соответственно, 180 ккал/час = 126 ккал из жира + 54 ккал из гликогена
        // ---- вычисления базовых величин ----
        double kcalPerHourNeat = MET_NEAT * p.getWeightKg() * 1.05;
        double kcalPerHourLiss = MET_LISS * p.getWeightKg() * 1.05;
        double kcalPerHourMed = MET_MED * p.getWeightKg() * 1.05;

        long days = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        if (days <= 0) days = 1;

        // доли (предполагается, что CARB_SHARE = 1 - FAT_SHARE)
        double fatKcalPerHourNeat = kcalPerHourNeat * FAT_SHARE_NEAT;
        double fatKcalPerHourLiss = kcalPerHourLiss * FAT_SHARE_LISS;
        double fatKcalPerHourMed = kcalPerHourMed * FAT_SHARE_MED;

        double carbKcalPerHourNeat = kcalPerHourNeat * CARB_SHARE_NEAT;
        double carbKcalPerHourLiss = kcalPerHourLiss * CARB_SHARE_LISS;
        double carbKcalPerHourMed = kcalPerHourMed * CARB_SHARE_MED;

        // ---- время, чтобы сжечь нужные ккал жира (в часах) ----
        double fatHoursNeat = (fatKcalPerHourNeat > 0) ? (kcalTotal / fatKcalPerHourNeat) : Double.POSITIVE_INFINITY;
        double fatHoursLiss = (fatKcalPerHourLiss > 0) ? (kcalTotal / fatKcalPerHourLiss) : Double.POSITIVE_INFINITY;
        double fatHoursMed = (fatKcalPerHourMed > 0) ? (kcalTotal / fatKcalPerHourMed) : Double.POSITIVE_INFINITY;

        // ---- сколько жира/углеводов тратится в день при выбранном расписании ----
        // часы жира в день
        double fatNeatPerDay = neatBoxSelected ? (fatHoursNeat / days) : 0;
        double fatLissPerDay = lissBoxSelected ? (fatHoursLiss / days) : 0;
        double fatMediumPerDay = mediumBoxSelected ? (fatHoursMed / days) : 0;

        // за это же время — сколько ккал из углеводов сгорело:
        // total carb kcal за весь период = fatHours * carbKcalPerHour
        double carbTotalNeat = (carbKcalPerHourNeat > 0 && !Double.isInfinite(fatHoursNeat)) ? fatHoursNeat * carbKcalPerHourNeat : 0;
        double carbTotalLiss = (carbKcalPerHourLiss > 0 && !Double.isInfinite(fatHoursLiss)) ? fatHoursLiss * carbKcalPerHourLiss : 0;
        double carbTotalMed = (carbKcalPerHourMed > 0 && !Double.isInfinite(fatHoursMed)) ? fatHoursMed * carbKcalPerHourMed : 0;

        // carb per day = carbTotal / days  (или равняется fatPerDayHours * carbKcalPerHour)
        double carbNeatPerDay = neatBoxSelected ? (carbTotalNeat / days) : 0;
        double carbLissPerDay = lissBoxSelected ? (carbTotalLiss / days) : 0;
        double carbMediumPerDay = mediumBoxSelected ? (carbTotalMed / days) : 0;

        return new FatBurnResult(toLose, kcalTotal, fatHoursNeat, fatHoursLiss, fatHoursMed,
                fatNeatPerDay, fatLissPerDay, fatMediumPerDay,
                carbKcalPerHourNeat, carbKcalPerHourLiss, carbKcalPerHourMed,
                carbNeatPerDay, carbLissPerDay, carbMediumPerDay);
    }
}
