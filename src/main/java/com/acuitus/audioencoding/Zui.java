package com.acuitus.audioencoding;
import javafx.application.Application;
import javafx.stage.Stage;


public class Zui extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		ZuiWin creator = ZuiWin.createWithUserPreferences(stage);
		creator.start();
	}
}