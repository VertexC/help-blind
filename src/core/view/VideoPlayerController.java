package core.view;

import core.MainApp;
import core.util.Utilities;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VideoPlayerController {

    @FXML
    private Button playVideoButton;
    @FXML
    private ImageView currentFrame;
    @FXML
    private Slider slider;
    @FXML
    private BarChart<String, Integer> histogram;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private Label processLabel;
    @FXML
    private StackPane stackPane;
    // for histogram
    private ObservableList<String> valueRange = FXCollections.observableArrayList();

    // for video
    private VideoCapture capture = new VideoCapture();
    private boolean playVideoStatus = false;
    private int videoTimerPeriod = 4000;  // 4000 ms
    private int audioTimerPeriod = 40;  // 40 ms
    private int framewidth = 700;
    private int frameheight = 400;
    private ScheduledExecutorService videoTimer;
    private ScheduledExecutorService soundTimer;
    private ScheduledExecutorService chartTimer;
    // parameter to resize and play the frame with sound and histogram
    private int width;
    private int height;
    private int sampleRate;
    private int sampleSizeInBits;
    private int numberOfChannels;
    private int numberOfQuantizationLevels;
    private int numberOfSamplesPerColumn;
    private double[] freq; //frequency to play the sound

    // for click sound
    private MediaPlayer mediaPlayer;
    private Clip clip;

    // for exchange data
    private MainApp mainApp;

    @FXML
    private void initialize() throws LineUnavailableException {
        // for audio
        width = 64;
        height = 64;

        sampleRate = 8000;
        sampleSizeInBits = 8;
        numberOfChannels = 1;
        numberOfQuantizationLevels = 16;
        numberOfSamplesPerColumn = 500;

        // set the frequency on each col
        freq = new double[height];
        freq[height / 2 - 1] = 440.0; // 440KHZ - Sound of A (La)
        for (int m = height / 2; m < height; m++) {
            freq[m] = freq[m - 1] * Math.pow(2, 1.0 / 12.0);
        }
        for (int m = height / 2 - 2; m >= 0; m--) {
            freq[m] = freq[m + 1] * Math.pow(2, -1.0 / 12.0);
        }

        // for click sound
        File soundFile = new File("source/sound/briefcase-lock-5.wav");
        clip = AudioSystem.getClip();
        try {
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(soundFile);
            clip.open(inputStream);
        } catch (IOException e) {
            System.err.println("Cannot open the clip file.");
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Wrong file format for clip.");
        }

        // for video capture
        slider.valueChangingProperty().addListener(new ChangeListener<Boolean>() {
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (capture.isOpened()) {
                    double currentSliderPosition = slider.getValue();
                    double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
                    double toSetFrameNumber;
                    toSetFrameNumber = (currentSliderPosition - slider.getMin())
                            / (slider.getMax() - slider.getMin()) * totalFrameCount;
                    capture.set(Videoio.CAP_PROP_POS_FRAMES, toSetFrameNumber);
                }
            }
        });

        // for histogram
        String[] ranges = new String[numberOfQuantizationLevels];
        for (int i = 0; i < numberOfQuantizationLevels; i++) {
            ranges[i] = Integer.toString(i);
        }
        valueRange.addAll(Arrays.asList(ranges));
        xAxis.setCategories(valueRange);
        xAxis.setLabel("QR-level");
        yAxis.setLabel("Count");
    }

    private double[][] processImage(Mat image) {
        // grayscale, resize, quantization
        //
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        // resize the image
        Mat resizedImage = new Mat();
        Imgproc.resize(grayImage, resizedImage, new Size(width, height));

        // quantization
        // compressing a range of values to a single quantum value
        double[][] roundedImage = new double[resizedImage.rows()][resizedImage.cols()];
        for (int row = 0; row < resizedImage.rows(); row++) {
            for (int col = 0; col < resizedImage.cols(); col++) {
                roundedImage[row][col] = (double) Math.floor(resizedImage.get(row, col)[0] / (256 / numberOfQuantizationLevels)) / numberOfQuantizationLevels;
            }
        }
        return roundedImage;
    }

    private void playAudio(Mat image) throws LineUnavailableException {
        if (!image.empty()) {
            double[][] roundedImage = processImage(image);
            // I used an AudioFormat object and a SourceDataLine object to perform audio output. Feel free to try other options
            AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, numberOfChannels, true, true);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.open(audioFormat, sampleRate);
            sourceDataLine.start();
            // begin to write to sourceDataLine and play

            class AudioPlayer implements Runnable {
                private int col = 0;
                private double[][] roundedImage;
                private SourceDataLine sourceDataLine;

                public AudioPlayer(double[][] roundedImage, SourceDataLine sourceDataLine) {
                    this.roundedImage = roundedImage;
                    this.sourceDataLine = sourceDataLine;
                }

                private void writeAudio(double[][] roundedImage, SourceDataLine sourceDataLine) {
                    // for each row:
                    // generate audio buffer, and play (write)
                    col++;
                    if (col < width) {
                        byte[] audioBuffer = new byte[numberOfSamplesPerColumn];
                        for (int t = 1; t <= numberOfSamplesPerColumn; t++) {
                            double signal = 0;
                            for (int row = 0; row < height; row++) {
                                int m = height - row - 1; // Be sure you understand why it is height rather width, and why we subtract 1
                                int time = t + col * numberOfSamplesPerColumn;
                                double ss = Math.sin(2 * Math.PI * freq[m] * (double) time / sampleRate);
                                signal += roundedImage[row][col] * ss;
                            }
                            double normalizedSignal = signal / height; // signal: [-height, height];  normalizedSignal: [-1, 1]
                            audioBuffer[t - 1] = (byte) (normalizedSignal * 0x7F); // Be sure you understand what the weird number 0x7F is for
                        }
                        sourceDataLine.write(audioBuffer, 0, numberOfSamplesPerColumn);
                    } else {
                        // want to modify UI in fx. must use Platform.runLater();
                        Platform.runLater(() -> {
                                    sourceDataLine.drain();
                                    sourceDataLine.close();
                                }
                        );
                    }
                }

                public void run() {
                    this.writeAudio(this.roundedImage, this.sourceDataLine);
                }
            }
            this.soundTimer = Executors.newSingleThreadScheduledExecutor();
            AudioPlayer colPlayer = new AudioPlayer(roundedImage, sourceDataLine);
            this.soundTimer.scheduleAtFixedRate(colPlayer, 0, this.audioTimerPeriod, TimeUnit.MILLISECONDS);
        } else {

        }

    }

    // deal with the data
    @FXML
    protected void playVideo(ActionEvent event) {
        // open the click sound
        if (!this.playVideoStatus) {
            // open the video
            // this.capture = new VideoCapture("source/video/test.mp4");
            setCapture();
            if (this.capture.isOpened()) {
                this.playVideoStatus = true;
                // create a frameGrabber
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        // grab the frame from the video
                        Mat frame = grabFrame();
                        double ratio = frame.height() * 1.0 / frame.width();
                        Mat resizedFrame = new Mat();
                        Imgproc.resize(frame, resizedFrame, new Size(framewidth, (framewidth * ratio) > frameheight ? frameheight : (framewidth * ratio)));
                        if (!resizedFrame.empty()) {
                            // play audio
                            stackPane.setPrefWidth(framewidth);
                            stackPane.setPrefHeight(frameheight);
                            Image imageToshow = Utilities.mat2Image(resizedFrame);
                            currentFrame.setFitWidth(framewidth);
                            updateVideoView(currentFrame, imageToshow);
                            updateHistogramView(resizedFrame);
                            try {
                                playAudio(resizedFrame);
                            } catch (Exception e) {
                                System.err.println("Exception: " + e);
                            }
                            double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
                            double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
                            slider.setValue(currentFrameNumber / totalFrameCount * (slider.getMax() - slider.getMin()));
                            Platform.runLater(() -> {
                                processLabel.setText("" + (int) currentFrameNumber + "/" + (int) totalFrameCount);
                            });
                            // play the clip
                            clip.stop();
                            clip.setFramePosition(0);
                            clip.start();
                        } else {
                            // end of the video
                            capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
                        }
                    }
                };
                this.videoTimer = Executors.newSingleThreadScheduledExecutor();
                this.videoTimer.scheduleAtFixedRate(frameGrabber, 0, videoTimerPeriod, TimeUnit.MILLISECONDS);
                this.playVideoButton.setText("Stop");
            } else {
                // the capture cannot be opened
                // how alert
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Warning Dialog");
                alert.setContentText("The capture is not open!");
                alert.showAndWait();
                // System.err.println("Cannot open the video at " + "source/video/test.mp4");
            }
        } else {
            // the camera is not active
            // stop the videoTimer
            this.stopPlaying();
        }
    }

    public void setCapture() {
        String path = mainApp.getOpenedFilePath();
        if (path != null) {
            this.capture.open(mainApp.getOpenedFilePath());
        }
    }

    private Mat grabFrame() {
        Mat frame = new Mat();
        // check if capture is opened
        if (this.capture.isOpened()) {
            try {
                // get the current frame
                this.capture.read(frame);

                // if the frame is not empty
//                if(!frame.empty()) {
//                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
//                }
            } catch (Exception e) {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        return frame;
    }


    private void stopPlaying() {
        System.out.println("Stop Timer.");
        this.playVideoStatus = false;
        // update the playVideoButton content
        this.playVideoButton.setText("Play");
        stopTimer(soundTimer);
        stopTimer(videoTimer);
        releaseVideo();
    }

    private void stopTimer(ScheduledExecutorService timer) {
        if (timer != null && !timer.isShutdown()) {
            try {
                timer.shutdown();
                timer.awaitTermination(videoTimerPeriod, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                System.err.println("exception: " + e);
            }
        }
    }

    private void releaseVideo() {
        if (this.capture.isOpened()) {
            this.capture.release();
        }
    }

    private void updateVideoView(ImageView view, Image image) {
        Utilities.onFXThread(view.imageProperty(), image);
    }

    private void updateHistogramView(Mat resizedFrame) {
        double[][] processedImage = processImage(resizedFrame);
        // make the frame into grey
        // build up the histogram according to its grey value
        int group[] = new int[numberOfQuantizationLevels]; // 0 - 15, 16 - 31, ..., 240 - 255
        for (int i = 0; i < numberOfQuantizationLevels; i++) {
            group[i] = 0;
        }
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                group[(int) Math.floor(processedImage[i][j] * numberOfQuantizationLevels)] += 1;
            }
        }
        XYChart.Series<String, Integer> series = createHistogramSeries(group);
        // put the data into bar chart
        Platform.runLater(() -> {
                    histogram.getData().setAll(series);
                }
        );


    }

    private XYChart.Series<String, Integer> createHistogramSeries(int[] group) {
        XYChart.Series<String, Integer> series = new XYChart.Series<String, Integer>();
        for (int i = 0; i < group.length; i++) {
            XYChart.Data<String, Integer> data = new XYChart.Data<String, Integer>(valueRange.get(i), group[i]);
            series.getData().add(data);
        }
        return series;
    }

    /*
        The precoded method to play the image
     */
    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

}