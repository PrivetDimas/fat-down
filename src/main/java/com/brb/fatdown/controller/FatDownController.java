package com.brb.fatdown.controller;


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

    private Profile current;


    @FXML
    public void initialize() {
        sexChoice.getItems().addAll(Sex.values());

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
            if (!newV.matches("\\d*")) ageField.setText(newV.replaceAll("[^\\d]", ""));
        });
        weightField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("[\\d,.]*")) weightField.setText(oldV);
        });
        heightField.textProperty().addListener(fieldListener);
        waistField.textProperty().addListener(fieldListener);
        neckField.textProperty().addListener(fieldListener);
        hipField.textProperty().addListener(fieldListener);
        sexChoice.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> recomputeCurrentBfAndAdjustSlider());

        targetSlider.valueProperty().addListener((o, oldV, newV) -> sliderValueLabel.setText(String.format("%.1f%%", newV.doubleValue())));

        calculateButton.setOnAction(e -> onCalculate());
        newProfileButton.setOnAction(e -> onNewProfile());
        saveProfileButton.setOnAction(e -> onSaveProfile());

        sliderValueLabel.setText(String.format("%.1f%%", targetSlider.getValue()));
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

            bf = Math.max(2.0, Math.min(60.0, bf));

            currentBfLabel.setText(String.format("%.2f%%", bf));
            targetSlider.setMin(5.0);
            targetSlider.setMax(Math.max(5.0, bf));
            if (targetSlider.getValue() > targetSlider.getMax()) targetSlider.setValue(targetSlider.getMax());
            sliderValueLabel.setText(String.format("%.1f%%", targetSlider.getValue()));

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

        repo.setLastUsedProfileId(p.getId());
    }

    private void frameFullTime(final FatBurnResult res, final boolean neatBoxSelected, final boolean lissBoxSelected,
                               final boolean mediumBoxSelected) {
        neatTimeLabel.setText(neatBoxSelected ? formatHours(res.getHoursNeat(), "NEAT") : "");
        lissTimeLabel.setText(lissBoxSelected ? formatHours(res.getHoursLiss(), "LISS") : "");
        mediumTimeLabel.setText(mediumBoxSelected ? formatHours(res.getHoursMedium(), "MEDIUM") : "");
    }

    private void framePerDayTime(final FatBurnResult res, final boolean neatBoxSelected, final boolean lissBoxSelected,
                                 final boolean mediumBoxSelected) {
        perDayNeat.setText(neatBoxSelected ? formatHours(res.getNeatPerDay(), "NEAT") : "");
        perDayLiss.setText(lissBoxSelected ? formatHours(res.getLissPerDay(), "LISS") : "");
        perDayMedium.setText(mediumBoxSelected ? formatHours(res.getMediumPerDay(), "MEDIUM") : "");
    }

    private String formatHours(double hours, String ex) {
        if (Double.isInfinite(hours)) return "—";
        int h = (int) Math.floor(hours);
        int m = (int) Math.round((hours - h) * 60);
        return String.format("%s: %d ч %d мин", ex, h, m);
    }
}