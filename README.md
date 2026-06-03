# Jalin Training Camunda - Day 3

Proyek Spring Boot untuk pelatihan integrasi Camunda 8 (Zeebe engine) di Jalin Pembayaran Nusantara. Proyek ini mendemonstrasikan cara membuat Job Worker dan memulai proses BPMN menggunakan Camunda 8 self-managed.

---

## Tujuan

Proyek ini dibuat sebagai bahan pelatihan Day 3 untuk memahami:
- Cara mengintegrasikan Spring Boot dengan Camunda 8 menggunakan `camunda-spring-boot-starter`
- Cara membuat Job Worker yang merespons task dari Zeebe engine
- Cara memulai proses BPMN melalui REST API menggunakan Zeebe Client
- Cara mengonsumsi external API dari dalam Job Worker menggunakan WebClient

---

## Tech Stack

| Teknologi | Versi |
|-----------|-------|
| Java | 21 |
| Spring Boot | 3.5.6 |
| Camunda Spring Boot Starter | 8.8.0 |
| Spring Web / WebFlux | (via Spring Boot) |
| Lombok | (via Spring Boot) |
| Maven | 3.x |

---

## Prasyarat

Pastikan sudah terinstall:

- **Java 21** atau lebih baru
- **Maven 3.6+** (atau gunakan `mvnw` yang sudah disertakan)
- **Camunda 8 self-managed** berjalan secara lokal (Zeebe engine)
  - gRPC address: `localhost:26500`
  - REST address: `localhost:8080`
- **BPMN Process** dengan ID `BPM_Customer_Verification` sudah di-deploy ke Camunda

> Untuk menjalankan Camunda 8 secara lokal, gunakan [Camunda 8 Run](https://docs.camunda.io/docs/self-managed/camunda-deployment/camunda-run/) atau Docker Compose dari [camunda/camunda-platform](https://github.com/camunda/camunda-platform).

---

## Instalasi & Menjalankan Aplikasi

### 1. Clone Repository

```bash
git clone <repository-url>
cd jalin-training-camunda-day3
```

### 2. Konfigurasi (opsional)

Sesuaikan konfigurasi di `src/main/resources/application.yaml` jika Camunda berjalan di alamat berbeda:

```yaml
server:
  port: 8081

camunda:
  client:
    mode: self-managed
    security:
      plaintext: true
    zeebe:
      enabled: true
      grpc-address: http://localhost:26500
      rest-address: http://localhost:8080
```

### 3. Build & Run

```bash
# Menggunakan Maven Wrapper (direkomendasikan)
./mvnw spring-boot:run

# Atau build JAR terlebih dahulu
./mvnw clean package
java -jar target/camunda.training-0.0.1-SNAPSHOT.jar
```

> Di Windows gunakan `mvnw.cmd` sebagai pengganti `./mvnw`.

Aplikasi akan berjalan di **http://localhost:8081**.

---

## Fitur & Komponen

### 1. REST API — Memulai Proses BPMN

**Endpoint:** `POST /verification/start`

Memulai proses BPMN `BPM_Customer_Verification` melalui Zeebe Client.

**Request Body:**
```json
{
  "nik": "3201234567890001",
  "value1": 10,
  "value2": 20
}
```

**Response (sukses):**
```json
{
  "processInstanceKey": 2251799813685251
}
```

**Response (gagal):**
```json
{
  "message": "pesan error"
}
```

---

### 2. Job Worker — `CalculateWorker`

**Job Type:** `calculate-value`

Menangani task kalkulasi dari Zeebe. Menerima dua variabel integer, menjumlahkannya, dan mengembalikan hasilnya sebagai variabel proses.

**Input Variables:**
| Variable | Tipe | Keterangan |
|----------|------|------------|
| `value1` | Integer | Nilai pertama |
| `value2` | Integer | Nilai kedua |

**Output Variables:**
| Variable | Tipe | Keterangan |
|----------|------|------------|
| `result` | Integer | Hasil penjumlahan `value1 + value2` |
| `message` | String | Pesan deskripsi hasil, contoh: `"Addition result: 30"` |

---

### 3. Job Worker — `CheckStatusWorker`

**Job Type:** `check-status`

Mengecek status pelanggan berdasarkan NIK dengan memanggil external API. Worker ini menggunakan WebClient (non-blocking HTTP client dari Spring WebFlux).

**External API:** `GET http://localhost/api/pelanggan.json`

**Input Variables:**
| Variable | Tipe | Keterangan |
|----------|------|------------|
| `nik` | String | Nomor Induk Kependudukan pelanggan |

**Output Variables:**
| Variable | Tipe | Keterangan |
|----------|------|------------|
| `status` | String | Status pelanggan (`ACTIVE`, `INACTIVE`, `NOT FOUND`, `ERROR`) |
| `message` | String | Pesan hasil pengecekan |

**Alur logika:**
1. Panggil API external untuk mendapatkan daftar pelanggan
2. Iterasi data pelanggan, cocokkan NIK
3. Jika ditemukan → kembalikan `status` dari data pelanggan
4. Jika tidak ditemukan → kembalikan `"NOT FOUND"`
5. Jika error → kembalikan `"ERROR"` beserta pesan exception

---

## Struktur Proyek

```
src/
└── main/
    ├── java/co/id/jalin/camunda/training/
    │   ├── JalinTrainingCamundaApplication.java   # Entry point Spring Boot
    │   ├── CalculateWorker.java                   # Worker: penjumlahan dua nilai
    │   ├── CheckStatusWorker.java                 # Worker: cek status pelanggan by NIK
    │   └── controller/
    │       └── VerificationController.java        # REST API untuk memulai proses BPMN
    └── resources/
        └── application.yaml                       # Konfigurasi server & Camunda
```

---

## Cara Kerja (Alur Umum)

```
Client (Postman/Frontend)
        |
        | POST /verification/start  { nik, value1, value2 }
        v
VerificationController
        |
        | newCreateInstanceCommand (BPM_Customer_Verification)
        v
Zeebe Engine (Camunda 8)
        |
        |-- Job: check-status      --> CheckStatusWorker --> cek NIK ke API external
        |-- Job: calculate-value   --> CalculateWorker   --> hitung value1 + value2
```

---

## Contoh Penggunaan (cURL)

```bash
curl -X POST http://localhost:8081/verification/start \
  -H "Content-Type: application/json" \
  -d '{
    "nik": "3201234567890001",
    "value1": 15,
    "value2": 25
  }'
```

---

## Catatan Pengembangan

- Worker `CheckStatusWorker` menggunakan `autoComplete = false` (default), artinya Zeebe menunggu worker menyelesaikan job secara eksplisit atau lewat return value.
- Worker `CalculateWorker` menggunakan `autoComplete = true`, sehingga job otomatis selesai setelah method return.
- Koneksi ke Camunda menggunakan mode `plaintext` (tanpa TLS), cocok untuk environment development lokal.
