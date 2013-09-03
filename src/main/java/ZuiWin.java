
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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class ZuiWin {
	private final Stage stage;
	final SimpleObjectProperty<SinVorbis.AudioRecorder> recorder = new SimpleObjectProperty<>();
	final SimpleObjectProperty<RecordingPreferences> recordingPreferencesProperty;

	private ZuiWin(Stage stage, RecordingPreferences recordingPreferences) {
		this.stage = stage;
		this.recordingPreferencesProperty = new SimpleObjectProperty<>(recordingPreferences);
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
		MenuItem menuOpen = new MenuItem("_Open");
		menuFile.getItems().addAll(menuOpen);
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
		menuOpen.setOnAction(new EventHandler<ActionEvent>() {@Override public void handle(ActionEvent actionEvent) {
			DirectoryChooser directoryChooser = new DirectoryChooser();
//			directoryChooser.setInitialDirectory("")
			File dir = directoryChooser.showDialog(stage);
		}});
		hbox.getChildren().add(relayoutButton);
		hbox.getChildren().add(recordButton);
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
		RecordingPreferences recordingPreferences = this.recordingPreferencesProperty.get();
		SinVorbis.AudioEncoder audioEncoder;
		int bufferSampleCount = recordingPreferences.sampleRate * 1;
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmm");
			Date startDate = new Date();
			String prefix = "recording-" + dateFormat.format(startDate);
			if (recordingPreferences.fileFormat == RecordingPreferences.SoundFormat.OGG) {
				SinVorbis.OggVorbisFactory oggVorbisFactory = SinVorbis.OggVorbisFactory.create();
				File file = new File(prefix + ".ogg");
				int retryCount = 0;
				while (! file.createNewFile()) {
					file = new File(prefix + "-" + ++retryCount + ".ogg");
				}
				@SuppressWarnings("resource")
				WritableByteChannel out = new FileOutputStream(file).getChannel();
				SinVorbis.OggVorbisEncoder encoder = oggVorbisFactory.createEncoder(out, recordingPreferences.sampleRate, recordingPreferences.numChannels, .1f);
				@SuppressWarnings("resource")
				SinVorbis.OggVorbisAudioEncoder myAudioEncoder = new SinVorbis.OggVorbisAudioEncoder(encoder, recordingPreferences.numChannels, bufferSampleCount);
				audioEncoder = myAudioEncoder;
			} else {
				File file = new File(prefix + ".wav");
				int retryCount = 0;
				while (! file.createNewFile()) {
					file = new File(prefix + "-" + retryCount + ".wav");
				}
				RandomAccessFile wavFile = new RandomAccessFile(file, "rw");
				audioEncoder = SinVorbis.WavAudioEncoder.writeWavHeaderAndCreate(wavFile, recordingPreferences.numChannels, recordingPreferences.sampleRate);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		recorder.set(SinVorbis.AudioRecorder.start(recordingPreferences.numChannels, recordingPreferences.sampleRate, bufferSampleCount, audioEncoder));
	}
	private void stopRecording() {
		SinVorbis.AudioRecorder recorder = this.recorder.get();
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
