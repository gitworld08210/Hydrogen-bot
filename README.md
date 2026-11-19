# ⚡ Hydrogen Bot (हाइड्रोजन बॉट)
> **Ultra-Fast, Lightweight Autonomous Voice Assistant for Android**

[![Platform: Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/gitworld08210/Hydrogen-bot)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin_Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://github.com/gitworld08210/Hydrogen-bot)
[![Download APK](https://img.shields.io/badge/Download_APK-HydrogenBot--v1.0.apk-0284C7?style=for-the-badge&logo=android&logoColor=white)](https://github.com/gitworld08210/Hydrogen-bot/raw/main/release/HydrogenBot-v1.0.apk)

---

## 📲 Direct APK Download

Aap bina computer ke bhi seedha apne Android phone me APK download aur install kar sakte hain:

👉 **[Download HydrogenBot-v1.0.apk (Direct Download)](https://github.com/gitworld08210/Hydrogen-bot/raw/main/release/HydrogenBot-v1.0.apk)** *(Size: ~22.4 MB)*

### Phone Me Install Karne Ka Tareeqa:
1. Upar diye gaye link par tap karke **`HydrogenBot-v1.0.apk`** download karein.
2. Download complete hone par file par tap karein aur **"Install"** dabayein.
3. App kholte hi:
   * **Microphone Permission:** Allow karein (voice commands ke liye).
   * **Email OTP:** Apna email enter karke login karein.

---

## 🌟 Key Features

1. **Native App-First Intent Routing (`AppRouter.kt`)**:
   * **Amazon India Search:** Agar phone me Amazon app installed hai, toh **100% official Amazon App hi launch hogi**, web browser nahi.
   * **YouTube Search/Play:** Seedha official YouTube native app khulegi.
   * **Flipkart & WhatsApp:** Target-locked native app execution.

2. **Apple & Stripe Inspired "Clean White" UI (`ui/theme/` & `ui/components/`)**:
   * Pure White canvas (`#FFFFFF`) with soft pearl surfaces (`#F8FAFC`).
   * **Subtle & Gentle Mic Button:** Zero harsh neon; pure white circular button with a gentle sky-blue breathing ring (`#BAE6FD`).
   * High-contrast Slate Black typography (`#0F172A`) for effortless daylight readability.
   * Fluid 5-bar voice waveform visualizer reacting to audio volume.

3. **Voice Engine & Natural Feedback (`VoiceManager.kt` & `TextToSpeechManager.kt`)**:
   * Native Android `SpeechRecognizer` configured for **Hindi (`hi-IN`) and Indian English (`en-IN`)**.
   * Spoken audio responses in natural voice.

4. **Human-in-the-Loop Safety Gate (`SafetyGateCard.kt`)**:
   * Ticket booking (IRCTC) ya transactions ke waqt bot auto-fill karega, lekin **Payment/OTP screen aane se pehle 100% stop ho jayega** aur aawaz dega taaki user khud safely PIN enter kare.

5. **Email OTP Authentication (`AuthManager.kt` & `AuthScreen.kt`)**:
   * 100% Free, passwordless 6-digit Email OTP login.
   * Multi-device session persistence via **Firebase Cloud Firestore**.

6. **Hardware-Backed Biometric Vault (`EncryptedVaultManager.kt` & `VaultScreen.kt`)**:
   * Passwords, IRCTC profiles aur addresses **Android KeyStore (AES-256-GCM)** hardware encryption me store hote hain.
   * View karne se pehle **Fingerprint / Biometric scan** compulsory hai.

---

## 🎤 Sample Voice Commands to Test

| Command (Hindi / English) | Action Performed |
| :--- | :--- |
| *"Mere liye Amazon pe running shoes search karo"* | Official **Amazon Native App** search results screen open |
| *"YouTube pe Arijit Singh ke gaane chalao"* | Official **YouTube Native App** search/play |
| *"Flipkart pe wireless earbuds dhundo"* | Official **Flipkart Native App** open |
| *"IRCTC kal ka train ticket check karo"* | Booking workflow + **Safety Guardrail Pause** |

---

## 🛠️ Project Tech Stack

* **Language:** Kotlin 1.9
* **UI:** Jetpack Compose (Material3)
* **Design DNA:** Clean White Minimalist (Apple & Stripe)
* **AI Brain:** Google Gemini 1.5/2.0 Flash
* **Cloud & Auth:** Firebase Authentication & Cloud Firestore (`oneleven-a669b`)
* **Security:** AndroidX Biometric + Security KeyStore (AES-256)
