# Real-time Drowsy Detection Android App

## Overview
This Android app helps in detecting drowsiness in drivers in real-time. Using YOLOv5 and TensorFlow Lite (TFLite), the app monitors the driver's face and alerts the driver with a sound notification if drowsiness is detected. This can potentially help in preventing accidents caused by drowsy driving.

## Features
- Real-time face detection for driver monitoring.
- Drowsiness detection using machine learning models.
- Alert sound triggered when drowsiness is detected.
- Lightweight and efficient, using TensorFlow Lite for performance on mobile devices.

## Technologies Used
- **Kotlin**: Main programming language for Android development.
- **YOLOv5**: Used for real-time face detection and drowsiness monitoring.
- **TensorFlow Lite (TFLite)**: Optimized model for on-device inference.
- **Android SDK**: For building the mobile app and managing device sensors.
  
## Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/drowsy-detection-app.git
2. Open the project in Android Studio.
3. Build and run the app on your Android device or emulator.
4. Make sure to enable the camera permissions in your Android device to allow face detection.

## Usage
- Launch the app and grant necessary permissions for camera and audio.
- The app will start detecting the driver's face using the camera in real-time.
- If the app detects signs of drowsiness, it will trigger an alert sound to notify the driver.
- You can stop the monitoring by closing the app or pressing the stop button.

## Model Training
- The model used for drowsiness detection is based on the YOLOv5 architecture.
- You can train the model on a custom dataset using PyTorch and then convert it to TensorFlow Lite format for use on the Android device.
- Follow these steps for training and converting the model:
- Train YOLOv5 on a labeled dataset of driver faces with drowsiness signs.
- Convert the trained model to TensorFlow Lite using the TensorFlow Lite Converter.

## License
This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments
- YOLOv5 for the object detection model.
- TensorFlow Lite for optimized on-device inference.
- OpenCV for face detection and tracking.