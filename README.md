# Strike Ops: AI-Integrated Tactical Shooter

Strike Ops is a groundbreaking mobile shooter for Android that explores the intersection of traditional gaming mechanics and artificial intelligence. This project serves as a technical demonstration of how AI can be leveraged to assist in the generation of complex game assets, logic, and environmental design.

Featuring responsive controls and an optimized rendering engine, Strike Ops showcases the potential of modern Android gaming architectures.

## 🎮 Building the Project

Follow these steps to compile and run the Strike Ops source code.

### Prerequisites

- [Android Studio](https://developer.android.com/studio)
- A device or emulator with OpenGL ES 3.0+ support

### Setup Instructions

1. **Initialize Project**
   Open the repository in Android Studio.

2. **Sync and Repair**
   Allow the IDE to complete the Gradle sync. If prompted to fix incompatibilities or update the Android Gradle Plugin, proceed with the recommended fixes to ensure project stability.

3. **Enable AI Logic Engine**
   The game's dynamic elements are powered by an external model.
   - Define your credentials in a `.env` file at the project root.
   - Requirement: `GEMINI_API_KEY=your_key_here`

4. **Local Build Configuration**
   To avoid signing conflicts during local development, ensure that the `build.gradle.kts` file is configured to use your local debug configuration rather than a production signing key.

5. **Execution**
   Connect your Android device and execute the **Run** command. Enjoy the technical showcase of Strike Ops.
