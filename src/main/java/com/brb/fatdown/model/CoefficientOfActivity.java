package com.brb.fatdown.model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum CoefficientOfActivity {
    NO_ACTIVITY(1.2, "Программист"),
    LOW_ACTIVITY(1.375, "Преподаватель"),
    MEDIUM_ACTIVITY(1.55, "Официант"),
    HIGH_ACTIVITY(1.725, "Грузчик"),
    VERY_HIGH_ACTIVITY(1.9, "Шахтёр");

    private final double coefficient;
    private final String professionExample;

    CoefficientOfActivity(double coefficient, final String professionExample) {
        this.coefficient = coefficient;
        this.professionExample = professionExample;
    }

    public static String[] getProfessionExamples() {
        return Arrays.stream(CoefficientOfActivity.values()).map(CoefficientOfActivity::getProfessionExample).toArray(String[]::new);
    }

    public static CoefficientOfActivity getByProfessionExample(String professionExample) {
        return Arrays.stream(CoefficientOfActivity.values())
                .filter(x -> x.getProfessionExample().equals(professionExample)).findFirst().orElse(NO_ACTIVITY);
    }

}
