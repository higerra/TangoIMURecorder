# Introduction
The tango app that record camera position, IMU sensor data and optionally WiFi signals. The app is still under development, expect unstable behavior and future changes.

# Compile and install
1. Open project in Android Studio
2. Connect the Tango phone with the computer and compile the app to the phone
# User interface
![alt text](https://github.com/higerra/TangoIMURecorder/raw/master/screenshot/image.png "User interface")
## Main Interface
  * The main interface is shown on the left.
  * The upper left part shows current configuration. See “Setting” section for details.
  * The upper right is the “Start/Stop” button. When file writing is enabled, a folder with the format “yyyymmddhhmmss” will be created under “Download” folder. All output files for this scanning will be steamed into that folder.
  * The canvas in the middle renders the real-time trajectory and view frustum (not shown in the above figure).
  * The lower middle shows the reading from various sensors. 
  * The grid shows IMU readings. Notice that readings from “Gravity” is omitted.
  * “Wifi Records” indicates how many scans have been made since the start; “Wifi hotspots” shows the number of hotspots responded from the latest scanning.
  * “Step Count” shows the step counts since starts.
  * The “Scan” button initiate a WiFi scanning when the “WiFi” is ON and is in the manual mode (see “Setting” for details). The button only appears when “Enable WiFi” is selected in the system settings.
## System settings
  * The setting interface is shown on the right. It can be opened by pressing the button on the top right corner.
  * Enable motion tracking: turn on Tango’s motion tracking.
  * Area learning mode: If enabled, the Tango system will try to memorize visual features of the scene in following scans and stores as “Area Description File (ADF)”. When the user scan the same scene later on, the corresponding ADF can be loaded. ADF can help with: 1. use the same global coordinate frame as when the ADF is created; 2. reduce drifting; 3. loop closure. For more about area learning, see here.
Load Area Description File: loading previously created ADF. After enabled, please select specify ADF by clicking “Select area description file”.
  * Select area description file: browse the list of existing ADF and select one.
  * Enable Wifi: record WiFi signals.
  * Continues Wifi scanning: when enabled, the system automatically initiate a scanning request every 3 seconds. The “Scan” button in the main interface will be disabled. When unchecked, the user manually initiate WiFi scanning by the “Scan” button.
Number of requests per scan: The number of requests sent for each scanning (manually or automatically).
  * Write files: when checked, a folder named “yyyymmddhhmmss” will be created under “Downloads” directory. When unchecked, no folder or files will be created.
  * Folder prefix: optional characters in front of the folder name. The final folder name will be “<prefix>-yyyymmddhmmss”.

# Common Workflows
## Pre-scan the area
### Prerequisites
  * Motion tracking ON
  * Area learning mode ON
### Steps
  * Start app, grant all required permissions.
  * Click “Start” button, hold steady for 3 seconds.
  * Scan the area.
  * Click “Stop” button, wait until the system indicates “Stopped”.
  * In the dialog popped up, specify the name for new ADF.
### Outcomes
  * A new ADF is created and store in the system scope (can be access by all apps).

## Collect pose and IMU data
### Prerequisites
  * Motion Tracking: ON
  * Area learning mode OFF
  * (Optional) Load pre-scanned ADF
  * Write files: ON
### Steps
  * Start app, grant all required permissions.
  * Properly place the device, click “Start” button, hold steady for 5 seconds.
  * Collect data.
  * When finishing walking, stay still for 5 seconds, then click “Stop”. Wait until the system indicates “Stopped”.
### Outcomes
  * Under Downloads/yyyymmddhhmmss: gyro.txt, acce.txt, linacce.txt, gravity.txt, magnet.txt, orientation.txt, pose.txt.

## Collect WiFi data
(You need to turn on the location service before receiving any Wifi scanning results).
### Prerequisites
  * In Android settings, turn on the Wifi and location service.
  * Enable Wifi: ON
  * (Optional) Load area description file. This will provide consistent global coordinate frame.
### Steps
  * Start the app, grant all permissions.
  * Click “Start” button, If “Continuous Wifi Scanning” is checked, the system will send a ground of  k scanning requests with the interval between each group a fixed amount of time. The time interval is configured with “Scan interval” option. If “Continuous Wifi Scanning” is unchecked, a button titled “Scan” will appear at the bottom right corner of the main screen. A ground of k scanning requests will be sent each time the user presses the button.
  * Collect data.
  * Click “Stop”, wait until the system indicates “Stopped”.
### Outcomes
  * Under “Downloads/yyyymmddhhmmss”: wifi.txt. The text file follows the format:
  * The first line is the comment
  * The second line stores number of scans.
  * From the fourth line to the end, scanning results will be stored one by one. Each scan results is stored as the number of APs in the scan followed by timestamp, BSSID and RSSI from each of the AP.
