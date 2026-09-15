# RikkeiBank API — Ngân hàng số (Microservices Banking Platform)

Triển khai đầy đủ theo yêu cầu SRS: Java 17, Spring Boot 3.3.4, Spring Cloud 2023.0.3.

## 1. Kiến trúc & lý do chọn Microservice (MSA)

Hệ thống được tách từ Monolithic thành 8 module độc lập, mỗi module tự chạy, tự đóng gói,
và (trừ 3 hạ tầng) **sở hữu database riêng (Database-per-service)**:

| # | Service | Port | DB (H2 in-memory) | Vai trò |
|---|---------|------|--------------------|---------|
| 1 | config-server | 8888 | – | Cấu hình tập trung (Spring Cloud Config, native/local repo) |
| 2 | eureka-server | 8761 | – | Service Registry & Discovery |
| 3 | api-gateway | 8080 | – | Cổng vào duy nhất, định tuyến + Load Balancing + xác thực JWT |
| 4 | identity-service | 8081 | identitydb | Đăng ký/đăng nhập, phát hành JWT, phân quyền, force-logout |
| 5 | customer-service | 8082 | customerdb | Catalog: Customer, Staff, AccountType (CRUD) |
| 6 | account-service | 8083 | accountdb | Tài khoản, số dư, debit/credit, cache Redis |
| 7 | transaction-service | 8084 | transactiondb | Nghiệp vụ chuyển khoản, Saga Orchestrator, Circuit Breaker |
| 8 | notification-service | 8085 | notificationdb | Consume Kafka, lưu & trả về thông báo biến động |

**Vì sao chọn MSA thay vì Monolithic/SOA:**
- **Tách rời (Decoupling):** mỗi nghiệp vụ (khách hàng, tài khoản, giao dịch, thông báo) thay đổi
  độc lập, deploy độc lập, không ảnh hưởng dây chuyền tới toàn hệ thống.
- **Mở rộng độc lập (Independent scaling):** ví dụ `transaction-service` chịu tải giao dịch giờ
  cao điểm có thể scale-out nhiều instance mà không cần scale `customer-service`.
- **Chịu lỗi (Fault isolation) tốt hơn:** một service lỗi (vd. account-service quá tải) không kéo
  sập toàn hệ thống nhờ Circuit Breaker + fallback, thay vì một lỗi trong Monolith làm treo cả khối.
- **Đội nhóm làm việc song song:** mỗi service là 1 codebase nhỏ, dễ chia việc, dễ test riêng lẻ.

## 2. Các pattern & công nghệ đã triển khai (đối chiếu yêu cầu kỹ thuật)

| Yêu cầu SRS | Triển khai |
|---|---|
| RESTful + JSON + đúng HTTP method/status | Controllers dùng `@GetMapping/@PostMapping/...`, trả `ResponseEntity` với status chuẩn (200/201/204/400/401/403/404/409/503) |
| Spring Cloud Config | `config-server` (native profile) phục vụ `config-repo/*.yml` cho toàn bộ service |
| Eureka Discovery | Mọi service `@EnableDiscoveryClient`, đăng ký & tự khám phá theo tên |
| API Gateway + Load Balancing | `api-gateway` dùng `lb://SERVICE-NAME` (Spring Cloud LoadBalancer), route theo path |
| Đồng bộ (RestTemplate/OpenFeign) | `transaction-service` gọi `account-service` qua `AccountServiceClient` (OpenFeign) theo tên Eureka |
| Bất đồng bộ (WebFlux + Kafka) | `transaction-service` publish sự kiện Kafka (`TransactionEventProducer`); `notification-service` consume (`@KafkaListener`); WebClient (WebFlux) có sẵn trên classpath cho các lời gọi reactive |
| Circuit Breaker (Resilience4j, 3 trạng thái) | `TransferSagaOrchestrator.callDebit/callCredit` gắn `@CircuitBreaker(name="accountService")` với fallback; cấu hình CLOSED→OPEN→HALF_OPEN trong `transaction-service.yml` |
| Distributed Caching (Redis, Cache-Aside) | `AccountService` dùng `@Cacheable/@CachePut/@CacheEvict`, `RedisCacheConfig` |
| Saga Pattern (Orchestrator) | `TransferSagaOrchestrator`: debit → credit → compensate-debit khi bước 2 lỗi |
| Database-per-service | Mỗi service 1 schema H2 riêng, không service nào truy cập trực tiếp DB của service khác |
| JWT + @PreAuthorize | `identity-service` phát JWT; Gateway validate + forward header `X-User-Role/X-User-Id`; mỗi service dùng `HeaderAuthenticationFilter` + `@PreAuthorize("hasRole(...)")` |
| AOP + Exception Handling chuẩn | `LoggingAspect` (@Aspect) log thời gian thực thi + lỗi; `GlobalExceptionHandler` (@RestControllerAdvice) trả cấu trúc lỗi thống nhất |
| Unit Test + Jacoco | `AccountServiceTest`, `AuthServiceTest` (JUnit5 + Mockito); `jacoco-maven-plugin` cấu hình sẵn ở pom cha |

## 3. Bảo mật & trải nghiệm đăng nhập

- **Access token** (JWT, 15 phút) dùng cho mọi API qua Gateway.
- **Refresh token** (UUID, 30 ngày, lưu server-side) → CUSTOMER không cần đăng nhập lại mỗi lần mở app
  (gọi `POST /api/identity/auth/refresh` để lấy access token mới khi hết hạn).
- **ADMIN force-logout**: `POST /api/identity/auth/force-logout` ghi timestamp vào Redis
  (`blacklist:{username}`); Gateway so sánh `issuedAt` của mọi token với mốc này → **thu hồi truy cập
  ngay lập tức** dù JWT vốn stateless.
- Phân quyền 3 role: `ADMIN` (toàn quyền), `TELLER` (chỉ xem/xử lý giao dịch được phân công, không đụng
  giao dịch của teller khác — lọc theo `tellerId`), `CUSTOMER` (chỉ xem dữ liệu của chính mình, kiểm tra
  qua header `X-User-Id`). Khách vãng lai (không có token) bị Gateway chặn 401 ở mọi endpoint trừ
  `/api/identity/auth/**`.

## 4. Saga & Circuit Breaker — luồng chuyển khoản

```
Customer -> Gateway -> transaction-service.transfer()
                          |
                          ├─ Step 1: callDebit(fromAccount)  --(Feign+CircuitBreaker)--> account-service
                          |     lỗi? => transaction=FAILED, publish TRANSACTION_FAILED, dừng
                          |
                          ├─ Step 2: callCredit(toAccount)   --(Feign+CircuitBreaker)--> account-service
                          |     lỗi? => compensateDebit(fromAccount) (hoàn tiền)
                          |            transaction=COMPENSATED, publish TRANSACTION_COMPENSATED
                          |
                          └─ Cả 2 bước OK => transaction=SUCCESS, publish TRANSACTION_COMPLETED
                                              -> notification-service consume Kafka -> lưu Notification
```

Circuit Breaker (`accountService`) theo dõi tỉ lệ lỗi cửa sổ trượt 10 request; ≥50% lỗi → **OPEN**
(chặn luôn, gọi fallback ngay, không gọi thật account-service nữa) → sau 10s tự chuyển **HALF_OPEN**
(thử vài request) → nếu ổn thì **CLOSED** trở lại. Điều này ngăn lỗi dây chuyền (cascading failure)
khi account-service quá tải/crash.

## 5. Chạy dự án (yêu cầu máy có internet để tải Maven & Docker images)

### 5.1. Khởi động hạ tầng (Kafka, Zookeeper, Redis)
```bash
docker compose up -d
```

### 5.2. Build toàn bộ
```bash
mvn clean install -DskipTests
```

### 5.3. Khởi chạy theo đúng thứ tự
```bash
cd config-server        && mvn spring-boot:run &
# đợi config-server sẵn sàng (http://localhost:8888/actuator/health) rồi mới chạy tiếp
cd eureka-server         && mvn spring-boot:run &
# đợi eureka lên (http://localhost:8761)
cd identity-service       && mvn spring-boot:run &
cd customer-service       && mvn spring-boot:run &
cd account-service        && mvn spring-boot:run &
cd transaction-service    && mvn spring-boot:run &
cd notification-service   && mvn spring-boot:run &
cd api-gateway            && mvn spring-boot:run &
```
Kiểm tra dashboard Eureka tại `http://localhost:8761` — tất cả 6 service nghiệp vụ + gateway phải hiện UP.

### 5.4. Tài khoản demo (seed sẵn)
| username | password | role | liên kết |
|---|---|---|---|
| admin | admin123 | ADMIN | – |
| teller1 | teller123 | TELLER | Staff #1 |
| customer1 | customer123 | CUSTOMER | Customer #1 / Account RKB2026CUST0001 (10,000,000đ) |
| customer2 | customer123 | CUSTOMER | Customer #2 / Account RKB2026CUST0002 (5,000,000đ) |

### 5.5. Test nhanh bằng curl
```bash
# 1. Đăng nhập lấy access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/identity/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"customer1","password":"customer123"}' | jq -r .accessToken)

# 2. Xem tài khoản của mình (customerId=1 -> account id=1)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/accounts/customer/1

# 3. Chuyển khoản từ account 1 -> account 2, 500,000đ
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":500000}'

# 4. ADMIN thu hồi quyền truy cập của customer1 ngay lập tức
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/identity/auth/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
curl -X POST http://localhost:8080/api/identity/auth/force-logout \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"customer1"}'
# Token cũ của customer1 giờ bị Gateway từ chối 401 dù chưa hết hạn 15 phút
```

### 5.6. Kịch bản rollback (Saga compensation)
Tắt `account-service` (Ctrl+C) rồi thử chuyển khoản → bước debit sẽ lỗi (Circuit Breaker OPEN sau vài
request thất bại) → giao dịch chuyển sang `FAILED`, không trừ tiền. Nếu tắt đúng lúc giữa bước debit và
credit (khó tái hiện thủ công) hệ thống sẽ tự động gọi `compensate-debit` để hoàn tiền và đánh dấu
`COMPENSATED`, đồng thời phát sự kiện Kafka `TRANSACTION_COMPENSATED` để `notification-service` ghi nhận.

## 6. Postman

Import file `postman/RikkeiBank.postman_collection.json`. Bộ sưu tập có biến `{{gateway}}`,
`{{accessToken}}` tự động lưu sau bước Login, và các folder theo từng service, toàn bộ đi qua
`http://localhost:8080` (API Gateway).

## 7. Ghi chú mở rộng

- Đây là bộ khung đầy đủ kiến trúc + luồng nghiệp vụ chính; các bạn có thể mở rộng thêm: OpenAPI/Swagger
  UI, Sleuth/Zipkin distributed tracing, Config Server dùng Git backend thay vì native/local, Kafka
  Streams cho xử lý sự kiện phức tạp hơn, và thêm test coverage để đạt % Jacoco mong muốn.
- Mật khẩu JWT secret trong `config-repo/application.yml` chỉ dùng cho demo — đổi sang biến môi trường/
  secret vault trước khi đưa vào production thật.
