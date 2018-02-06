package core.view;

import core.util.Utilities;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.opencv.core.Mat;
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
    @FXML
    private void initialize(){

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

        if (this.capture.isOpened()){
            this.capture.release();
        }
    }

    private void updateImageView(ImageView view, Image image) {
        Utilities.onFXThread(view.imageProperty(), image);
    }

    private void playFrame(Mat frame){

    }


}