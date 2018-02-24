### CMPT365 Assignment2 Report
#### Group Member: Bowen Chen, Haipeng Li

Required Features:
- [x] use the java skeleton
- [x] can loads a video
- [x] user is able to select whatever video to be loaded
- [x] click sound between each frame 
- [x] convert image of each frame into sound

Extra Features:
- [x] add play/stop button
- [x] add slider, slider moves as video plays, and user can drag slider to change the frame to be played
- [x] add frame count prompt, shown as currentFrame/totalFrame
- [x] add histogram, which shows the image's RBG information of the current frame
- [x] add CSS, to beautify the UI

#### Screen Shots of the Program - Leo


#### Implementation

##### File Selector
<img src="\image\fileSelector.png" width="200">
<div style="font-size:13px">In RootLayoutController,add open event on "Open Video" menu item. When user select the file from the dialog, get the file path and store it in public variable in mainApp.c</div>

```java
@FXML
private void openVideo(){
    Stage dialogStage = new Stage();
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Open the Source File");
    File file = fileChooser.showOpenDialog(dialogStage);
    if (file != null){
        mainApp.setOpenedFilePath(file.getPath());
    }
}   
```

##### Play the video - Leo

##### Slider
<img src="\image\slider.png" width="">
<div style="font-size:13px"> Add a listener to slider, when its value(position) changes, set the frame of opened video according to the slider's position. </div>

```java
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
```

##### Click Sound
<div style="font-size:13px">For click sound between frames, Clip would be a good implementation as the sounds is relatively short. We download a 1s click sound .wav and load it into clip. </div>

```java
// load in click .wav
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
```
<div style="font-size:13px">In each run of video frame, the clip is played before the image.</div>

```java
// play the click
Runnable frameGrabber = new Runnable() {
    @Override
    public void run() {
        ...
        // play the clip
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        ...
    }
}
```
##### Image to Sound - Leo

##### Histogram
<img src="\image\histogram.png" width="200">

<div style="font-size:13px">The main idea of histogram is to show the disribution of the color value across the image. For histogram, after getting the quantilized color value of resized grey image, we group them into different catagries according to the range of value.
</div>

```java
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

    private XYChart.Series<String, Integer> createHistogramSeries(int[] group) {
    XYChart.Series<String, Integer> series = new XYChart.Series<String, Integer>();
    for (int i = 0; i < group.length; i++) {
        XYChart.Data<String, Integer> data = new XYChart.Data<String, Integer>(valueRange.get(i), group[i]);
        series.getData().add(data);
    }
    return series;
}
```

##### CSS - Leo


#### Discussion

