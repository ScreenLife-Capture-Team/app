# ScreenLife Capture Android App

### Changelog
#### v. 1.1.0

- Fixed multiple bugs regarding QR scanner, starting issue on Pixel devices
- Added a descriptor field in file names

#### v. 1.1.0
#### 28 Feb 2023

- Added notice screens when user intentionally pause/resume recording
- Updated gradle for SDK 32 (Android 12), fixed issue with intent passing for android 12
- Updated gradle and fixed method calls for notification and datetime utils for SDK 25 and below (Android 7 and below)

This repo contains the Android Application used in the ScreenLife Capture study. The application allows participants to record and upload screenshots taken every X number of seconds. The general layout of the code is explained below.

## Usage guide

### Installing via Android Studio (debug mode)

Refer to the [official guide here](https://developer.android.com/studio/run/device) for running an app on a physical device.

Summary of steps:

- Ensure your Android device has USB debugging mode turned on.
- Connect your Android device using data cable and select file transfer mode. Android Studio should automatically register your device.
- In Android Studio, select your device from the device list on the top toolbar. Click "Run app". The app should be installed in your device.

### Packaging APK for distribution



## App structure

### Activities

| Activity Name    | Purpose                                                      |
| ---------------- | ------------------------------------------------------------ |
| RegisterActivity | Handles registration for new users.                          |
| MainActivity     | Contains the main interface of the app, allowing participants to start/stop the screen capture. |
| DevToolsActivity | Contains tools to tweak how the app works, including the number of images to send per batch, the number of batches to send in parallel etc. |

### Service

| Service Name   | Purpose                                                      |
| -------------- | ------------------------------------------------------------ |
| CaptureService | Responsible for capturing screenshots every X number of seconds. Runs continously throughout the duration of the study. |
| UploadService  | Responsible for uploading of screenshots to the cloud functions. Is triggered at certain times by `UploadScheduler` |

### Other Files

| File Name          | Purpose                                                      |
| ------------------ | ------------------------------------------------------------ |
| Constants          | Contains the constants used throughout the application.      |
| Batch              | Contains a "batch" of files, used by `UploadService`.        |
| Encryptor          | Used during the encryption process by `CaptureService`.      |
| InfoDialog         | The dialog that is shown when the "information" button is pressed on the main activity. |
| InternetConnection | A set of functions to check if the device is connected to the internet, and through what type of connection (WiFi vs mobile data). |
| Logger             | Utility functions to save logs to SharedPreferences.         |
| UploadScheduler    | Schedules the `UploadService` at certain times a day.        |



## Constants

Most app-related constants are located in the `Constants` file. The constants are explained below.

| Constant Name       | Explanation                                   |
| ------------------- | --------------------------------------------- |
| REGISTER_ADDRESS    | The address of the "register" cloud function. |
| UPLOAD_ADDRESS      | The address of the "upload" cloud function.   |
| COUNT_ADDRESS       | The address of the "count" cloud function.    |
| BATCH_SIZE_DEFAULT  | TODO                                          |
| MAX_TO_SEND_DEFAULT | TODO                                          |
| MAX_BATCHES_TO_SEND | TODO                                          |
| REQ_TIMEOUT         | TODO                                          |


