package core.view;

import core.util.Utilities;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VideoPlayerController {

    @FXML
    private Button button;
    @FXML
    private ImageView currentFrame;

    private VideoCapture capture = new VideoCapture();
    private boolean playVideo = false;

    private ScheduledExecutorService timer;


    // deal with the data
    @FXML
    protected void playVideo(ActionEvent event) {
        capture = new VideoCapture("source/video/test.mp4");
        if (!this.playVideo) {

            // create a frameGrabber
            Runnable frameGrabber = new Runnable() {
                @Override
                public void run() {
                    // ignore skeleton for a moment
                    Mat frame = grabFrame();
                    // convert
                    Image imageToshow = Utilities.mat2Image(frame);
                    updateImageView(currentFrame, imageToshow);
                }

                Executors.newSingleThreadScheduledExecutor()
            };

            this.button.setText("Stop");
        } else {
            // the camera is not active at this point
            this.playVideo = false;
            // update the button content
            this.button.setText("Play");
        }
    }

    private  Mat grabFrame(){
        Mat frame = new Mat();
        // check if capture is opened
        if(this.capture.isOpened()){
            try {
                // get the current frame
                this.capture.read(frame);

                // if the frame is not empty
                if(!frame.empty()) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                }
            }
            catch (Exception e){
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }
        return frame;
    }

    private void updateImageView(ImageView view, Image image){
        Utilities.onFXThread(view.imageProperty(),image);
    }


}