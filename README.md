# Ticket Queue System

## 프로젝트 목표
- 추석 때, KTX 예매 서버가 터지는 상황을 보고, 트래픽이 집중되는 상황에도 안정적으로 운영되는 대기열 시스템을 구현해보고자 만든 개인 프로젝트입니다.
- 수만 명이 동시에 접속하더라도 **대기열 진입 → 순위 확인 → 입장 토큰 발급 → 예약 확정** 흐름이 끊기지 않는 시스템을 검증하는 것이 목표입니다.

## 시스템 구성
| 구성 요소 | 설명 |
| --- | --- |
| Spring Boot API 서버 | REST API와 SSE(Server-Sent Events) 엔드포인트를 제공하며 대기열 로직, 토큰 발급, 스케줄러가 동작합니다. |
| Kafka | `/api/queue/enter` 요청을 비동기 메시지 큐에 적재해 HTTP 피크를 완화합니다. |
| Redis | 대기열(ZSet), 활성 사용자(Set), 입장 토큰(Key-Value) 등 실시간 상태를 저장하며 Lua 스크립트로 원자적 연산을 수행합니다. |
| 정적 웹(HTML) | `waiting.html`, `entry.html` 두 페이지로 순위 확인과 예약 절차를 시각화합니다. |


## 구현 내용 요약
### 1. 대기열 입장 및 순위 조회
- `QueueController`가 `/api/queue/enter` 요청을 받아 Kafka 토픽(`ticket-queue-topic`)으로 사용자 ID를 발행합니다.
- `KafkaConsumerService`가 메시지를 꺼내 `WaitingQueueService`에 위임하고, Redis **Sorted Set**(`waiting:queue`)에 타임스탬프 스코어로 적재합니다.
- `/api/queue/rank/{userId}`는 Sorted Set의 `rank` 연산으로 상시 순위를 반환합니다.

### 2. 활성 구간 전환과 토큰 발급
- `QueueScheduler`가 3초마다 실행되어 다음 절차를 수행합니다.
  1. `EntryQueueService`가 Lua 스크립트(`pop_random_active_users.lua`)로 활성 사용자 일부를 제거하여 슬롯을 확보합니다.
  2. 활성 인원이 `queue.settings.max-active-users`보다 부족하면 그 차이만큼 `QueueManagerService`가 `move_users_from_waiting_to_active.lua` 스크립트로 상위 N명을 **원자적으로** 이동시킵니다.
  3. 이동된 사용자마다 3분 TTL의 입장 토큰을 생성하여 Redis에 양방향(`entry:token*`, `user:token*`) 매핑으로 저장합니다.

### 3. 실시간 알림과 SSE
- `SseService`가 사용자별 `SseEmitter`를 관리하며 다음 이벤트를 발송합니다.
  - `RANK_UPDATE`: 전체 대기열을 한번에 조회해 순위를 브로드캐스트합니다.
  - `ACTIVE_TURN`: 입장 차례가 도달했음을 알리고 UI에 완료 메시지를 표시합니다.
  - `ENTRY_TOKEN`: 토큰 값과 만료 시각을 JSON 형태로 전달하여 클라이언트를 `entry.html`로 리다이렉트합니다.
- 정적 페이지는 EventSource로 이벤트를 수신하며 순위, 안내 문구, 토큰, 타이머를 즉시 갱신합니다.

### 4. 예약 확정
- `entry.html`에서 남은 시간을 계산하면서 `/api/queue/reserve/{userId}/{entryToken}`을 호출합니다.
- `QueueManagerService.isValidEntryToken`이 Redis 키 존재 여부를 확인하여 만료되었거나 조작된 토큰을 걸러냅니다.

## 문제 해결 
1. **유입 스파이크**: Kafka를 통해 HTTP 요청과 Redis 쓰기를 분리하여 갑작스러운 트래픽을 큐에서 처리합니다.
2. **순위 공정성**: Sorted Set을 사용해 밀리초 단위 선착순을 유지하고, Lua 스크립트로 이동 연산 전체를 원자적으로 처리합니다.
3. **동시 활성 제어**: `MAX_ACTIVE_USERS` 설정으로 동시에 예약 가능한 세션을 제한하고, 활성 풀에서 무작위로 사용자를 비우며 장기 점유를 방지합니다.
4. **유효 토큰 기반 입장**: 사용자 ↔ 토큰을 양방향으로 매핑하고 TTL을 부여하여 토큰 탈취나 재사용을 차단합니다.
5. **실시간 사용자 경험**: SSE를 통해 순위, 입장 알림, 토큰 정보를 즉시 푸시해 새로고침 없이 상태를 확인하도록 했습니다.
