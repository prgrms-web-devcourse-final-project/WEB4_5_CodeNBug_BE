# REST API 명세서

## 1. 사용자 관련 API

### **회원가입 (POST /api/v1/users/signup)**

- **설명**: 신규 사용자 계정을 생성합니다.
- **요청 예시 (JSON):**

    ```json
    {
      "email": "user@example.com",
      "password": "password123",
      "name": "홍길동",
      "age": 23,
      "sex": "MALE",
      "phoneNum": "010-1234-5678",
      "location": "Seoul"
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "회원가입 성공",
      "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "age": 23,
        "sex": "MALE",
        "phoneNum": "010-1234-5678",
        "location": "Seoul",
        "createdAt": "2025-04-24T10:00:00"
      }
    }
    ```

- **응답 예시 (에러)**: (중복된 이메일)

    ```json
    {
      "code": 409,
      "msg": "이미 존재하는 이메일입니다.",
      "data": null
    }
    ```

- body 데이터 누락

    ```json
    {
    	"code": 400,
    	"msg": "필수 데이터가 누락되었습니다.",
    	"data": null
    }
    ```

- body 데이터 형식 잘못됨

    ```json
    {
    	"code": 400,
    	"msg": "데이터 형식이 잘못되었습니다.",
    	"data": null
    }
    ```

- **인증**: 필요 없음

---

### **이메일 인증 코드 발송 (POST /api/v1/email/send)**

- **설명**: 회원가입을 위한 이메일 인증 코드를 발송합니다. 6자리 숫자로 구성된 인증 코드가 이메일로 전송됩니다.
- **요청 헤더**:

    ```
    Content-Type: application/json
    ```

- **요청 본문**:

    ```json
    {
        "mail": "example@gmail.com"
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
        "code": "200-SUCCESS",
        "msg": "인증코드가 발송되었습니다."
    }
    ```

- **응답 예시 (에러)**:

    ```json
    {
        "code": 500,
        "msg": "이메일 발송에 실패했습니다."
    }
    ```

- **참고사항**:
    - 인증 코드는 30분간 유효합니다.
    - 동일한 이메일로 재요청 시 이전 인증 코드는 무효화됩니다.

### **이메일 인증 코드 검증 (POST /api/v1/email/verify)**

- **설명**: 발송된 이메일 인증 코드의 유효성을 검증합니다.
- **요청 헤더**:

    ```
    Content-Type: application/json
    ```

- **요청 본문**:

    ```json
    {
        "mail": "example@gmail.com",
        "verifyCode": "123456"
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
        "code": "200-SUCCESS",
        "msg": "인증이 완료되었습니다."
    }
    ```

- **응답 예시 (에러)**:

    ```json
    {
        "code": "400-BAD_REQUEST",
        "msg": "인증에 실패했습니다."
    }
    ```

- **참고사항**:
    - 인증 코드는 숫자로만 구성된 6자리입니다.
    - 인증 시도는 코드가 만료되기 전까지 제한 없이 가능합니다.
    - 인증 성공 후에는 해당 인증 코드는 더 이상 사용할 수 없습니다.

### **제한사항**

- 이메일 형식이 올바르지 않은 경우 요청이 거부됩니다.
- 인증 코드는 30분 후 만료됩니다.
- 동일한 이메일에 대해 새로운 인증 코드 요청 시 이전 코드는 무효화됩니다.
- Redis를 사용하여 인증 코드를 관리합니다.

[개선 사항]

1. 인증 시도 회수 제한
2. 인증 코드 만료 시간 및 남은 시간 반환
3. 재시도 매커니즘 추가 및 비동기 처리
4. 유효성 검사

---

### **로그인 (POST /api/v1/users/login)**

- **설명**: 이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.
- **요청 예시 (JSON)**:

    ```json
    {
      "email": "user@example.com",
      "password": "password123"
    }
    ```

- **응답 예시 (성공)**:
- header

    ```json
    Set-Cookie: accessToken=<access-token>; HttpOnly; Path=/; Secure; SameSite=None
    Set-Cookie: refreshToken=<refresh-token>; HttpOnly; Path=/auth/refresh; Secure; SameSite=None
    Content-Type: application/json
    ```

- body

    ```json
    {
      "code": 200,
      "msg": "로그인 성공",
      "data": {
    	  "tokenType": "Bearer",
        "accessToken": "<access-token>",
        "refreshToken": "<refresh-token>"
      }
    }
    ```

- **응답 예시 (에러)**: (인증 실패 - 비밀번호 오류)

    ```json
    {
      "code": 401,
      "msg": "이메일 또는 비밀번호가 올바르지 않습니다. 다시 확인해 주세요.",
      "data": null
    }
    ```

---

### OAuth 로그인 ( 추가 필요 )

---

---

### **토큰 재발급 (POST /api/v1/users/refresh)**

- **설명**: Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.
- **요청 예시**: 쿠키에 refreshToken 전달
- **응답 예시(성공)**

    ```json
    {
    	"code": 200,
    	"msg": "토큰 재발급 성공",
    	"data": {
    			"tokenType": "Bearer",
    			"accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    		}
    }
    ```

- **응답 예시 (에러)**: (유효하지 않은 Refresh Token)

    ```json
    {
    	"code": 401,
    	"msg": "유효하지 않은 Refresh Token입니다.",
    	"data": null
    }
    ```

---

### **로그아웃 (POST /api/v1/users/logout)**

- **설명**: 클라이언트에서 JWT 토큰을 무효화하고 로그아웃합니다. (필요 시 서버에서 토큰 블랙리스트 처리)
- **요청 예시**: 쿠키에 accessToken, refreshToken 전달
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "로그아웃 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (인증 토큰 없음)

    ```json
    {
      "code": 401,
      "msg": "인증 정보가 필요합니다.",
      "data": null
    }
    ```

- **인증**: 예

---

### **회원탈퇴 (DELETE /api/v1/users/me)**

- **설명**: 로그인한 사용자의 계정을 삭제합니다.(유예기간 X)
- **요청 예시**: 쿠키에 accessToken, refreshToken 추가
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "회원 탈퇴 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (인증 실패)

    ```json
    {
      "code": 401,
      "msg": "인증 정보가 필요합니다.",
      "data": null
    }
    
    ```

- **인증**: 예

---

### **프로필 조회 (GET /api/v1/users/me)**

- **설명**: 로그인한 사용자의 프로필 정보를 조회합니다.
- **요청 예시**:  쿠키에 accessToken, refreshToken 추가
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "프로필 조회 성공",
      "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "sex": "MALE",
        "age": 23,
        "phoneNum": "010-1234-5678",
        "location": "Seoul",
        "role": "USER",
        "createdAt": "2025-04-24T10:00:00",
        "modifiedAt": "2025-04-24T12:30:00"
      }
    }
    ```

- **응답 예시 (에러)**: (토큰 만료)

    ```json
    {
      "code": 401,
      "msg": "토큰이 만료되었습니다. 다시 로그인해주세요.",
      "data": null
    }
    ```

- **인증**: 예

---

### **프로필 수정 (PUT /api/v1/users/me)**

- **설명**: 로그인한 사용자의 프로필 정보를 수정합니다. (이메일 변경 불가 또는 별도 인증 필요)
- **요청 예시**:  쿠키에 accessToken, refreshToken 추가
- **요청 예시 (JSON)**:

    ```json
    {
      "name": "홍길동",
      "password": "aesfaefaef",
      "phoneNum": "010-8765-4321",
      "location": "Busan"
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "프로필 수정 성공",
      "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "sex": "MALE",
        "age": 23,
        "phoneNum": "010-8765-4321",
        "location": "Busan",
        "role": "USER",
        "modifiedAt": "2025-04-24T13:00:00"
      }
    }
    ```

- **응답 예시 (에러)**: (잘못된 요청 - 필드 오류)

    ```json
    {
      "code": 400,
      "msg": "입력 값이 유효하지 않습니다.",
      "data": null
    }
    ```

- 인증 오류

    ```json
    {
      "code": 401,
      "msg": "인증이 필요합니다.",
      "data": null
    }
    ```

- **인증**: 예

### **유저 환불 (POST /api/v1/payments/{paymentKey}/cancel)**

- **설명**: 승인된 결제를 paymentKey로 취소합니다. 취소 이유를 cancelReason에 추가해야 합니다.
- 쿠키에 accessToken, refreshToken 추가
- **요청 예시 (JSON)**:

    ```json
    {
        "cancelReason": "단순 변심"
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
        "code": "200",
        "msg": "결제 취소 완료",
        "data": {
            "paymentKey": "tviva20250507101731LhTC6",
            "orderId": "order-1746580651517",
            "status": "PARTIAL_CANCELED",
            "method": "카드",
            "totalAmount": 1000,
            "receiptUrl": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20250507101731LhTC6&ref=PX",
            "cancels": [
                {
                    "cancelAmount": 1000,
                    "canceledAt": "2025-05-07T10:18:08",
                    "cancelReason": "단순 변심"
                }
            ]
        }
    }
    ```

- 인증 오류

    ```json
    {
      "code": 401,
      "msg": "인증이 필요합니다.",
      "data": null
    }
    ```

- **인증**: 예

---

## 2. 마이페이지 기능 API

~~현재 구매 이력 조회 기능의 response가 구매 상세 조회 기능의 response로 사용되고 있습니다.
추후 `구매 이력 조회 리스트`와 `구매 상세 조회`로 분리하여 진행 예정입니다.~~

수정 완료

### **구매 이력 조회 (GET /api/v1/users/me/purchases)**

- **설명**: 로그인한 사용자의 전체 구매 이력을 조회합니다.
- **요청 예시**: `GET /users/me/purchases` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "구매 이력 조회 성공",
      "data": [
        {
          "purchaseId": 5001,
          "itemName": "뮤지컬 공연 A",
          "amount": 80000,
          "purchaseDate": "2025-04-24T13:30:00",
          "paymentMethod": "TOSS",
          "paymentStatus": "PAID"
        },
        {
          "purchaseId": 5002,
          "itemName": "콘서트 B",
          "amount": 50000,
          "paymentMethod": "TOSS",
          "purchaseDate": "2025-03-20T20:00:00",
          "paymentStatus": "PAID"
        }
      ]
    }
    ```

- **응답 예시 (에러)**: (인증 필요)

    ```json
    {
      "code": 401,
      "msg": "로그인이 필요합니다.",
      "data": null
    }
    
    ```

- **인증**: 예

### **구매 상세 조회 (GET api/v1/user/me/purchases/{purchaseId})**

- **설명**: 특정 구매 내역의 상세 정보를 조회합니다.
- **요청 예시**: `GET /purchases/5001` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
        "status": "200-SUCCESS",
        "message": "구매 이력 조회 성공",
        "data": {
            "purchases": [
                {
                    "purchaseId": 5001,
                    "eventId": 123,           // 추가된 필드
                    "itemName": "콘서트 티켓",
                    "amount": 150000,
                    "purchaseDate": "2024-03-15T14:30:00",
                    "paymentMethod": "CARD",
                    "paymentStatus": "DONE",
                    "tickets": [
                        {
                            "ticketId": 10,
                            "seatLocation": "A1"
                        },
                        {
                            "ticketId": 11,
                            "seatLocation": "A2"
                        }
                    ]
                }
            ]
        }
    }
    ```

- **응답 예시 (에러)**: (존재하지 않는 구매)

    ```json
    {
      "code": 404,
      "msg": "구매 내역을 찾을 수 없습니다.",
      "data": null
    }
    
    ```

- **인증**: 예

## 3. 티켓 구매 및 결제 API

유저 티켓 구매 페이지 진입 → producer로 유저가 대기열에 들어옴 메시지 생성 → 대기열 서비스가 이 메시지를 consume해서 대기열에 유저를 추가 → 대기열 서비스에서 유저를 빼 → producer로
유저가 빠짐을 나타내는 메시지 생성 → 대기열 해제 알림 서비스에서 consumer로 메시지를 받아서 클라이언트에게 대기열을 빠져나왔음을 알림 → 좌석 선택 → producer로 메시지 생성 → consumer로
좌석 락 서비스가 메시지를 받아서 락을 검

```jsx
[유저
:
티켓
구매
페이지
접속
]
↓
[Producer
:
queue - enter
]
↓ Kafka(topic
:
user.queue.enter
)
[대기열 서비스 Consumer]
    ↓ Redis
Sorted
Set(queue
:
concert123
)
↓ 유저
순서
확인
후
조건
도달
시
    [Producer
:
queue - leave
]
↓ Kafka(topic
:
user.queue.leave
)
[알림 서비스 Consumer]
    ↓ 클라이언트(WebSocket / Push)
    [좌석
선택
페이지
진입
]
↓
[Producer
:
seat - lock - request
]
↓ Kafka(topic
:
seat.lock
)
[좌석 락 서비스 Consumer]
    ↓ Redis
Lock
or
Lua
```

### **대기열 진입 (POST /api/v1/event/{event-id}/tickets/waiting)**

- **설명:** 특정 행사의 티켓을 구매하기 위해 대기열에 로그인한 유저를 진입시킵니다. 이때 클라이언트는 프런트 서버와 웹소켓을 연결해 실시간으로 자신의 대기열 번호를 확인합니다. 웹소켓 연결이 끊어질 경우
  대기열에서 나오게 됩니다.
- **응답 예시 (JSON):**

  대기열 순서를 반환합니다.

    ```json
    {
    	"code": 200,
    	"msg": "대기열 추가 완료",
    	"data": {
    		"order": 123
    	}
    }
    ```

### **대기열 조회 (GET /api/v1/waiting)**

- **설명**: 로그인한 유저의 대기열 번호를 조회합니다.
- **응답 예시(JSON)**:

    ```json
    {
    	"code": 200,
    	"msg": "대기열 조회 완료",
    	"data": {
    		"order": 123,
    		"status" : "waiting" / "done"
    	}
    }
    ```

### **대기열 취소 (DELETE /api/v1/waiting)**

- **설명:** 로그인한 유저를 대기열에서 내보냅니다.
- **응답 예시 (JSON):**

    ```json
    {
    	"code": 200,
    	"msg": "대기열 삭제 완료"
    }
    ```

### **좌석 조회 (GET /api/v1/event/{event-id}/seats)**

- **설명** : 행사의 좌석 레이아웃을 조회합니다.
- **응답 예시 ( 성공 )**:

    ```json
    {
        "code": "200",
        "msg": "좌석 조회 성공",
        "data": {
            "seats": [
                {
                    "seatId": 243,
                    "location": "A1",
                    "grade": "A",
                    "available": true
                },
                {
                    "seatId": 244,
                    "location": "A2",
                    "grade": "S",
                    "available": true
                },
                ...
            ],
            "layout": [
                [
                    null,
                    null,
                    "A1",
                    "A2",
                    "A3",
                    null,
                    null
                ],
                [
                    "B1",
                    "B2",
                    "B3",
                    "B4",
                    "B5",
                    "B6",
                    "B7"
                ]
            ]
        }
    }
    ```

- **응답 예시 ( 실패 → 해당하는 이벤트 ID가 존재하지 않을 때 )**:

    ```json
    {
    	"code": 404,
    	"msg" : "행사가 존재하지 않습니다."
    }
    ```

### **좌석 선택 (POST /api/v1/event/{event-id}/seats)**

- **설명**: 대기열을 통과한 사용자가 희망하는 좌석(최대 4개)을 선택합니다.
- 요청 예시(JSON)
- data: seat - [seat1, seat2, seat3, seat4] → 고정 LIST

    ```json
    {
    	"seatList": ["1", "2"],
    	"ticketCount": 2
    }
    ```

    ```json
    {
    	"code": 200,
    	"msg": "좌석 선택 완료",
    	"data": {
    			"seatList": ["A1", "A2"]
    	}
    }
    ```

### **좌석 취소 (DELETE /api/v1/event/{event-id}/seats)**

- 설명 : 선택한 좌석을 취소합니다. → 좌석을 선택할 때 락을 걸건지
- 요청 예시 (JSON) → seat의 id를 담은 List

    ```json
    {
    	"seatList":[ "1", "2" ]
    }
    ```

- 응답 예시 ( 성공 )

    ```json
    {
    	"code": 200,
    	"msg": "좌석 취소 성공"
    }
    ```

- 실패

    ```java
    {
    	"code": 404,
    	"msg": "해당 좌석을 찾을 수 없습니다.",
    }
    ```

---

결제 플로우

좌석 고르고 구매 버튼 클릭 → api 호출 ( metadata )Uuid 생성해서 Payment 객체를 생성. 결제 상태가 결제 진행중. Uuid : Payment 객체의 ID, → 결제 페이지 팝업 → api
호출 결제 진행 → 결제 UUID 가 업데이트 됨. 결제 상태가 결제 완료로 변경. → ticket 객체 생성 및 저장.

- ID (Primary Key)
- 결제 UUID
- 사용자 ID (Foreign Key)
- 구매 일시
- 결제 금액
- 결제 수단
- 결제 상태 ( 결제 진행중 / 결제 완료 / 결제 취소 )

### 결제 사전 등록 (POST /api/v1/**payments**/init)

- **설명**: 사용자가 “결제하기” 버튼을 누른 직후, requestPayment() 호출 직전에 호출되는 api입니다.
- 요청

```json
{
  "eventId": 1,
  "amount": 1000
}
```

- 응답

```json
{
  "code": "200",
  "message": "결제 준비 완료",
  "data": {
    "puchaseId": 1,
    "status": "IN_PROGRESS"
  }
}
```

### 티켓 구매 (POST /api/v1/payments/confirm)

- **설명:** **결제 완료 후 successUrl로 리다이렉트 되었을 때**, 프론트에서 호출
  사용자가 좌석을 선택하지 않는 행사의 티켓을 구매합니다.
  시스템에서 자동으로 좌석을 배정하고, 결제 후 티켓을 생성합니다.
- 요청

```json
{
  "purchaseId": 50,
  "paymentKey": "tviva20250502114628Tfml5",
  "orderId": "order-1746153988430",
  "amount": 1000
}
```

- 응답

```json
{
  "code": "200",
  "msg": "결제 승인 완료",
  "data": {
    "paymentKey": "tviva20250502114628Tfml5",
    "orderId": "order-1746153988430",
    "orderName": "지정석 2매",
    "totalAmount": 1000,
    "status": "DONE",
    "method": "카드",
    "approvedAt": "2025-05-02T02:46:53",
    "receipt": {
      "url": "https://dashboard.tosspayments.com/receipt/redirection?transactionId=tviva20250502114628Tfml5&ref=PX"
    }
  }
}
```

[흐름](https://www.notion.so/1df3550b7b5580e180ceda3b28993f26?pvs=21)

---

## 4. 알림 API

### **알림 구독 (GET /api/v1/notifications/subscribe)**

- **설명**: 시스템 이벤트 발생 시에 알림을 생성하고, 이를 토스트 메시지로 즉시 보여주는 기능
- **요청 예시**: `GET /api/v1/notifications/subscribe`  (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "알림 구독 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (인증 실패)

    ```json
    {
      "code": 401,
      "msg": "로그인이 필요합니다.",
      "data": null
    }
    ```

- **인증**: 예

### **알림 목록 조회 (GET /api/v1/notifications)**

- **설명**: 로그인한 사용자의 모든 알림 내역을 조회합니다.
- **요청 예시**: `GET /notifications` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "알림 목록 조회 성공",
      "data": [
        {
          "notificationId": 3001,
          "type": "SYSTEM",
          "content": "예매가 완료되었습니다.",
          "sentAt": "2025-04-24T13:30:05",
          "isRead": false
        },
        {
          "notificationId": 3002,
          "type": "SYSTEM",
          "content": "오늘 개최되는 이벤트가 있습니다.",
          "sentAt": "2025-04-24T08:00:00",
          "isRead": true
        }
      ]
    }
    ```

- **응답 예시 (에러)**: (인증 실패)

    ```json
    {
      "code": 401,
      "msg": "로그인이 필요합니다.",
      "data": null
    }
    ```

- **인증**: 예

### **알림 상세 조회 (GET /api/v1/notifications/{id})**

- **설명**: 특정 알림의 상세 내용을 조회하고, 읽음 상태를 업데이트할 수 있습니다.
- **요청 예시**: `GET /notifications/3001` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "알림 조회 성공",
      "data": {
        "notificationId": 3001,
        "type": "SYSTEM",
        "content": "예매가 완료되었습니다.",
        "sentAt": "2025-04-24T13:30:05",
        "isRead": true
      }
    }
    ```

- **응답 예시 (에러)**: (알림 없음)

    ```json
    {
      "code": 404,
      "msg": "해당 알림을 찾을 수 없습니다.",
      "data": null
    }
    ```

- **인증**: 예

### **알림 생성 (POST /api/v1/notifications)**

- **설명**: (관리자/시스템 전용) 특정 사용자에게 새로운 알림을 생성합니다.
- **요청 예시 (JSON)**:

    ```json
    {
      "userId": 1,
      "type": "SYSTEM",
      "content": "관리자 공지사항이 등록되었습니다."
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "알림 생성 성공",
      "data": {
        "notificationId": 3003,
        "userId": 1,
        "type": "SYSTEM",
        "content": "관리자 공지사항이 등록되었습니다.",
        "sentAt": "2025-04-24T14:00:00"
      }
    }
    ```

- **응답 예시 (에러)**: (잘못된 요청)

    ```json
    {
      "code": 400,
      "msg": "유효하지 않은 요청입니다.",
      "data": null
    }
    
    ```

- **인증**: 예 (관리자 권한 필요)

### **미읽음 알림 조회 (GET /api/v1/notifications/unread)**

- **설명**: 로그인한 사용자의 읽지 않은 알림을 페이지네이션 형태로 조회합니다.
- **요청 예시**: `GET /notifications/unread` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "미읽은 알림 조회 성공",
      "data": {
        "content": [
          {
            "notificationId": 3001,
            "type": "SYSTEM",
            "content": "예매가 완료되었습니다.",
            "sentAt": "2025-04-24T13:30:05",
            "isRead": false
          },
          {
            "notificationId": 3005,
            "type": "EVENT",
            "content": "내일 행사가 진행됩니다.",
            "sentAt": "2025-04-23T10:15:20",
            "isRead": false
          }
        ],
        "pageable": {
          "pageNumber": 0,
          "pageSize": 20,
          "sort": {
            "orders": [
              {
                "direction": "DESC",
                "property": "sentAt"
              }
            ]
          },
          "offset": 0,
          "paged": true,
          "unpaged": false
        },
        "totalElements": 5,
        "totalPages": 1,
        "last": true,
        "size": 20,
        "number": 0,
        "sort": {
          "orders": [
            {
              "direction": "DESC",
              "property": "sentAt"
            }
          ]
        },
        "numberOfElements": 2,
        "first": true,
        "empty": false
      }
    }
    ```

- **응답 예시 (에러)**: (인증 실패)

    ```json
    {
      "code": 401,
      "msg": "로그인이 필요합니다.",
      "data": null
    }
    ```

### **알림 삭제 - 단건 (DELETE /api/v1/notifications/{id})**

- **설명**: 특정 ID의 알림을 삭제합니다.
- **응답 예시 (성공)**:

    ```json
    {
      "code": "200-SUCCESS",
      "msg": "알림 삭제 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (알림이 없거나 권한이 없는 경우)

    ```json
    {
      "code": "400-BAD_REQUEST",
      "msg": "해당 알림을 찾을 수 없습니다.",
      "data": null
    }
    ```

- **인증**: 예 (사용자 본인의 알림만 삭제 가능)

### **알림 삭제 - 다건 (DELETE /api/v1/notifications)**

- **설명**: 여러 개의 알림을 동시에 삭제합니다.
- **요청 예시 (JSON)**:

    ```json
    {
      "notificationIds": [11, 12, 13]
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
      "code": "200-SUCCESS",
      "msg": "알림 삭제 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (일부 알림에 대한 권한이 없는 경우)

    ```json
    {
      "code": "400-BAD_REQUEST",
      "msg": "일부 알림에 접근할 권한이 없습니다.",
      "data": null
    }
    ```

- **인증**: 예 (사용자 본인의 알림만 삭제 가능)

### **알림 삭제 - 전체 (DELETE /api/v1/notifications/all)**

- **설명**: 현재 로그인한 사용자의 모든 알림을 삭제합니다.
- **응답 예시 (성공)**:

    ```json
    {
      "code": "200-SUCCESS",
      "msg": "모든 알림 삭제 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (인증 실패 시)

    ```json
    {
      "code": "401-UNAUTHORIZED",
      "msg": "인증에 실패했습니다.",
      "data": null
    }
    ```

- **인증**: 예 (로그인 필요)
- **참고**: 사용자의 모든 알림을 영구적으로 삭제

## 5. 행사 조회 및 상세 API

### **이벤트 목록 조회 (GET /api/v1/events)**

- **설명**: 전체 이벤트 목록을 조회합니다. 필터(장르, 날짜 등)는 쿼리 파라미터로 전달할 수 있습니다.
- location : 실제 주소
- hallName : 행사장의 이름
- **요청 예시**: `GET /events?type=2&startDate=2025-05-01`
    - 필터 종류
        - 지역별 (`location= 서울/부산/...`)
            - 시 별로 필터링합니다.
        - 금액대별( `minPrice=&maxPrice=`)
            - 가격은 가장 저렴한 티켓을 기준으로 검색합니다.
        - 타입별 ( `type=` 뮤지컬, 콘서트, 스포츠, 전시 , … )
        - 티켓팅 가능 상태 : (`status=`티켓팅이 “종료된 / 진행중인 / 예정인” 행사 )
        - 일자별 (`startDate=&endDate=` )
    - 제목으로 검색 ( `title=` )

      ![image.png](attachment:26deb07b-a27f-41e0-82a0-9fc1fb5315a2:image.png)

- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "이벤트 목록 조회 성공",
      "data": [
        {
          "eventId": 10,
          "title": "뮤지컬 공연 A",
          "type": "MUSICAL",
           "manager_id": 12,
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "status": "AVAILABLE",
          "startDate": "2025-05-10T19:00:00",
          "endDate": "2025-05-10T21:00:00",
          "location": "Seoul Art center",
          "hallName": "Art center",
          "price": 20000
        },
        {
          "eventId": 11,
          "title": "콘서트 B",
          "type": "CONCERT",
           "manager_id": 13,
          "thumbnailUrl": "https://example.com/thumb2.jpg",
          "startDate": "2025-05-15T18:00:00",
          "endDate": "2025-05-15T20:00:00",
          "status": "AVAILABLE",
          "location": "Seoul Olympic Stadium",
          "hallName": "Art center",
          "price": 20000
        }
      ]
    }
    ```

- **응답 예시 (에러)**: (잘못된 필터 값)

    ```json
    {
      "code": 400,
      "msg": "유효하지 않은 쿼리 파라미터입니다.",
      "data": null
    }
    ```

- **인증**: 불필요

### **이벤트 상세 조회 (GET /api/v1/events/{id})**

- **설명**: 특정 이벤트의 상세 정보를 조회합니다.
- **요청 예시**: `GET /events/10`
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "이벤트 상세 조회 성공",
      "data": {
        "eventId": 10,
        "title": "뮤지컬 공연 A",
        "type": "MUSICAL",
        "description": "화려한 뮤지컬 공연입니다.",
        "restriction": "촬영 금지",
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "startDate": "2025-05-10T19:00:00",
        "endDate": "2025-05-10T21:00:00",
        "location": "Seoul Arts Center",
        "hallName": "Arts center",
        "seatCount": 500,
        "price": 40000,
        "bookingStart": "2025-04-01T10:00:00",
        "bookingEnd": "2025-05-09T23:59:59"
        "agelimit": 15,
        "viewCount": 123,
        "created_at":"2025-03-01T10:00:00",
        "modified_at":"2025-03-01T10:00:00",
        "status": "AVAILABLE"   
      }
    }
    ```

- **응답 예시 (에러)**: (이벤트 없음)

    ```json
    {
      "code": 404,
      "msg": "해당 이벤트를 찾을 수 없습니다.",
      "data": null
    }
    ```

- **인증**: 불필요

### **남은 좌석수 조회 (GET /api/v1/events/{id}/seats)**

- **설명**: 특정 이벤트의 남은 좌석 숫자를 조회합니다.
- **요청 예시**: `GET /events/10/seats`
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "좌석 정보 조회 성공",
      "data": {
    	  "available": 12
    	}
    }
    ```

- **응답 예시 (에러)**: (이벤트 없음)

    ```json
    {
      "code": 404,
      "msg": "이벤트를 찾을 수 없습니다.",
      "data": null
    }
    ```

- **인증**: 불필요

### **카테고리 목록 조회 (GET /api/v1/event-types)**

- **설명**: 이벤트 장르(종류) 목록을 조회합니다.
- **요청 예시**: `GET /event-types`
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "이벤트 유형 조회 성공",
      "data": [
        {"typeId": 1, "name": "MUSICAL"},
        {"typeId": 2, "name": "CONCERT"},
        {"typeId": 3, "name": "THEATER"}
      ]
    }
    
    ```

- **응답 예시 (에러)**: (서버 오류)

    ```json
    {
      "code": 500,
      "msg": "서버 에러가 발생했습니다.",
      "data": null
    }
    
    ```

- **인증**: 불필요

## 6. 매니저 기능 API

### **이벤트 생성 (POST /api/v1/manager/events)**

- **설명**: 매니저가 새로운 이벤트를 등록합니다.
- **요청 예시 (JSON)**:

    ```json
    {
        "title": "뮤지컬 공연 A",
        "type": "MUSICAL",
        "description": "화려한 뮤지컬 공연입니다.",
        "restriction": "촬영 금지",
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "startDate": "2025-05-10T19:00:00",
        "endDate": "2025-05-10T21:00:00",
        "location": "Seoul Arts Center",
        "hallName": "Arts center",
        "seatCount": 10,
        "layout": {
          "layout": [
              [
                  null,
                  null,
                  "A1",
                  "A2",
                  "A3",
                  null,
                  null
              ],
              [
                  "B1",
                  "B2",
                  "B3",
                  "B4",
                  "B5",
                  "B6",
                  "B7"
              ]
          ],
          "seat": {
              "A1": {
                  "grade": "A"
              },
              "A2": {
                  "grade": "S"
              },
              "A3": {
                  "grade": "S"
              },
              "B1": {
                  "grade": "A"
              },
              "B2": {
                  "grade": "A"
              },
              "B3": {
                  "grade": "A"
              },
              "B4": {
                  "grade": "A"
              },
              "B5": {
                  "grade": "A"
              },
              "B6": {
                  "grade": "A"
              },
              "B7": {
                  "grade": "A"
              }
          }
        },
        "price": [
    	    {
    		    "grade": "A",
    		    "amount": 50000
    		  },
    		  {
    			  "grade": "S",
    				"amount": 100000
    			}
    		],
        "bookingStart": "2025-04-01T10:00:00",
        "bookingEnd": "2025-05-09T23:59:59",
        "agelimit": 15 
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
        "eventId": 1,
        "title": "뮤지컬 공연 A",
        "type": "MUSICAL",
        "description": "화려한 뮤지컬 공연입니다.",
        "restriction": "촬영 금지",
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "startDate": "2025-05-10T19:00:00",
        "endDate": "2025-05-10T21:00:00",
        "location": "Seoul Arts Center",
        "hallName": "Arts center",
        "seatCount": 10,
        "layout": {
            "layout": [
                [
                    null,
                    null,
                    "A1",
                    "A2",
                    "A3",
                    null,
                    null
                ],
                [
                    "B1",
                    "B2",
                    "B3",
                    "B4",
                    "B5",
                    "B6",
                    "B7"
                ]
            ],
            "seat": {
                "A1": {
                    "grade": "A"
                },
                "A2": {
                    "grade": "S"
                },
                "A3": {
                    "grade": "S"
                },
                "B1": {
                    "grade": "A"
                },
                "B2": {
                    "grade": "A"
                },
                "B3": {
                    "grade": "A"
                },
                "B4": {
                    "grade": "A"
                },
                "B5": {
                    "grade": "A"
                },
                "B6": {
                    "grade": "A"
                },
                "B7": {
                    "grade": "A"
                }
            }
        },
        "price": [
            {
                "grade": "A",
                "amount": 50000
            },
            {
                "grade": "S",
                "amount": 100000
            }
        ],
        "bookingStart": "2025-04-01T10:00:00",
        "bookingEnd": "2025-05-09T23:59:59",
        "agelimit": 10,
        "createdAt": "2025-05-09T23:59:59",
        "modifiedAt":"2025-05-09T23:59:59",
        "status": "available"
    }
    ```

- **응답 예시 (에러)**: (필수 항목 누락)

    ```json
    {
      "code": 400,
      "msg": "필수 입력 항목이 누락되었습니다.",
      "data": null
    }
    ```

- **인증**: 예 (매니저 권한 필요, `Authorization: Bearer <token>`)

### **이벤트 수정 (PUT /api/v1/manager/events/{event-Id})**

- **설명**: 매니저가 기존 이벤트 정보를 수정합니다.
- **요청 예시 (JSON)**:

    ```json
    {
        "title": "바뀐 뮤지컬 공연 A",
        "type": "MUSICAL",
        "description": "바뀐 화려한 뮤지컬 공연입니다.",
        "restriction": "바뀐 촬영 금지",
        "thumbnailUrl": "https://example.com/바뀐-thumb.jpg",
        "location": "바뀐 Seoul Arts Center",
        "hallName": "바뀐 Arts center",
        "seatCount": 12,
        "layout": {
            "layout": [
                [
                    null,
                    null,
                    "A1",
                    "A2",
                    "A3",
                    "A4",
                    "A5"
                ],
                [
                    "B1",
                    "B2",
                    "B3",
                    "B4",
                    "B5",
                    "B6",
                    "B7"
                ]
            ],
            "seat": {
                "A1": {
                    "grade": "A"
                },
                "A2": {
                    "grade": "S"
                },
                "A3": {
                    "grade": "S"
                },
                "A4": {
                    "grade": "S"
                },
                "A5": {
                    "grade": "S"
                },
                "B1": {
                    "grade": "A"
                },
                "B2": {
                    "grade": "A"
                },
                "B3": {
                    "grade": "A"
                },
                "B4": {
                    "grade": "A"
                },
                "B5": {
                    "grade": "A"
                },
                "B6": {
                    "grade": "A"
                },
                "B7": {
                    "grade": "A"
                }
            }
        },
        "price": [
            {
                "grade": "A",
                "amount": 50000
            },
            {
                "grade": "S",
                "amount": 100000
            }
        ],
        "agelimit": 18
    }
    ```

- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "이벤트 수정 성공",
      "data": {
    	    "eventId": 1,
    	    "title": "바뀐 뮤지컬 공연 A",
    	    "type": "MUSICAL",
    	    "description": "바뀐 화려한 뮤지컬 공연입니다.",
    	    "restriction": "바뀐 촬영 금지",
    	    "thumbnailUrl": "https://example.com/바뀐-thumb.jpg",
    	    "startDate": "2025-05-10T19:00:00",
    	    "endDate": "2025-05-10T21:00:00",
    	    "location": "바뀐 Seoul Arts Center",
    	    "hallName": "바뀐 Arts center",
    	    "seatCount": 12,
    	    "layout": {
    	        "layout": [
    	            [
    	                null,
    	                null,
    	                "A1",
    	                "A2",
    	                "A3",
    	                "A4",
    	                "A5"
    	            ],
    	            [
    	                "B1",
    	                "B2",
    	                "B3",
    	                "B4",
    	                "B5",
    	                "B6",
    	                "B7"
    	            ]
    	        ],
    	        "seat": {
    	            "A1": {
    	                "grade": "A"
    	            },
    	            "A2": {
    	                "grade": "S"
    	            },
    	            "A3": {
    	                "grade": "S"
    	            },
    	            "A4": {
    	                "grade": "S"
    	            },
    	            "A5": {
    	                "grade": "S"
    	            },
    	            "B1": {
    	                "grade": "A"
    	            },
    	            "B2": {
    	                "grade": "A"
    	            },
    	            "B3": {
    	                "grade": "A"
    	            },
    	            "B4": {
    	                "grade": "A"
    	            },
    	            "B5": {
    	                "grade": "A"
    	            },
    	            "B6": {
    	                "grade": "A"
    	            },
    	            "B7": {
    	                "grade": "A"
    	            }
    	        }
    	    },
    	    "price": [
    	        {
    	            "grade": "A",
    	            "amount": 50000
    	        },
    	        {
    	            "grade": "S",
    	            "amount": 100000
    	        }
    	    ],
    	    "bookingStart": "2025-04-01T10:00:00",
    	    "bookingEnd": "2025-05-09T23:59:59",
    	    "agelimit": 18,
    	    "createdAt": "2025-05-09T23:59:59",
    	    "modifiedAt":"2025-05-12T23:59:59",
    	    "status": "AVAILABLE"
    	}
    }
    ```

- **응답 예시 (에러)**: (권한 없음)

    ```json
    {
      "code": 403,
      "msg": "해당 이벤트를 수정할 권한이 없습니다.",
      "data": null
    }
    
    ```

- **인증**: 예 (매니저 권한)

### **이벤트 삭제 (PATCH /api/v1/manager/events/{eventId})**

- **설명**: 매니저가 특정 이벤트를 삭제합니다. 이벤트의 상태가 “삭제됨”으로 변경됩니다.
- **요청 예시**: `DELETE /manager/events/12` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "이벤트 삭제 성공",
      "data": null
    }
    ```

- **응답 예시 (에러)**: (이벤트 없음)

    ```json
    {
      "code": 404,
      "msg": "해당 이벤트를 찾을 수 없습니다.",
      "data": null
    }
    ```

- **인증**: 예 (매니저 권한)

### **자신이 작성한 이벤트 목록 (GET /api/v1/manager/events)**

- **설명**: 매니저가 자신이 등록한 이벤트 목록을 조회합니다.
- **요청 예시**: `GET /manager/events` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "매니저 이벤트 목록 조회 성공",
      "data": [
         {
          "eventId": 1,
          "title": "뮤지컬 공연 A",
          "type": "MUSICAL",
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "status": "AVAILABLE",
          "startDate": "2025-05-10T19:00:00",
          "endDate": "2025-05-10T21:00:00",
          "location": "Seoul Art center",
          "hallName": "Art center",
          "price": 20000
        },
        {
          "eventId": 2,
          "title": "뮤지컬 공연 B",
          "type": "MUSICAL",
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "status": "AVAILABLE",
          "startDate": "2025-05-10T19:00:00",
          "endDate": "2025-05-10T21:00:00",
          "location": "Seoul Art center",
          "hallName": "Art center",
          "price": 10000
        },
        {
          "eventId": 2,
          "title": "뮤지컬 공연 C",
          "type": "MUSICAL",
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "status": "REMOVED",
          "startDate": "2025-05-10T19:00:00",
          "endDate": "2025-05-10T21:00:00",
          "location": "Seoul Art center",
          "hallName": "Art center",
          "price": 30000
        }
      ]
    }
    ```

- **응답 예시 (에러)**: (권한 없음)

    ```json
    {
      "code": 403,
      "msg": "권한이 없는 요청입니다.",
      "data": null
    }
    ```

- **인증**: 예 (매니저 권한)

### **행사의 티켓 구매 내역 조회 (GET /api/v1/manager/{eventId}/purchases)**

- **설명**: 매니저가 행사의 구매 내역을 조회합니다.
- **요청 예시**: `GET /api/v1/manager/purchases` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "구매 내역 조회 성공",
      "data": [
      {
        "purchaseId": 5001,
        "userId" :1,
        "userName": "홍길동",
        "userEmail": "example@google.com"
        "payment_status": "CANCELED",
        "ticket_id": [10,11,12,13],
        "amount": 150,000
      },
      {
        "purchaseId": 5022,
        "userId" :2,
        "userName": "아무개",
        "userEmail": "example@google.com"
        "payment_status": "CANCELED",
        "ticket_id": [43,44,45,46],
        "amount": 350,000
      },
      ...
      ]
    }
    
    ```

- **응답 예시 (에러)**: (존재하지 않는 eventId)

    ```json
    {
      "code": 404,
      "msg": "잘못된 이벤트 ID 입니다.",
      "data": null
    }
    
    ```

- **인증**: 예 (매니저 권한)

### **매니저 환불 처리 (POST /api/v1/manager/purchases/{purchase-id}/refund)**

- **설명**: 매니저가 특정 구매 건에 대해 환불을 처리합니다.
- **요청 예시**: `POST /manager/purchases/5001/refund` (쿠키에 토큰 포함)
- 단건/다건

    ```json
    {
    	purchaseIds:[5001, 5002],
    	eventId: null
    }
    ```

- 일괄

    ```json
    {
    	purchaseIds: null,
    	eventId: 10
    }
    ```

- Purchase 테이블에 새로운 열을 추가하는걸로 처리
    - payment_status = CANCELED
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "환불 처리 성공",
      "data": {
        "purchaseId": 5001,
        "userId" :1,
        "payment_status": "CANCELED",
        "ticket_id": [10,11,12,13],
        "refundAmount": 80000,
        "refundDate": "2025-04-24T15:00:00"
      }
    }
    
    ```

- **응답 예시 (에러)**: (이미 환불된 구매)

    ```json
    {
      "code": 409,
      "msg": "이미 환불 처리된 구매입니다.",
      "data": null
    }
    
    ```

- **인증**: 예 (매니저 권한)

### ~~행사의 티켓을 구매한 사용자 조회( GET /api/v1/manager/events/{event-id}/buyers)~~

- **~~설명:** event-id에 해당하는 행사의 티켓을 구매한 사용자의 리스트를 조회합니다.~~
- ~~응답 예시~~

    ```json
    {
      "code": 200,
      "msg": "사용자 목록 조회 성공",
      "data": [
    		{
    		    "id": 1,
    		    "email": "user@example.com",
    		    "name": "홍길동",
    		    "phoneNum": "010-1234-5678",
    		    "role": "USER",
    		    "createdAt": "2025-04-24T10:00:00",
    		    "modifiedAt": "2025-04-24T12:30:00"
    		}
      ]
    }
    ```

- ~~인증 : 필요~~

  ~~로그인한 매니저는 자신의 행사의 구매자만 조회할 수 있습니다.~~

---

## 7. 후순위

### 좋아요 토글( PUT /api/v1/events/{event-id}/like )

- 설명 : 유저가 좋아요를 누른 상태라면 좋아요를 삭제하고, 좋아요를 누르지 않은 상태라면 좋아요를 추가한다.
- 요청 예시 : `PUT /api/v1/events/{event-id}/like`
- 응답 예시

    ```json
    {
      "code": 200,
      "msg": "좋아요 추가 성공",
      "data": [
        {
          "eventId": 5001,
          "userId": 12
        }
      ]
    }
    ```

- 인증 : 필요

### 좋아요 눌렀는지 확인 ( GET /api/v1/events{event-id}/like )

- 설명 : 유저가 특정 행하세 좋아요를 눌렀는지 확인합니다.
- 요청 예시 : `GET /api/v1/events/{event-id}/like`
- 응답 예시

```json
{
  "code": 200,
  "msg": "좋아요 조회 성공",
  "data": {
    "like": True,
    "event-id": 12
  }
}
```

### **관심 행사 목록 조회 (GET /api/v1/users/me/likes)**

- **설명**: 사용자가 찜(좋아요)한 이벤트 목록을 조회합니다.
- **요청 예시**: `GET /users/me/likes` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "찜한 이벤트 목록 조회 성공",
      "data": [
        {
          "eventId": 10,
          "title": "뮤지컬 공연 A",
          "type": "MUSICAL",
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "status": "AVAILABLE",
          "startDate": "2025-05-10T19:00:00",
          "endDate": "2025-05-10T21:00:00",
          "location": "Seoul Art center",
          "hallName": "Art center",
          "price": 20000
        }
        // 생략
      ]
    }
    ```

- **응답 예시 (에러)**: (찜한 이벤트 없음)

    ```json
    {
      "code": 404,
      "msg": "찜한 이벤트가 없습니다.",
      "data": null
    }
    ```

- **인증**: 예

### **티켓 판매 통계 조회 (GET /api/v1/events/{eventId}/statistics)**

- **설명**: 특정 이벤트의 판매 통계 정보를 조회합니다.
- **요청 예시**: `GET /events/10/statistics` (쿠키에 토큰 포함)
    - 연령별
    - 성별 예매율
- **응답 예시 (성공):**

    ```json
    {
    	"code": 200,
    	"msg": "통계 조회 성공",
    	"data":{
    		"age_statistics": [ 10.10, 20.30, 30.50,30.10],
    		"sex_statistics": [ 50.00, 50.00]
    	}
    }
    ```

- **인증**: 예 (매니저 권한)

운영자 기능 API

### 운영자 회원가입 ( POST /api/v1/admin)

- **설명:** 운영자를 새로 등록합니다.
- **요청 예시**:

    ```json
    {
      "email": "user@example.com",
      "password": "password123",
      "name": "홍길동",
      "age": 23,
      "sex": "MALE",
      "phoneNum": "010-1234-5678",
      "location": "Seoul"
    }
    ```

- **응답 예시 (성공):**

    ```json
    {
      "code": 200,
      "msg": "회원가입 성공",
      "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "age": 23,
        "sex": "MALE",
        "phoneNum": "010-1234-5678",
        "location": "Seoul",
        "role": "ADMIN",
        "createdAt": "2025-04-24T10:00:00"
      }
    }
    
    ```

### 운영자 로그인( POST /api/v1/admin/login

- **설명:** 운영자가 로그인합니다.
- **요청 예시**:

    ```json
    {
      "email": "user@example.com",
      "password": "password123"
      
    }
    ```

- **응답 예시 (성공):**

    ```json
    {
      "code": 200,
      "msg": "로그인 성공",
      "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "age": 23,
        "sex": "MALE",
        "phoneNum": "010-1234-5678",
        "location": "Seoul",
        "role": "ADMIN",
        "createdAt": "2025-04-24T10:00:00"
      }
    }
    
    ```

### **사용자 목록 조회 (GET /api/v1/admin/users)**

- **설명**: 운영자가 모든 사용자 목록을 조회합니다.
- **요청 예시**: `GET /admin/users` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "사용자 목록 조회 성공",
      "data": [
    		{
    		    "id": 1,
    		    "email": "user@example.com",
    		    "name": "홍길동",
    		    "phoneNum": "010-1234-5678",
    		    "role": "USER",
    		    "createdAt": "2025-04-24T10:00:00",
    		    "modifiedAt": "2025-04-24T12:30:00"
    		}
      ]
    }
    ```

- **응답 예시 (에러)**: (권한 없음)

    ```json
    {
      "code": 403,
      "msg": "운영자 권한이 필요합니다.",
      "data": null
    }
    
    ```

- **인증**: 예 (운영자 권한)

### **사용자 상세 조회 (GET /api/v1/admin/users/{id})**

- **설명**: 운영자가 특정 사용자의 상세 정보를 조회합니다.
- **요청 예시**: `GET /admin/users/1` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "사용자 상세 조회 성공",
      "data": {
        "id": 1,
        "email": "user@example.com",
        "name": "홍길동",
        "sex": "MALE",
        "age": 23,
        "phoneNum": "010-1234-5678",
        "location": "Seoul",
        "role": "USER",
        "createdAt": "2025-04-24T10:00:00",
        "modifiedAt": "2025-04-24T12:30:00"
      }
    }
    
    ```

- **응답 예시 (에러)**: (사용자 없음)

    ```json
    {
      "code": 404,
      "msg": "해당 사용자를 찾을 수 없습니다.",
      "data": null
    }
    
    ```

- **인증**: 예 (운영자 권한)

### **사용자 정지 (DELETE /admin/users/{id})**

- **설명**: 운영자가 특정 사용자를 삭제합니다.
- 사용자 role enum을 “BANNED”로 변경
- **요청 예시**: `DELETE /admin/users/2` (쿠키에 토큰 포함)
- **응답 예시 (성공)**:

    ```json
    {
      "code": 200,
      "msg": "사용자 정지 성공",
      "data": null
    }
    
    ```

- **응답 예시 (에러)**: (권한 없음)

    ```json
    {
      "code": 403,
      "msg": "운영자 권한이 필요합니다.",
      "data": null
    }
    ```

- **인증**: 예 (운영자 권한)