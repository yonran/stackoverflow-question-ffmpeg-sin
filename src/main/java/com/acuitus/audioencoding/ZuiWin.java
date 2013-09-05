package com.acuitus.audioencoding;

import java.awt.image.SampleModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class ZuiWin {
	private final Stage stage;
	final SimpleObjectProperty<AudioRecorderLoop> recorder = new SimpleObjectProperty<>();
	final SimpleObjectProperty<RecordingPreferences> recordingPreferencesProperty;
	final StringProperty directoryProperty;

	private ZuiWin(Stage stage, RecordingPreferences recordingPreferences) {
		this.stage = stage;
		this.recordingPreferencesProperty = new SimpleObjectProperty<>(recordingPreferences);
		this.directoryProperty = new SimpleStringProperty(Preferences.userNodeForPackage(getClass()).get("saveDirectory", null));
	}
	public static ZuiWin createWithUserPreferences(Stage stage) {
		RecordingPreferences recordingPreferences = RecordingPreferences.createFromPreferences();
		return new ZuiWin(stage, recordingPreferences);
	}
	public void start() {
		VBox vbox = new VBox();
		HBox hbox = new HBox();
		MenuBar menuBar = new MenuBar();
		vbox.getChildren().add(menuBar);
		vbox.getChildren().add(hbox);
		Button relayoutButton = new Button("Relayout");
		relayoutButton.setOnAction(new EventHandler<ActionEvent>() {@Override public void handle(ActionEvent actionEvent) {
			start();
		}});
		Menu menuFile = new Menu("_File");
		MenuItem menuSaveDirectory = new MenuItem("S_ave in");
		menuFile.getItems().addAll(menuSaveDirectory);
		Menu menuEdit = new Menu("_Edit");
		menuBar.getMenus().addAll(menuFile, menuEdit);
		MenuItem menuPreferences = new MenuItem("_Preferences");
		menuEdit.getItems().add(menuPreferences);
		menuPreferences.setOnAction(new EventHandler<ActionEvent>() {@Override public void handle(ActionEvent actionEvent) {
			openPreferencesWindow();
		}});
		Button recordButton = new Button();
		recordButton.textProperty().bind(Bindings.when(recorder.isNotNull()).then("Stop").otherwise("Record"));
		recordButton.setOnAction(new EventHandler<ActionEvent>() {@Override public void handle(ActionEvent actionEvent) {
			boolean startRecordign = recorder.get() == null;
			if (startRecordign)
				startRecording();
			else
				stopRecording();
		}});
		menuSaveDirectory.setOnAction(new EventHandler<ActionEvent>() {@Override public void handle(ActionEvent actionEvent) {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			String initialDirectory;
			if (null != (initialDirectory = directoryProperty.get()))
				directoryChooser.setInitialDirectory(new File(initialDirectory));
			File dir = directoryChooser.showDialog(stage);
			if (dir != null) {
				String absoluteDir = dir.getAbsolutePath();
				directoryProperty.set(absoluteDir);
				Preferences.userNodeForPackage(getClass()).put("saveDirectory", absoluteDir);
			}
			
		}});
		hbox.getChildren().add(relayoutButton);
		hbox.getChildren().add(recordButton);
		Slider slider = new Slider();
		slider.setOrientation(Orientation.VERTICAL);
		vbox.getChildren().add(slider);
		Scene scene = new Scene(vbox);
//		scene.getStylesheets().add(getClass().getResource("resources/styles.css").toExternalForm());
		stage.setScene(scene);

		stage.setOnCloseRequest(new EventHandler<WindowEvent> () {
			@Override public void handle(WindowEvent we) {
				onClose();
			}
		});

		stage.show();
	}
	private void startRecording() {
		final RecordingPreferences recordingPreferences = this.recordingPreferencesProperty.get();
		AudioEncoder audioEncoder;
		int bufferSampleCount = recordingPreferences.sampleRate * 1;
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmm");
			Date startDate = new Date();
			String prefix = "recording-" + dateFormat.format(startDate);
			File dir = directoryProperty.get() == null ? null : new File(directoryProperty.get());
			if (recordingPreferences.fileFormat == RecordingPreferences.SoundFormat.OGG) {
				OggVorbisEncoder.OggVorbisFactory oggVorbisFactory = OggVorbisEncoder.OggVorbisFactory.create();
				File file = new File(dir, prefix + ".ogg");
				int retryCount = 0;
				while (! file.createNewFile()) {
					file = new File(dir, prefix + "-" + ++retryCount + ".ogg");
				}
				@SuppressWarnings("resource")
				WritableByteChannel out = new FileOutputStream(file).getChannel();
				OggVorbisEncoder encoder = oggVorbisFactory.createEncoder(out, recordingPreferences.sampleRate, recordingPreferences.numChannels, .1f);
				@SuppressWarnings("resource")
				OggVorbisAudioEncoder myAudioEncoder = new OggVorbisAudioEncoder(encoder, recordingPreferences.numChannels, bufferSampleCount);
				audioEncoder = myAudioEncoder;
			} else {
				File file = new File(dir, prefix + ".wav");
				int retryCount = 0;
				while (! file.createNewFile()) {
					file = new File(dir, prefix + "-" + retryCount + ".wav");
				}
				RandomAccessFile wavFile = new RandomAccessFile(file, "rw");
				audioEncoder = WavAudioEncoder.writeWavHeaderAndCreate(wavFile, recordingPreferences.numChannels, recordingPreferences.sampleRate);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		TimingListener timingListener = new TimingListener() {
			long lastEmittedTimestamp;
			long lastCumulativeSampleCount;
			@Override public void timeSync(long cumulativeSampleCount, long systemTime) {
				long expectedTimestamp = lastEmittedTimestamp + (cumulativeSampleCount - lastCumulativeSampleCount) / (recordingPreferences.sampleRate/1000);
				if (lastCumulativeSampleCount != 0 || Math.abs(systemTime - expectedTimestamp) >= 10) {
					lastEmittedTimestamp = systemTime;
					lastCumulativeSampleCount = cumulativeSampleCount;
				}
			}
			@Override public void close() throws Exception {
			}
		};
		recorder.set(AudioRecorderLoop.start(recordingPreferences.numChannels, recordingPreferences.sampleRate, bufferSampleCount, timingListener, audioEncoder));
	}
	private void stopRecording() {
		AudioRecorderLoop recorder = this.recorder.get();
		if (recorder == null)
			return;
		this.recorder.set(null);
		recorder.stop();
		try {
			recorder.finished.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	private static class RecordingPreferences {
		public final SoundFormat fileFormat;
		public final int numChannels;
		public final int sampleRate;
		private RecordingPreferences(SoundFormat fileFormat, int numChannels, int sampleRate) {
			this.fileFormat = Objects.requireNonNull(fileFormat);
			this.numChannels = numChannels;
			this.sampleRate = sampleRate;
		}
		enum SoundFormat {
			OGG,
			WAV
		}
		public static RecordingPreferences createFromPreferences() {
			Preferences preferencesNode = Preferences.userNodeForPackage(RecordingPreferences.class);
			String fileFormatString = preferencesNode.get("soundFormat", "ogg");
			SoundFormat fileFormat = SoundFormat.valueOf(fileFormatString.toUpperCase());
			if (fileFormat == null)
				fileFormat = SoundFormat.OGG;
			int numChannels = preferencesNode.getInt("numChannels", 2);
			if (numChannels != 1 && numChannels != 2)
				numChannels = 2;
			int sampleRate = preferencesNode.getInt("sampleRate", 44100);
			if (sampleRate <= 0)
				sampleRate = 44100;
			return new RecordingPreferences(fileFormat, numChannels, sampleRate);
		}
		public void saveToPreferences() {
			Preferences preferencesNode = Preferences.userNodeForPackage(RecordingPreferences.class);
			preferencesNode.put("soundFormat", this.fileFormat.name().toLowerCase());
			preferencesNode.putInt("numChannels", this.numChannels);
			preferencesNode.putInt("sampleRate", this.sampleRate);
		}
		@Override public String toString() {return String.format("%s[soundFormat=%s, numChannels=%d, sampleRate=%d]", getClass().getName(), this.fileFormat.name().toLowerCase(), numChannels, sampleRate);}
	}
	private void openPreferencesWindow() {
		Stage preferencesWindow = new Stage();
		final ComboBox<RecordingPreferences.SoundFormat> soundFormatComboBox = new ComboBox<>();
		soundFormatComboBox.getItems().addAll(ZuiWin.RecordingPreferences.SoundFormat.values());
		final ComboBox<Integer> numChannelsComboBox = new ComboBox<>();
		numChannelsComboBox.getItems().addAll(1, 2);
		final ComboBox<Integer> sampleRateComboBox = new ComboBox<>();
		sampleRateComboBox.getItems().addAll(8000, 16000, 22050, 44100);
		
		RecordingPreferences recordingPreferences = recordingPreferencesProperty.get();
		soundFormatComboBox.getSelectionModel().select(recordingPreferences.fileFormat);
		numChannelsComboBox.getSelectionModel().select((Integer)recordingPreferences.numChannels);
		sampleRateComboBox.getSelectionModel().select((Integer)recordingPreferences.sampleRate);
		
		VBox vbox = new VBox();
		vbox.getChildren().addAll(soundFormatComboBox, numChannelsComboBox, sampleRateComboBox);
		
		EventHandler<ActionEvent> saveRunnable = new EventHandler<ActionEvent>() {@Override public void handle(ActionEvent actionEvent) {
			ZuiWin.RecordingPreferences.SoundFormat soundFormat = soundFormatComboBox.getSelectionModel().getSelectedItem();
			Integer numChannels = numChannelsComboBox.getSelectionModel().getSelectedItem();
			Integer sampleRate = sampleRateComboBox.getSelectionModel().getSelectedItem();
			RecordingPreferences newPreferences = new RecordingPreferences(soundFormat, numChannels, sampleRate);
			newPreferences.saveToPreferences();
			recordingPreferencesProperty.set(newPreferences);
		}};
		soundFormatComboBox.setOnAction(saveRunnable);
		numChannelsComboBox.setOnAction(saveRunnable);
		sampleRateComboBox.setOnAction(saveRunnable);
		
		preferencesWindow.setScene(new Scene(vbox));
		preferencesWindow.show();
		preferencesWindow.setTitle("Recording Preferences");
	}
	private void onClose() {
		stopRecording();
	}
}
