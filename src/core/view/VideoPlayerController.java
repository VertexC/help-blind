package core.view;

import core.MainApp;
import core.util.Utilities;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VideoPlayerController {

    @FXML
    private Button button;
    @FXML
    private ImageView currentFrame;
    @FXML
    private Slider slider;
    @FXML
    private ImageView histogram;

    // for video
    private VideoCapture capture = new VideoCapture();
    private boolean playVideo = false;

    private ScheduledExecutorService timer;

    // parameter to resize and play the frame with sound
    private int width;
    private int height;
    private int sampleRate;
    private int sampleSizeInBits;
    private int numberOfChannels;
    private int quantizationLevel;
    private int numberOfSamplesPerColumn;
    private double[] freq; //frequency to play the sound

    // for click sound
    private MediaPlayer mediaPlayer;
    private Clip clip;

    // parameter for hitogram
    private int histoWidth;
    private int histoHeight;

    // for slider
    private boolean sliderDragged;

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
        quantizationLevel = 16;
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
    }

    // deal with the data
    @FXML
    protected void playVideo(ActionEvent event) {
        // open the click sound
        if (!this.playVideo) {
            this.playVideo = true;
            // open the video
            // this.capture = new VideoCapture("source/video/test.mp4");
            setCapture();
            if (this.capture.isOpened()) {
                // create a frameGrabber
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        // detect slider
                        if (sliderDragged) {
                            System.out.println(sliderDragged);
                            // after slider is dragged, set the video capture to that frame
                            double currentSliderPosition = slider.getValue();
                            double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
                            double toSetFrameNumber;
                            toSetFrameNumber = (currentSliderPosition - slider.getMin())
                                    / (slider.getMax() - slider.getMin()) * totalFrameCount;
                            capture.set(Videoio.CAP_PROP_POS_FRAMES, toSetFrameNumber);
                            sliderDragged = false; // set back to false
                        } else {
                            // grab the frame from the video
                            Mat frame = grabFrame();
                            if (!frame.empty()) {
                                // play the clip
                                clip.stop();
                                clip.setFramePosition(0);
                                clip.start();
                                // convert
                                Image imageToshow = Utilities.mat2Image(frame);
                                updateVideoView(currentFrame, imageToshow);
                                updateHistogramView(imageToshow);
                                double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
                                double totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
                                slider.setValue(currentFrameNumber / totalFrameCount * (slider.getMax() - slider.getMin()));

                            } else {
                                // end of the video
                                capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
                            }
                        }
                    }
                };
                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 1000, TimeUnit.MILLISECONDS);
                this.button.setText("Stop");
            } else {
                // the capture cannot be opened
                // creat a warning dialog here
                System.err.println("Cannot open the video at " + "source/video/test.mp4");

            }

        } else {
            // the camera is not active
            this.playVideo = false;
            // update the button content
            this.button.setText("Play");
            // stop the timer
            this.stopAcquisition();
        }
    }

    @FXML
    protected void slidDrag() {
        // After drag done
        if(capture.isOpened()){
            this.sliderDragged = true;
        }
        // detect whether the capture is opened
    }

    public void setCapture(){
        this.capture = new VideoCapture(mainApp.getOpenedFilePath());
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

    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.err.println("Exception in stopping the frame capture, trying to release the video..." + e);
            }
        }

        if (this.capture.isOpened()) {
            this.capture.release();
        }
    }

    private void updateVideoView(ImageView view, Image image) {
        Utilities.onFXThread(view.imageProperty(), image);
    }

    private void updateHistogramView(Image image) {
    }

    /*
        This method fails to play the audio.
     */
    private void playClick() {
        String musicFile = "source/sound/electric-typewriter.mp3";
        Media sound = new Media(new File(musicFile).toURI().toString());
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setStartTime(Duration.millis(1));
        mediaPlayer.setStopTime(Duration.millis(10));
        mediaPlayer.play();
        int count = 10000;
        while (count > 0) {
            count--;
        }
        mediaPlayer.stop();
    }
    /*
        The precoded method to play the image
     */
    private void playFrame(Mat image) throws LineUnavailableException {
        if (!image.empty()) {
            // convert RGB into greyscale
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

            // resize the image
            Mat resizedImage = new Mat();
            Imgproc.resize(grayImage, resizedImage, new Size(width, height));

            // quantization
            double[][] roundedImage = new double[grayImage.rows()][grayImage.cols()];
            for (int row = 0; row < resizedImage.rows(); row++) {
                for (int col = 0; col < resizedImage.cols(); col++) {
                    roundedImage[row][col] = (double) Math.floor(resizedImage.get(row, col)[0] / (Math.pow(2, 8) / quantizationLevel)) / quantizationLevel;
                }
            }

            // I used an AudioFormat object and a SourceDataLine object to perform audio output. Feel free to try other options
            AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, numberOfChannels, true, true);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
            sourceDataLine.open(audioFormat, sampleRate);
            sourceDataLine.start();

            for (int col = 0; col < width; col++) {
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
            }
            sourceDataLine.drain();
            sourceDataLine.close();

        } else {
            System.err.println("Cannot play sounds from frame as it is empty.");
        }
    }

    public void setMainApp(MainApp mainApp){
        this.mainApp = mainApp;
    }

}