# HamRadio — SDR/DSP Simulation Platform

A modular ham radio simulation platform with real-time voice communication, built with Java + C JNI for DSP/RF signal processing and JavaFX desktop GUI.

Two users can run separate client applications, connect to a shared server, and communicate via simulated radio — with realistic propagation effects (path loss, multipath fading, ionospheric effects, noise) applied to the signal in real time.

## Features

- **SSB / AM / FM modulation** with real-time DSP (C JNI)
- **Realistic RF propagation**: FSPL, multipath, ionospheric fading, noise floor
- **Client-Server architecture**: multiple users communicate over TCP
- **Voice support**: real-time microphone input + speaker output with PTT (Push-to-Talk)
- **Spectrum analyzer** and **waterfall display** (JavaFX Canvas)
- **IQ recording/playback** with SigMF metadata
- **SQLite database** for scenario and recording history
- **Plugin framework** for custom DSP blocks and propagation models

## Architecture

```
┌──────────────────┐        TCP:7100       ┌──────────────────┐
│ Client A (JavaFX)│ ←────────────────────→│ Client B (JavaFX)│
│ Mic → TX audio   │    ┌────────────┐     │ RX audio → Speaker│
│ Spectrum/Waterfall│    │   Server   │     │ Spectrum/Waterfall │
│ PTT / Text TX    │←──│ RF Engine  │──→  │ PTT / Text TX     │
│                  │    │ DSP Engine │     │                   │
└──────────────────┘    │ Propagation│     └──────────────────┘
                        │ Database   │
                        └────────────┘
```

---

## Quick Start (macOS Apple Silicon / M1/M2/M3/M4)

### Prerequisites

| Requirement | Install Command |
|---|---|
| **Homebrew** | `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"` |
| **JDK 21** | `brew install temurin@21` |
| **GCC (via Xcode CLI tools)** | `xcode-select --install` |
| **GNU Make** | Already included with Xcode CLI tools |

### Step 1: Clone and Build

```bash
git clone <repository-url> hamradio
cd hamradio
```

### Step 2: Download JavaFX SDK for macOS (aarch64)

```bash
curl -L -o /tmp/javafx.zip \
  https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_osx-aarch64_bin-sdk.zip
unzip -o /tmp/javafx.zip -d .
mv javafx-sdk-21.0.2 javafx-sdk
rm /tmp/javafx.zip
```

### Step 3: Set JAVA_HOME

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Add this to your `~/.zshrc` to persist:
```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
```

### Step 4: Build

```bash
make all
```

This compiles:
- 111 Java source files (client, server, protocol, UI, DSP, RF, data layers)
- 7 C source files → `lib/libhamradio.dylib` (native DSP/RF via JNI)

> **Note for macOS**: If the build produces `libhamradio.so`, rename it:
> ```bash
> mv lib/libhamradio.so lib/libhamradio.dylib
> ```
> Also update the Makefile `NATIVE_LIB` line if needed:
> ```makefile
> NATIVE_LIB = $(LIB)/libhamradio.dylib
> ```

### Step 5: Download SQLite + SLF4J Dependencies

These are downloaded automatically by `make all`. If not, run manually:

```bash
mkdir -p deps
curl -L -o deps/sqlite-jdbc-3.45.1.0.jar \
  https://github.com/xerial/sqlite-jdbc/releases/download/3.45.1.0/sqlite-jdbc-3.45.1.0.jar
curl -L -o deps/slf4j-api-2.0.9.jar \
  https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
curl -L -o deps/slf4j-nop-2.0.9.jar \
  https://repo1.maven.org/maven2/org/slf4j/slf4j-nop/2.0.9/slf4j-nop-2.0.9.jar
```

---

## Running

### Mode 1: Standalone (single process, no network)

```bash
make run
```

This launches a single JavaFX window with both stations simulated locally. Good for quick testing.

### Mode 2: Client-Server (two users over network)

#### Terminal 1 — Start the Server

```bash
make run-server
```

Output:
```
========================================
  HamRadio Simulation Server
========================================
  Port:              7100
  Sample rate:       44100 Hz
  Propagation model: full
  Max clients:       10
========================================
[Server] Listening on port 7100...
```

Server options:
```bash
make run-server ARGS="--port 7100 --sample-rate 44100 --propagation full --max-clients 10"
```

#### Terminal 2 — Start Client A

```bash
make run-client
```

A connection dialog appears:
- **Host**: `localhost` (or server IP if remote)
- **Port**: `7100`
- **Callsign**: `VK3ABC` (any unique name)
- **Latitude**: `-37.8` (Melbourne, Australia)
- **Longitude**: `144.9`
- **Frequency**: `7100000` (7.1 MHz)
- **Mode**: `SSB`

Click **OK** to connect.

#### Terminal 3 — Start Client B

```bash
make run-client
```

Enter different station details:
- **Callsign**: `JA1YXP` (must be different from Client A)
- **Latitude**: `35.7` (Tokyo, Japan)
- **Longitude**: `139.7`
- Other fields same as Client A

#### Communicate!

**Text mode (default):**
1. Type a message in the TX field (e.g., `CQ CQ CQ DE VK3ABC K`)
2. Click **TX**
3. The other client sees the signal in spectrum/waterfall display

**Voice mode:**
1. Click **VOICE** toggle button to switch to voice mode
2. **Hold** the PTT button and speak into your microphone
3. Release PTT to stop transmitting
4. The other client hears your voice through their speakers (with propagation effects!)

---

## Running Clients on a Different Machine

The client does **not** need the native C library — all DSP/RF processing runs on the server. You only need JDK 21 + JavaFX SDK.

### On the client machine (macOS M1):

```bash
# 1. Copy the client JAR
scp server-machine:hamradio/dist/client/hamradio-client.jar .

# 2. Download JavaFX SDK (macOS aarch64)
curl -L -o javafx.zip \
  https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_osx-aarch64_bin-sdk.zip
unzip javafx.zip

# 3. Run
java --module-path javafx-sdk-21.0.2/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp hamradio-client.jar \
     com.hamradio.client.HamRadioClient
```

In the connection dialog, enter the **server machine's IP address** instead of `localhost`.

### On Windows:

```powershell
# Download JavaFX SDK (Windows x64)
curl.exe -L -o javafx.zip https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_windows-x64_bin-sdk.zip
tar -xf javafx.zip

# Run
java --module-path javafx-sdk-21.0.2\lib --add-modules javafx.controls,javafx.fxml -cp hamradio-client.jar com.hamradio.client.HamRadioClient
```

---

## Testing

### Headless End-to-End Test (no GUI)

```bash
java -Djava.library.path=lib \
  -cp "build/classes:deps/sqlite-jdbc-3.45.1.0.jar:deps/slf4j-api-2.0.9.jar:deps/slf4j-nop-2.0.9.jar" \
  com.hamradio.integration.HeadlessQSOTest
```

### Client-Server Integration Test (real TCP, no GUI)

```bash
java -Djava.library.path=lib \
  -cp "build/classes:deps/sqlite-jdbc-3.45.1.0.jar:deps/slf4j-api-2.0.9.jar:deps/slf4j-nop-2.0.9.jar" \
  com.hamradio.integration.ClientServerQSOTest
```

### Unit Tests

```bash
# Scenario Manager state machine
java -cp "build/classes:deps/*" com.hamradio.control.ScenarioManagerTest

# Checkpoint save/load
java -cp "build/classes:deps/*" com.hamradio.control.ScenarioCheckpointTest

# DSP Graph topological sort
java -Djava.library.path=lib -cp "build/classes:deps/*" com.hamradio.dsp.graph.DSPGraphTest

# RF Channel Pipeline (FSPL, multipath, noise)
java -Djava.library.path=lib -cp "build/classes:deps/*" com.hamradio.rf.ChannelPipelineTest

# SigMF metadata format
java -cp "build/classes:deps/*" com.hamradio.data.SigMFMetadataTest
```

### Full UI Test Suite (automated GUI with screenshots)

```bash
bash src/test/java/com/hamradio/integration/FullUITestSuite.sh
```

This starts a server, runs 6 test scenarios (SSB, AM, FM, short/long distance, multi-TX), captures 14 screenshots in `test_screenshots/`.

---

## Project Structure

```
hamradio/
├── src/main/java/com/hamradio/
│   ├── HamRadioApp.java              # Standalone mode entry point
│   ├── client/                        # Network client (6 files)
│   │   ├── HamRadioClient.java        #   JavaFX Application
│   │   ├── ClientMainWindow.java      #   Main window with voice support
│   │   ├── ServerConnection.java      #   TCP socket management
│   │   ├── MessageDispatcher.java     #   Network → EventBus bridge
│   │   ├── MicrophoneInput.java       #   Real-time mic capture
│   │   └── SpeakerOutput.java         #   Real-time audio playback
│   ├── server/                        # Simulation server (5 files)
│   │   ├── HamRadioServer.java        #   Main entry point
│   │   ├── SimulationEngine.java      #   TX→Channel→RX processing
│   │   ├── ClientHandler.java         #   Per-client TCP handler
│   │   ├── ClientRegistry.java        #   Connected client registry
│   │   └── ServerConfig.java          #   Server configuration
│   ├── protocol/                      # Binary TCP protocol (19 files)
│   │   ├── MessageType.java           #   Message type enum
│   │   ├── Message.java               #   Base message class
│   │   ├── MessageCodec.java          #   Frame reader/writer
│   │   └── messages/                  #   16 concrete message types
│   ├── control/                       # Scenario lifecycle (8 files)
│   ├── dsp/                           # DSP graph engine (27 files)
│   ├── rf/                            # RF propagation models (10 files)
│   ├── event/                         # EventBus pub/sub (8 files)
│   ├── net/                           # Station/session management (4 files)
│   ├── data/                          # IQ recording, SQLite (5 files)
│   ├── plugin/                        # Plugin framework (4 files)
│   └── ui/                            # JavaFX UI panels (9 files)
├── src/main/c/                        # C native DSP/RF (12 files)
│   ├── hamradio_dsp.c                 #   FFT, FIR/IIR filters, resampler
│   ├── hamradio_modem.c               #   AM/FM/SSB modulation
│   ├── hamradio_rf.c                  #   Propagation models
│   ├── hamradio_buffer.c              #   Lock-free SPSC ring buffer
│   └── jni_*.c                        #   JNI glue code
├── src/test/                          # Tests (8 files)
├── dist/client/                       # Client distribution for Windows
├── Makefile                           # Build system
├── pom.xml                            # Maven configuration
└── javafx-sdk/                        # JavaFX SDK (download separately)
```

## Troubleshooting

### macOS: "libhamradio.dylib" cannot be opened

```bash
# Allow the library
xattr -d com.apple.quarantine lib/libhamradio.dylib
```

Or go to **System Settings → Privacy & Security** and click "Allow Anyway".

### macOS: Microphone permission

The first time you use voice mode (PTT), macOS will ask for microphone permission. Click **Allow**. If you denied it, go to **System Settings → Privacy & Security → Microphone** and enable it for the Terminal/Java app.

### Cannot connect to server

1. Check server is running: `make run-server` should show "Listening on port 7100"
2. Check firewall: `sudo pfctl -sr | grep 7100` — if blocked, temporarily disable: `sudo pfctl -d`
3. If connecting from another machine, use the server's LAN IP (find with `ifconfig | grep inet`)

### Build error: "jni.h not found"

```bash
# Verify JAVA_HOME
echo $JAVA_HOME
ls $JAVA_HOME/include/jni.h

# If not set
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Build error: macOS uses .dylib not .so

Edit `Makefile` line for `NATIVE_LIB`:
```makefile
NATIVE_LIB = $(LIB)/libhamradio.dylib
```

And the `CFLAGS`:
```makefile
CFLAGS = -dynamiclib -fPIC -O2 -Wall -Wno-unused-function
```

---

## License

Internal / Confidential — Platform Architecture Team
