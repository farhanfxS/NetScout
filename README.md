# 🛰️ NetScout

> Android cybersecurity toolkit for LAN discovery, nearby-device scanning, service detection and device intelligence.

NetScout is a cybersecurity-focused Android application built with Kotlin and Jetpack Compose. It brings network discovery, device intelligence, nearby-device discovery and security-oriented tooling into a mobile interface.

## ✨ Features

### 🔎 LAN Device Discovery

- Discover active devices on an authorized local network
- Identify IP addresses and hostnames when available
- Inspect reachable TCP ports and services
- Present discovered devices in a visual dashboard

### 🧠 Device Intelligence

NetScout analyzes available network evidence to classify discovered devices and identify possibilities such as:

- Android / mobile devices
- Windows computers
- Linux / Unix devices
- Network infrastructure
- Printers
- Possible IP cameras
- Possible IoT devices
- Other / unknown devices

Device classification is heuristic and should not be treated as definitive identification.

### 📡 Nearby Device Discovery

The app includes nearby-device intelligence using supported Android Wi-Fi and Bluetooth capabilities, including signal/proximity information where available.

RSSI-based proximity is an estimate and is affected by walls, interference, antenna characteristics and device hardware.

### 🌐 WAN / Network Information

NetScout includes network-information and WAN-oriented functionality for understanding the current network environment and for authorized assessment workflows.

### 🧪 CodeLab

The project includes a CodeLab interface for security-oriented development and testing workflows, including code review, lab/testing concepts and code-generation/patch-oriented workflows present in the current application.

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK 36
- Gradle
- Java 17
- Android Wi-Fi APIs
- Android Bluetooth APIs
- Android networking APIs

## 📱 Requirements

- Android 8.0 / API 26 or newer
- Wi-Fi hardware for Wi-Fi features
- Bluetooth hardware for Bluetooth features
- Appropriate Android permissions for the features being used

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/NetScout.git
cd NetScout
```

### 2. Open in Android Studio

Open the cloned `NetScout` folder and allow Gradle to synchronize.

### 3. Build the debug version

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

## 🔐 Android Permissions

Depending on Android version and the feature being used, NetScout may request permissions related to:

- Internet/network access
- Network state
- Wi-Fi state
- Nearby Wi-Fi devices
- Location
- Bluetooth scanning
- Bluetooth connection

Android permission requirements can change between Android versions.

## 📸 Screenshots

> Screenshots will be added to the `screenshots/` directory.

### Main Interface

![NetScout Home](screenshots/home.png)

### LAN Scan

![LAN Scan](screenshots/lan-scan.png)

### Detected Devices

![Detected Devices](screenshots/devices.png)

### Device Details

![Device Details](screenshots/device-details.png)

### Nearby Discovery

![Nearby Discovery](screenshots/proximity.png)

### Security / Assessment

![Security Assessment](screenshots/security.png)

## 🗂️ Project Structure

```text
NetScout/
├── app/
│   └── src/main/
│       ├── java/com/netscout/
│       │   ├── app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── CodeLab.kt
│       │   │   ├── model/
│       │   │   ├── scanner/
│       │   │   └── ui/theme/
│       │   └── ProximityScreen.kt
│       └── res/
├── screenshots/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
├── README.md
├── LICENSE
└── .gitignore
```

## ⚠️ Responsible Use

NetScout is intended for cybersecurity education, network administration, research and authorized security testing.

Only scan networks, devices and systems that you own or have explicit permission to assess.

Do not use NetScout to access, disrupt, interfere with or test systems without authorization.

The author is not responsible for misuse of the software.

## 🗺️ Roadmap

- [ ] Improved device fingerprinting
- [ ] Better manufacturer identification
- [ ] Improved camera detection
- [ ] More service identification
- [ ] Improved nearby-device intelligence
- [ ] Scan history
- [ ] Exportable security reports
- [ ] Enhanced visualization
- [ ] Improved assessment workflows
- [ ] Jarvis integration

## 👨‍💻 Author

**Farhan Ahmed**

Cybersecurity / Information Security Enthusiast

## 📄 License

NetScout is released under the MIT License. See [LICENSE](LICENSE) for details.
