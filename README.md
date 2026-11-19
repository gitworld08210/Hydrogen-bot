# ⚡ Hydrogen Bot (हाइड्रोजन बॉट)
> **Ultra-Fast, Lightweight Autonomous Voice Assistant for Android**

Built with **Kotlin & Jetpack Compose**, featuring an Apple & Stripe inspired **Pure White Minimalist Theme**, native app intent routing (guaranteed in-app launching for Amazon, YouTube, Flipkart), Email OTP Authentication, and an AES-256 Biometric Hardware Vault.

---

## 📱 Features Implemented

1. **Native App-First Intent Routing (`AppRouter.kt`)**:
   * **Amazon India Search:** Detects if `in.amazon.mShop.android.shopping` or `com.amazon.mShop.android.shopping` is installed. Explicitly launches the **official native Amazon App**, bypassing mobile web browsers.
   * **YouTube Search/Play:** Explicitly launches the official `com.google.android.youtube` app.
   * **Flipkart & WhatsApp:** Target-locked native app execution.

2. **Voice Engine & Audio Wave (`VoiceManager.kt` & `WaveformVisualizer.kt`)**:
   * Uses Android's native `SpeechRecognizer` configured for **Hindi (`hi-IN`) and Indian English (`en-IN`)**.
   * Real-time RMS audio volume feedback driving fluid, minimal sound bars.
   * Natural Hindi/English spoken voice responses via `TextToSpeechManager.kt`.

3. **Pure White Minimalist UI (`ui/theme/` & `ui/components/`)**:
   * Inspired by **Apple & Stripe** (`awesome-design-md` tokens).
   * Clean White Canvas (`#FFFFFF`) with soft pearl cards (`#F8FAFC`).
   * **Subtle & Gentle Mic Button:** Zero harsh neon; an elegant circular white button with a soft sky-blue breathing ring (`#BAE6FD`).
   * High-contrast Slate Black typography (`#0F172A`) for effortless daylight readability.

4. **Human-in-the-Loop Safety Gate (`SafetyGateCard.kt`)**:
   * During ticket booking (IRCTC) or transactions, the bot automatically fills details but **MANDATORILY STOPS before the payment/OTP screen**.
   * The bot speaks out: *"Payment screen ready hai. Please apna UPI/PIN enter kijiye."* and hands control back to the user.

5. **Email OTP Authentication (`AuthManager.kt` & `AuthScreen.kt`)**:
   * 100% Free, passwordless login via 6-digit OTP sent to user's email.
   * Multi-device session persistence.

6. **Hardware-Backed Biometric Vault (`EncryptedVaultManager.kt` & `VaultScreen.kt`)**:
   * Stores passwords, IRCTC passenger profiles, and addresses locally using **Android KeyStore (AES-256-GCM)**.
   * Requires biometric fingerprint / face authentication before revealing any credentials.

---

## 🛠️ How to Open & Run in Android Studio

1. Open **Android Studio**.
2. Click **File > Open** and select the folder:
   `C:\Users\Administrator\.gemini\antigravity\scratch\HydrogenBot`
3. Wait for Gradle Sync to finish.
4. Connect an Android phone via USB (with **USB Debugging** enabled in Developer Options) or start an Emulator.
5. Click the green **Run (▶)** button.

---

## 🎤 Sample Voice Commands to Test

| What to say (Hindi / English) | What Hydrogen Bot does |
| :--- | :--- |
| *"Mere liye Amazon pe running shoes search karo"* | Instantly opens the official **Amazon App** directly into the search results page. |
| *"YouTube pe Arijit Singh ke gaane chalao"* | Opens official **YouTube App** and plays/searches the song. |
| *"Flipkart pe best earbuds dhundo"* | Opens official **Flipkart App** search. |
| *"IRCTC kal ka train ticket check karo"* | Triggers booking workflow and surfaces the **Safety Guardrail Pause**. |
