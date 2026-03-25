# HamRadio Client — Windows Setup

## Yêu cầu
1. **JDK 21** — Download: https://adoptium.net/
2. **JavaFX SDK 21 (Windows)** — Download: https://gluonhq.com/products/javafx/

## Cài đặt

### Bước 1: Cài JDK 21
- Download Temurin JDK 21 từ https://adoptium.net/
- Chạy installer, chọn "Set JAVA_HOME" trong quá trình cài

### Bước 2: Download JavaFX SDK
- Vào https://gluonhq.com/products/javafx/
- Chọn: JavaFX 21.0.2, Windows, SDK
- Download và giải nén
- Copy thư mục `javafx-sdk-21.0.2` vào cùng thư mục với file này

### Bước 3: Cấu trúc thư mục
```
hamradio-client/
├── hamradio-client.jar        ← file JAR
├── run-client.bat             ← launcher (CMD)
├── run-client.ps1             ← launcher (PowerShell)
├── SETUP.md                   ← file này
└── javafx-sdk-21.0.2/         ← JavaFX SDK (bạn download)
    └── lib/
        ├── javafx.base.jar
        ├── javafx.controls.jar
        ├── javafx.fxml.jar
        ├── javafx.graphics.jar
        └── ...
```

## Chạy

### Server (trong WSL):
```bash
cd /home/hungpk/workspace/hamradio
make run-server
```

### Tìm IP của WSL:
```bash
hostname -I
# Ví dụ: 172.25.160.1
```

### Client (trên Windows):

**PowerShell (tự detect IP):**
```powershell
.\run-client.ps1
```

**Hoặc chỉ định IP:**
```powershell
.\run-client.ps1 -ServerHost 172.25.160.1
```

**CMD:**
```cmd
run-client.bat 172.25.160.1
```

### Mở 2 clients
- Chạy `run-client` 2 lần, mỗi lần nhập callsign khác nhau
- Client 1: VK3ABC, Melbourne
- Client 2: JA1YXP, Tokyo
- Nhấn TX để truyền tin, bên kia sẽ nhận được signal

## Firewall
Nếu không kết nối được, mở port 7100 trong WSL:
```bash
# Trong WSL
sudo ufw allow 7100/tcp 2>/dev/null
```

Windows Firewall thường cho phép outbound connections tự động.
Nếu cần, thêm rule cho inbound port 7100 trên Windows Firewall.
