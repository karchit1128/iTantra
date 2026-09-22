# iTantra Rescue 🛟

iTantra is an advanced, fully off-grid Android application built specifically for disaster response and emergency rescue operations. When cellular networks and internet connectivity fail, iTantra creates a decentralized Wi-Fi Aware Mesh Network, allowing rescue workers and victims to communicate seamlessly, log live triage data, and broadcast emergency SOS signals.

## 🚀 Key Features

*   **Off-Grid Mesh Networking:** Utilizes Android's Wi-Fi Aware (NAN) to create a self-healing, device-to-device mesh network without requiring routers, SIM cards, or internet access.
*   **Walkie-Talkie (Push-To-Talk):** Real-time voice communication over the mesh network. Includes automatic Voice Activity Detection (VAD).
*   **Live Triage Sync:** Log victim information (Priority RED, YELLOW, GREEN), injuries, and GPS coordinates. The data syncs across all connected mesh nodes using a Gossip Protocol.
*   **Offline AI Speech-To-Text & Text-To-Speech:** Integrated with Sherpa-ONNX for completely offline voice transcriptions and voice synthesis in multiple languages (including English and Hindi).
*   **Smart Auto-Translation:** Automatically translates incoming messages to the user's native language.
*   **Hands-Free Wake Word & SOS:** Say the wake word to automatically lock the channel and broadcast an emergency SOS with your exact GPS location.
*   **Hardware PTT Integration:** Use the physical Volume buttons on the Android device as a hardware Push-To-Talk button, even while wearing gloves.
*   **Bluetooth Fallback:** Automatically degrades gracefully to Bluetooth RFCOMM sockets if Wi-Fi Aware is unsupported by a peer device.
*   **Panic Wipe:** Securely wipe all sensitive triage and location data instantly in extreme situations.

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Networking:** Android Wi-Fi Aware API, Bluetooth Classic
*   **Local DB:** Room Database (SQLite)
*   **Machine Learning:** Sherpa-ONNX (Offline STT/TTS), AI4Bharat models

## 📱 Installation & Setup

1.  Clone the repository:
    ```bash
    git clone https://github.com/karchit1128/iTantra.git
    ```
2.  Open the project in **Android Studio**.
3.  Let Gradle sync and download the dependencies.
4.  Build and run the application on a physical Android device (Wi-Fi Aware cannot be tested on an emulator).

*Note: The app requires permissions for Location, Nearby Devices, Microphone, and Camera to function correctly.*

## 🤝 Usage

1.  **Radar / Discovery Screen:** Turn on your visibility to start broadcasting your mesh presence. The radar will show nearby connected rescue nodes.
2.  **Walkie-Talkie:** Press and hold the on-screen Microphone button or the physical Volume Down button to speak. Your voice is transcribed and sent across the mesh.
3.  **Triage Log:** Manually log a victim or use voice commands. Triage cards display the distance to the victim and their priority level.

## ⚠️ Requirements

*   **Android Version:** Android 9.0 (API 28) or higher recommended (Wi-Fi Aware support).
*   **Hardware:** A physical Android device with Wi-Fi hardware capable of Wi-Fi Aware (NAN).

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
