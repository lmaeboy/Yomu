# 📖 Yomu (夜読)

Manga reader inspired by **Comixology Guided View**. Features a local library and browsing feature with (Buzzword) ✨***AI***✨ Image recognition to animate a similar experience as Guided view, on the fly, for any manga.

## 🤔❓Usage:
Opening any manga chapter, the panel recognition model will load the image and figure out coordinates based on Hugging Face's **YOLOv8 Manga Panel Model** and Google's **ML Kit OCR** to map what it thinks are panels and text, and based on those coordinates, will use an **XYCut spatial algorithm** to sequence reading order, according to standard right-to-left manga reading flow.
Tapping advances through each panel for a smooth and cinematic manga reading experience!

## 💻⚙️ The computer talk:
- **Language**: Kotlin
- **Machine Learning**: ONNX Runtime Android + Google ML Kit Text Recognition
