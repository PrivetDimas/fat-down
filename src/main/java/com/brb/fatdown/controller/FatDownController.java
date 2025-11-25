package com.brb.fatdown.controller;


import com.brb.fatdown.model.ActivityMode;
import com.brb.fatdown.model.CoefficientOfActivity;
import com.brb.fatdown.model.FatBurnResult;
import com.brb.fatdown.model.Profile;
import com.brb.fatdown.model.Sex;
import com.brb.fatdown.repo.ProfileRepository;
import com.brb.fatdown.service.FatDownService;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FatDownController {
    private static final String KCAL_AND_PFC_TEMPLATE = "%s: %.0f ккал или %.1f гр";
    private static final String SLIDER_PATTERN = "%.1f%%";
    private final ProfileRepository repo;
    private final FatDownService service;
    public FatDownController(ProfileRepository repo, FatDownService service) {
        this.repo = repo;
        this.service = service;
    }


    //Top
    @FXML
    private ComboBox<Profile> profileCombo;
    @FXML
    private Button newProfileButton;
    @FXML
    private Button saveProfileButton;

    //Left/center
    @FXML
    private TextField nameField;
    @FXML
    private ChoiceBox<Sex> sexChoice;
    @FXML
    private TextField ageField;
    @FXML
    private TextField weightField;
    @FXML
    private TextField heightField;
    @FXML
    private TextField waistField;
    @FXML
    private TextField neckField;
    @FXML
    private TextField hipField;
    @FXML
    private Label hipLabel;

    //Center
    @FXML
    private Slider targetSlider;
    @FXML
    private Label currentBfLabel;
    @FXML
    private Label sliderValueLabel;
    @FXML
    private DatePicker targetDatePicker;
    @FXML
    private CheckBox neatBox;
    @FXML
    private CheckBox lissBox;
    @FXML
    private CheckBox mediumBox;
    @FXML
    private ChoiceBox<String> coefficientChoiceBox;
    @FXML
    private Button calculateButton;

    //Right
    @FXML
    private VBox resultsBox;
    @FXML
    private Label fatKgLabel;
    @FXML
    private Label fatKcalLabel;
    @FXML
    private Label neatTimeLabel;
    @FXML
    private Label lissTimeLabel;
    @FXML
    private Label mediumTimeLabel;
    @FXML
    private Label perDayNeat;
    @FXML
    private Label perDayLiss;
    @FXML
    private Label perDayMedium;
    @FXML
    private Label carbNeatPerHour;
    @FXML
    private Label carbLissPerHour;
    @FXML
    private Label carbMedPerHour;
    @FXML
    private Label carbNeatPerDay;
    @FXML
    private Label carbLissPerDay;
    @FXML
    private Label carbMedPerDay;
    @FXML
    private Label tdee;
    @FXML
    private TextField minutesToFatAndCarb;
    @FXML
    private Label fatAndCarb;
    @FXML
    private Slider bpmSlider;
    @FXML
    private Label bpmValueLabel;


    private Profile current;


    @FXML
    public void initialize() {
        sexChoice.getItems().addAll(Sex.values());
        coefficientChoiceBox.getItems().addAll(CoefficientOfActivity.getProfessionExamples());

        List<Profile> all = repo.loadAllProfiles();
        profileCombo.getItems().addAll(all);
        profileCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Profile object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public Profile fromString(String string) {
                return null;
            }
        });

        Optional<Profile> last = repo.loadLastUsedProfile(all);
        if (last.isPresent()) {
            profileCombo.getSelectionModel().select(last.get());
            setCurrentProfile(last.get());
        } else if (!all.isEmpty()) {
            profileCombo.getSelectionModel().select(0);
            setCurrentProfile(all.get(0));
        } else {
            Profile p = Profile.sample();
            repo.saveProfile(p);
            profileCombo.getItems().add(p);
            profileCombo.getSelectionModel().select(p);
            setCurrentProfile(p);
        }

        profileCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) setCurrentProfile(n);
        });


        ChangeListener<String> fieldListener = (obs, o, n) -> recomputeCurrentBfAndAdjustSlider();
        nameField.textProperty().addListener(fieldListener);
        ageField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                ageField.setText(newV.replaceAll("[^\\d]", ""));
                return;
            }
            updateBpmSliderRange();
        });
        weightField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("[\\d,.]*")) weightField.setText(oldV);
        });
        heightField.textProperty().addListener(fieldListener);
        waistField.textProperty().addListener(fieldListener);
        neckField.textProperty().addListener(fieldListener);
        hipField.textProperty().addListener(fieldListener);
        sexChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> recomputeCurrentBfAndAdjustSlider());

        targetSlider.valueProperty().addListener((o, oldV, newV) -> sliderValueLabel.setText(String.format(SLIDER_PATTERN, newV.doubleValue())));

        calculateButton.setOnAction(e -> onCalculate());
        newProfileButton.setOnAction(e -> onNewProfile());
        saveProfileButton.setOnAction(e -> onSaveProfile());

        sliderValueLabel.setText(String.format(SLIDER_PATTERN, targetSlider.getValue()));
        bpmSlider.valueProperty().addListener((obs, oldV, newV) -> {
            bpmValueLabel.setText(String.format("%.0f", newV.doubleValue()));
            if (!minutesToFatAndCarb.getText().isEmpty()) {
                convertMinutesToFatAndCarb();
            }
        });
        minutesToFatAndCarb.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*") || newV.length() > 9) {
                minutesToFatAndCarb.setText(oldV);
                return;
            }

            if (!newV.isEmpty()) {
                convertMinutesToFatAndCarb();
            } else {
                fatAndCarb.setText("-");
            }
        });
        updateBpmSliderRange();
    }

    private void setCurrentProfile(Profile p) {
        this.current = p;
        nameField.setText(p.getName());
        sexChoice.getSelectionModel().select(p.getSex());
        ageField.setText(String.valueOf(p.getAge()));
        weightField.setText(String.valueOf(p.getWeightKg()));
        heightField.setText(String.valueOf(p.getHeightCm()));
        waistField.setText(String.valueOf(p.getWaistCm()));
        neckField.setText(String.valueOf(p.getNeckCm()));
        hipField.setText(String.valueOf(p.getHip()));

        recomputeCurrentBfAndAdjustSlider();
        updateBpmSliderRange();
    }

    private void recomputeCurrentBfAndAdjustSlider() {
        try {
            Profile p = readFieldsToProfileOrNull();
            if (p == null) {
                currentBfLabel.setText("-");
                return;
            }

            //защита от невозможных комбинаций для формулы
            boolean validForCalc;
            if (Sex.MALE.equals(p.getSex())) {
                validForCalc = (p.getWaistCm() - p.getNeckCm()) > 0 && p.getHeightCm() > 0;
            } else {
                validForCalc = (p.getWaistCm() + p.getHip() - p.getNeckCm()) > 0 && p.getHeightCm() > 0;
            }
            if (!validForCalc) {
                currentBfLabel.setText("-");
                return;
            }

            double bf = service.calculateBodyFat(p);
            if (Double.isNaN(bf) || Double.isInfinite(bf)) {
                currentBfLabel.setText("-");
                return;
            }

            bf = Math.clamp(bf, 2.0, 60.0);

            currentBfLabel.setText(String.format("%.2f%%", bf));
            targetSlider.setMin(5.0);
            targetSlider.setMax(Math.max(5.0, bf));
            if (targetSlider.getValue() > targetSlider.getMax()) targetSlider.setValue(targetSlider.getMax());
            sliderValueLabel.setText(String.format(SLIDER_PATTERN, targetSlider.getValue()));

            if (p.getSex() == Sex.MALE) {
                hipField.setVisible(false);
                hipLabel.setVisible(false);
            } else {
                hipField.setVisible(true);
                hipLabel.setVisible(true);
            }
        } catch (Exception _) {
            currentBfLabel.setText("-");
        }
    }

    private void convertMinutesToFatAndCarb() {
        String minutesText = minutesToFatAndCarb.getText();
        if (minutesText == null || minutesText.isEmpty()) {
            fatAndCarb.setText("-");
            return;
        }

        int minutes = Integer.parseInt(minutesText);
        if (minutes <= 0) {
            fatAndCarb.setText("-");
            return;
        }

        double weightKg;
        if (current != null) {
            weightKg = current.getWeightKg();
        } else {
            try {
                weightKg = Double.parseDouble(weightField.getText());
            } catch (Exception _) {
                fatAndCarb.setText("Укажите вес");
                return;
            }
        }

        double bpm = bpmSlider.getValue();
        IntensityProfile intensity = intensityForBpm(bpm);
        double kcalPerHour = intensity.met() * weightKg * FatDownService.MET_CORRECTION; // kcal per hour

        double totalKcal = kcalPerHour * (minutes / 60.0);

        double fatKcal = totalKcal * intensity.fatShare();
        double carbKcal = totalKcal * intensity.carbShare();

        final double KCAL_PER_GRAM_BODY_FAT = 7.7;
        final double KCAL_PER_GRAM_CARB = 4.0;

        double fatGrams = fatKcal / KCAL_PER_GRAM_BODY_FAT;
        double carbGrams = carbKcal / KCAL_PER_GRAM_CARB;

        fatAndCarb.setText(String.format("%s при %.0f BPM   Всего %.0f ккал → ЖИР: %.0f ккал (%.1f г), УГЛИ: %.0f ккал (%.1f г)",
                                            formatMinutes(minutes), bpm, totalKcal, fatKcal, fatGrams, carbKcal, carbGrams));
    }

    private void updateBpmSliderRange() {
        double age;
        try {
            age = Double.parseDouble(ageField.getText());
        } catch (Exception _) {
            age = 30.0;
        }

        double maxHeartRate = Math.max(120.0, 220.0 - age);
        double minBpm = Math.round(maxHeartRate * 0.5);
        double maxBpm = Math.round(maxHeartRate * 0.7);

        bpmSlider.setMin(minBpm);
        bpmSlider.setMax(Math.max(minBpm + 1, maxBpm));
        if (bpmSlider.getValue() < bpmSlider.getMin() || bpmSlider.getValue() > bpmSlider.getMax()) {
            bpmSlider.setValue((bpmSlider.getMin() + bpmSlider.getMax()) / 2.0);
        }
        bpmValueLabel.setText(String.format("%.0f", bpmSlider.getValue()));
    }

    private IntensityProfile intensityForBpm(double bpm) {
        if (bpm <= 95) {
            return new IntensityProfile(FatDownService.MET_NEAT, FatDownService.FAT_SHARE_NEAT, FatDownService.CARB_SHARE_NEAT);
        }
        if (bpm <= 110) {
            double ratio = (bpm - 95) / 15.0;
            return interpolateIntensity(ratio,
                    new IntensityProfile(FatDownService.MET_NEAT, FatDownService.FAT_SHARE_NEAT, FatDownService.CARB_SHARE_NEAT),
                    new IntensityProfile(FatDownService.MET_LISS, FatDownService.FAT_SHARE_LISS, FatDownService.CARB_SHARE_LISS));
        }
        if (bpm <= 130) {
            double ratio = (bpm - 110) / 20.0;
            return interpolateIntensity(ratio,
                    new IntensityProfile(FatDownService.MET_LISS, FatDownService.FAT_SHARE_LISS, FatDownService.CARB_SHARE_LISS),
                    new IntensityProfile(FatDownService.MET_MED, FatDownService.FAT_SHARE_MED, FatDownService.CARB_SHARE_MED));
        }
        return new IntensityProfile(FatDownService.MET_MED, FatDownService.FAT_SHARE_MED, FatDownService.CARB_SHARE_MED);
    }

    private IntensityProfile interpolateIntensity(double ratio, IntensityProfile from, IntensityProfile to) {
        ratio = Math.clamp(ratio, 0.0, 1.0);
        double met = from.met() + (to.met() - from.met()) * ratio;
        double fatShare = from.fatShare() + (to.fatShare() - from.fatShare()) * ratio;
        double carbShare = from.carbShare() + (to.carbShare() - from.carbShare()) * ratio;
        return new IntensityProfile(met, fatShare, carbShare);
    }

    private record IntensityProfile(double met, double fatShare, double carbShare) {
    }

    private Profile readFieldsToProfileOrNull() {
        try {
            String name = nameField.getText().trim();
            Sex sex = sexChoice.getValue();
            if (sex == null) return null;

            int age = Integer.parseInt(ageField.getText().trim());
            double weight = Double.parseDouble(weightField.getText().trim());
            double height = Double.parseDouble(heightField.getText().trim());
            double waist = Double.parseDouble(waistField.getText().trim());
            double neck = Double.parseDouble(neckField.getText().trim());
            double hip = 0.0;
            if (sex == Sex.FEMALE) {
                hip = Double.parseDouble(hipField.getText().trim());
            }

            if (age <= 0 || weight <= 0 || height <= 0 || waist <= 0 || neck <= 0 || (sex == Sex.FEMALE && hip <= 0)) {
                return null;
            }

            return new Profile(current == null ? Profile.newId() : current.getId(),
                    name.isEmpty() ? "Unnamed" : name,
                    sex, age, weight, height, waist, neck, hip);
        } catch (Exception _) {
            return null;
        }
    }

    private void onNewProfile() {
        Profile p = Profile.sample();
        repo.saveProfile(p);
        profileCombo.getItems().add(p);
        profileCombo.getSelectionModel().select(p);
        setCurrentProfile(p);
    }

    private void onSaveProfile() {
        Profile p = readFieldsToProfileOrNull();
        if (p == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Please fill numeric fields correctly.", ButtonType.OK);
            a.showAndWait();
            return;
        }
        repo.saveProfile(p);
        repo.setLastUsedProfileId(p.getId());

        // replace existing entry if present, otherwise add
        int idx = profileCombo.getItems().indexOf(current);
        if (idx >= 0) {
            profileCombo.getItems().set(idx, p);
        } else {
            profileCombo.getItems().add(p);
        }
        profileCombo.getSelectionModel().select(p);
        setCurrentProfile(p);
    }

    private void onCalculate() {
        Profile p = readFieldsToProfileOrNull();
        if (p == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Заполните все поля корректно.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        double targetBf = Math.round(targetSlider.getValue() * 10.0) / 10.0;
        LocalDate targetDate = targetDatePicker.getValue();
        if (targetDate == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Выберите целевую дату.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        FatBurnResult res = service.analyze(p, targetBf, targetDate,
                neatBox.isSelected(), lissBox.isSelected(), mediumBox.isSelected());

        fatKgLabel.setText(String.format("Жир к потере: %.3f кг", res.getFatKgToLose()));
        fatKcalLabel.setText(String.format("Эквивалент: %.0f ккал", res.getKcalTotal()));

        frameFullTime(res, neatBox.isSelected(), lissBox.isSelected(), mediumBox.isSelected());

        framePerDayTime(res, neatBox.isSelected(), lissBox.isSelected(), mediumBox.isSelected());

        frameCarbsPerHour(res, neatBox.isSelected(), lissBox.isSelected(), mediumBox.isSelected());

        frameCarbsPerDay(res, neatBox.isSelected(), lissBox.isSelected(), mediumBox.isSelected());

        frameBmrAndPFC(p.getWeightKg(), p.getHeightCm(), p.getAge(),
                CoefficientOfActivity.getByProfessionExample(coefficientChoiceBox.getSelectionModel().getSelectedItem()));

        repo.setLastUsedProfileId(p.getId());
    }

    private void frameFullTime(final FatBurnResult res, final boolean neatBoxSelected, final boolean lissBoxSelected,
                               final boolean mediumBoxSelected) {
        neatTimeLabel.setText(neatBoxSelected ? formatHours(res.getFatHoursNeat(), ActivityMode.NEAT.name()) : "");
        lissTimeLabel.setText(lissBoxSelected ? formatHours(res.getFatHoursLiss(), ActivityMode.LISS.name()) : "");
        mediumTimeLabel.setText(mediumBoxSelected ? formatHours(res.getFatHoursMedium(), ActivityMode.MEDIUM.name()) : "");
    }

    private void framePerDayTime(final FatBurnResult res, final boolean neatBoxSelected, final boolean lissBoxSelected,
                                 final boolean mediumBoxSelected) {
        perDayNeat.setText(neatBoxSelected ? formatHours(res.getFatNeatPerDay(), ActivityMode.NEAT.name()) : "");
        perDayLiss.setText(lissBoxSelected ? formatHours(res.getFatLissPerDay(), ActivityMode.LISS.name()) : "");
        perDayMedium.setText(mediumBoxSelected ? formatHours(res.getFatMediumPerDay(), ActivityMode.MEDIUM.name()) : "");
    }

    private void frameCarbsPerHour(final FatBurnResult res, final boolean neatBoxSelected, final boolean lissBoxSelected,
                                   final boolean mediumBoxSelected) {
        carbNeatPerHour.setText(neatBoxSelected ? String.format(KCAL_AND_PFC_TEMPLATE, ActivityMode.NEAT.name(), res.getCarbNeatPerHour(),
                res.getCarbNeatPerHour() / 4.0) : "");
        carbLissPerHour.setText(lissBoxSelected ? String.format(KCAL_AND_PFC_TEMPLATE, ActivityMode.LISS.name(), res.getCarbLissPerHour(),
                res.getCarbLissPerHour() / 4.0) : "");
        carbMedPerHour.setText(mediumBoxSelected ? String.format(KCAL_AND_PFC_TEMPLATE, ActivityMode.MEDIUM.name(), res.getCarbMediumPerHour(),
                res.getCarbMediumPerHour() / 4.0) : "");
    }

    private void frameCarbsPerDay(final FatBurnResult res, final boolean neatBoxSelected, final boolean lissBoxSelected,
                                  final boolean mediumBoxSelected) {
        carbNeatPerDay.setText(neatBoxSelected ? String.format(KCAL_AND_PFC_TEMPLATE, ActivityMode.NEAT.name(), res.getCarbNeatPerDay(),
                res.getCarbNeatPerDay() / 4.0) : "");
        carbLissPerDay.setText(lissBoxSelected ? String.format(KCAL_AND_PFC_TEMPLATE, ActivityMode.LISS.name(), res.getCarbLissPerDay(),
                res.getCarbLissPerDay() / 4.0) : "");
        carbMedPerDay.setText(mediumBoxSelected ? String.format(KCAL_AND_PFC_TEMPLATE, ActivityMode.MEDIUM.name(), res.getCarbMediumPerDay(),
                res.getCarbMediumPerDay() / 4.0) : "");
    }

    private void frameBmrAndPFC(final double weight, final double height, final int age,
                                final CoefficientOfActivity coefficientOfActivity) {
        double dailyKcal = (10 * weight + 6.25 * height - 5 * age + 5) * coefficientOfActivity.getCoefficient();
        double leanMass = weight * (1 - 0.1565);
        double proteinForLean = 2.6 * leanMass;
        double fatForLean = 0.6 * weight;
        double carbForLean = (dailyKcal - (proteinForLean * 4 + fatForLean * 9)) / 4;
        this.tdee.setText(String.format("%.0f ккал, %.1f гр белка, %.1f гр жиров, %.1f гр углеводов", dailyKcal, proteinForLean, fatForLean,
                carbForLean));
    }

    private String formatHours(double hours, String ex) {
        if (Double.isInfinite(hours)) return "—";
        int h = (int) Math.floor(hours);
        int m = (int) Math.round((hours - h) * 60);
        return String.format("%s: %d ч %d мин", ex, h, m);
    }

    private String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format("%d:%02d", h, m);
    }
}