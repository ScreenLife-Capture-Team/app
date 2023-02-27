# ScreenLife Capture Android App

# Dev branch notes:

This branch (development) is for beta testing of new changes. Only merge with main when confirmed stable

### Changelog


#### v. 1.1.0

- Added notice screens when user intentionally pause/resume recording
- Updated gradle for SDK 32 (Android 12), fixed issue with intent passing for android 12
- Updated gradel and fixed method calls for notification and datetime utils for SDK 25 and below (Android 7 and below)



This repo contains the Android Application used in the ScreenLife Capture study. The application allows participants to record and upload screenshots taken every X number of seconds. The general layout of the code is explained below.

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

Shield: [![CC BY-NC-SA 4.0][cc-by-nc-sa-shield]][cc-by-nc-sa]

This work is licensed under a
[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License][cc-by-nc-sa].

[![CC BY-NC-SA 4.0][cc-by-nc-sa-image]][cc-by-nc-sa]

[cc-by-nc-sa]: http://creativecommons.org/licenses/by-nc-sa/4.0/
[cc-by-nc-sa-image]: https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png
[cc-by-nc-sa-shield]: https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg
