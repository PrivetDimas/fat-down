package com.brb.fatdown;

import com.brb.fatdown.controller.FatDownController;
import com.brb.fatdown.repo.ProfileRepository;
import com.brb.fatdown.service.FatDownService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class FatDownApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        ProfileRepository repo = new ProfileRepository();
        FatDownService service = new FatDownService();

        FXMLLoader loader = new FXMLLoader(FatDownApplication.class.getResource("main-view.fxml"));
        loader.setControllerFactory(type -> {
            if (type == FatDownController.class) {
                return new FatDownController(repo, service);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Scene scene = new Scene(loader.load(), 740, 530);
        stage.setTitle("FatDown");
        stage.setScene(scene);
        stage.show();
    }
}
