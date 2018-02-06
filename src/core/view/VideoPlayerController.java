package core.view;

import core.MainApp;
import core.util.Utilities;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


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
    private double[] freq; //frequency to play the sound

    @FXML
    private void initialize() {
        width = 64;
        height = 64;

        // set the frequency on each col
        freq = new double[height];
        freq[height / 2 - 1] = 440.0; // 440KHZ - Sound of A (La)
        for (int m = height / 2; m < height; m++) {
            freq[m] = freq[m - 1] * Math.pow(2, 1.0 / 12.0);
        }
        for (int m = height / 2 - 2; m >= 0; m--) {
            freq[m] = freq[m + 1] * Math.pow(2, -1.0 / 12.0);
        }
    }

    // deal with the data
    @FXML
    protected void playVideo(ActionEvent event) {
        if (!this.playVideo) {
            this.playVideo = true;
            // open the video
            this.capture = new VideoCapture("source/video/test.mp4");
            if (this.capture.isOpened()) {
                // create a frameGrabber
                Runnable frameGrabber = new Runnable() {
                    @Override
                    public void run() {
                        // grabe the frame from the video
                        Mat frame = grabFrame();
                        // convert
                        Image imageToshow = Utilities.mat2Image(frame);
                        // add a click sound before update the frame
                        updateImageView(currentFrame, imageToshow);
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

    private void playFrame(Mat frame) {
        if (!frame.empty()) {
            // convert RGB into greyscale
            Mat grayImage = new Mat();
            Imgproc.cvtColor(frame, grayImage, Imgproc.COLOR_BGR2GRAY);

            // resize the image
            Mat resizedImage = new Mat();
            Imgproc.resize(grayImage, resizedImage, new Size(width, height));

            // quantization
            double[][] roudedImage = new double[grayImage.rows()][grayImage.cols()];
            for (int row = 0; row < resizedImage.rows(); row++) {
                for (int col = 0; col < resizedImage.cols(); col++) {
                    roudedImage[row][col] = (double) Math.floor(resizedImage.get(row, col)[0] / (Math.pow(2, 8) / quantizationLevel)) / quantizationLevel;
                }
            }

            // paly the audio
        } else {
            System.err.println("Cannot play sounds from frame as it is empty.");
        }
    }


}