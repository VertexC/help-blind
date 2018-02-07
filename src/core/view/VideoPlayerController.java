package core.view;

import core.MainApp;
import core.util.Utilities;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
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
    private int  numberOfSamplesPerColumn;
    private MediaPlayer mediaPlayer;
    private double[] freq; //frequency to play the sound

    @FXML
    private void initialize() {
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

        // open the click sound
        String musicFile = "source/sound/Button_Push.mp3";
        Media sound = new Media(new File(musicFile).toURI().toString());
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setVolume(0.4);
        mediaPlayer.setCycleCount(100);
        mediaPlayer.play();

    }

    // deal with the data
    @FXML
    protected void playVideo(ActionEvent event) {
        if (!this.playVideo) {
            this.playVideo = true;
            // open the video
            this.capture = new VideoCapture("source/video/test.mp4");
            if (this.capture != null && this.capture.isOpened()) {
                // create a frameGrabber
                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run() {
                        // grabe the frame from the video
                        Mat frame = grabFrame();
                        if(!frame.empty()){
                            // convert
                            Image imageToshow = Utilities.mat2Image(frame);
                            // add a click sound before update the frame
                            updateImageView(currentFrame, imageToshow);
                            playClick();
                        } else{
                            // end of the video
                            capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
                        }

                    }
                };
                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
                this.button.setText("Stop");
            } else {
                // the capture cannot be opened
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

    private void updateImageView(ImageView view, Image image) {
        Utilities.onFXThread(view.imageProperty(), image);
    }
    private void playClick(){
        mediaPlayer.play();
    }

    private void playFrame(Mat image) throws LineUnavailableException{
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


}