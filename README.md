# 📖 Yomu (夜読) 

Manga reader inspired by **Comixology Guided View**. Features a local library and browsing feature with (Buzzword) ✨***AI***✨ Image recognition to animate a similar experience as Guided view, on the fly, for any manga.
<img width="376" height="795" alt="Screenshot 2026-07-31 221328" src="https://github.com/user-attachments/assets/7bb1074e-3d63-47a4-9832-4fbf8acd5049" />


## 🤔❓Usage:
Opening any manga chapter, the panel recognition model will load the image and figure out coordinates based on Hugging Face's **YOLOv8 Manga Panel Model** and Google's **ML Kit OCR** to map what it thinks are panels and text, and based on those coordinates, will use an **XYCut spatial algorithm** to sequence reading order, according to standard right-to-left manga reading flow.
Tapping advances through each panel for a smooth and cinematic manga reading experience!
<img width="448" height="542" alt="Screenshot 2026-07-31 221543" src="https://github.com/user-attachments/assets/d16a7dec-8bc6-4b79-a332-59d72c300223" />


## 💻⚙️ The computer talk:
- **Language**: Kotlin
- **Machine Learning**: ONNX Runtime Android + Google ML Kit Text Recognition

## ⚖️ Legal & Content Disclaimer
Yomu is an open-source client application built for educational and personal reading purposes. 
- **No Content Hosting**: Yomu does **not** host, store, or distribute any manga files or copyrighted content.
- **Public Content Scraping**: The app dynamically loads publicly accessible manga content directly from `weebcentral.com`.
- **Intellectual Property**: All manga titles, artwork, character designs, and trademarks belong entirely to their respective content creators, authors, and publishers.

## 🚀 Building from Source
### Prerequisites
- Android Studio (2024.2.1+)
- JDK 17
- Android SDK Platform 35
### Build Steps
```bash
# Clone the repository
git clone https://github.com/lmaeboy/Yomu.git
cd Yomu
./gradlew assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/yomu.apk`.
