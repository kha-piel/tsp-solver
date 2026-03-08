# 🗺️ TSP Solver — Tối Ưu Hành Trình Giao Hàng

> Ứng dụng web giải bài toán **Người Bán Hàng Du Lịch (TSP)** tích hợp bản đồ thực tế, hỗ trợ nhiều thuật toán tối ưu và lập lịch theo khung thời gian.

[![Java](https://img.shields.io/badge/Java-17%2B-red?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue?logo=apachemaven)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## ✨ Tính Năng

| Tính năng | Mô tả |
|---|---|
| 📍 **Geocoding** | Tự động tra toạ độ địa chỉ từ OpenStreetMap (Nominatim) |
| 🚗 **Routing thực tế** | Tính khoảng cách & thời gian di chuyển thực qua OSRM API |
| 🧠 **Nhiều thuật toán** | NN+2-Opt, NN+3-Opt, Simulated Annealing, A* Search |
| 🕐 **TSPTW** | Hỗ trợ lập lịch giao hàng với khung thời gian (Time Windows) |
| 🔄 **Re-route** | Tự động tìm tuyến thay thế khi né tránh một đoạn đường |
| 🗺️ **Bản đồ trực quan** | Hiển thị tuyến đường trên bản đồ tương tác |

---

## 🏗️ Kiến Trúc

```
src/main/java/com/example/tsp/
├── controller/
│   └── TspController.java       # REST + MVC Controller (GET /, POST /, POST /reroute)
├── service/
│   ├── GeocodingService.java    # Nominatim geocoding API
│   ├── RoutingService.java      # OSRM distance/duration matrix
│   └── SolverService.java       # Thuật toán: NN, 2-Opt, 3-Opt, SA, A*
└── model/
    ├── AddressData.java          # Địa chỉ + schedule info
    ├── DeliveryPointInput.java   # Input điểm giao hàng
    ├── FormData.java             # Dữ liệu form
    └── RouteResult.java          # Kết quả tuyến đường
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy

### Yêu cầu hệ thống
- **Java 17+** (Đã có JDK là đủ)
- **Maven 3.9+**

### Cài Maven (nếu chưa có) — Windows
```powershell
# Cài Scoop (package manager)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression

# Mở terminal MỚI, sau đó cài Maven
scoop install maven
```

### Chạy ứng dụng

```bash
# Clone repo
git clone https://github.com/<YOUR_USERNAME>/tsp-solver.git
cd tsp-solver/doanse-java

# Chạy với Spring Boot Maven Plugin
mvn spring-boot:run
```

Ứng dụng sẽ khởi động tại: **http://localhost:8081**

### Tùy chỉnh cổng

```bash
# Chạy trên cổng khác (ví dụ: 9000)
PORT=9000 mvn spring-boot:run

# Windows PowerShell
$env:PORT = "9000"; mvn spring-boot:run
```

---

## 🐳 Chạy Bằng Docker

```bash
# Build image
docker build -t tsp-solver .

# Chạy container
docker run -p 8081:8081 tsp-solver
```

---

## 🧠 Các Thuật Toán

| Thuật toán | Loại | Độ phức tạp | Ghi chú |
|---|---|---|---|
| **Nearest Neighbor + 2-Opt** | Heuristic | O(n²) | Nhanh, kết quả tốt |
| **Nearest Neighbor + 3-Opt** | Heuristic | O(n³) | Chất lượng cao hơn 2-Opt |
| **Simulated Annealing (SA)** | Metaheuristic | O(n·iter) | Tốt cho bài toán lớn |
| **A\* Search** | Exact (TSP-TW) | O(n²·2ⁿ) | Tối ưu chính xác, giới hạn ≤12 điểm |

### Chế độ giải
- **`distance`** — So sánh tất cả thuật toán, xếp hạng theo khoảng cách ngắn nhất
- **`astar`** — Chỉ dùng A* (chính xác tuyệt đối, giới hạn ≤12 điểm)
- **`schedule`** — Giải TSP với khung thời gian (TSPTW) dùng Simulated Annealing

---

## 🌐 API Endpoints

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/` | Trang chủ, form nhập địa chỉ |
| `POST` | `/` | Giải TSP, trả về trang kết quả |
| `POST` | `/reroute` | Tìm tuyến thay thế (JSON API) |

### Ví dụ request `/reroute`

```json
POST /reroute
{
  "all_addresses_data": [
    { "display_name": "Kho hàng A", "lat": 10.762, "lon": 106.660 },
    { "display_name": "Điểm giao 1", "lat": 10.775, "lon": 106.701 }
  ],
  "avoid_segment": {
    "from": "Kho hàng A",
    "to": "Điểm giao 1"
  }
}
```

---

## 🔗 Công Nghệ Sử Dụng

- **[Spring Boot 3.2](https://spring.io/projects/spring-boot)** — Backend framework
- **[Thymeleaf](https://www.thymeleaf.org/)** — Server-side templating
- **[Lombok](https://projectlombok.org/)** — Giảm boilerplate code
- **[Nominatim (OSM)](https://nominatim.openstreetmap.org/)** — Geocoding miễn phí
- **[OSRM](http://router.project-osrm.org/)** — Open Source Routing Machine
- **[Leaflet.js](https://leafletjs.com/)** *(frontend)* — Bản đồ tương tác

---

## 📄 License

MIT License — xem file [LICENSE](LICENSE) để biết thêm chi tiết.

---

<div align="center">
  Made with ❤️ using Java & Spring Boot
</div>
