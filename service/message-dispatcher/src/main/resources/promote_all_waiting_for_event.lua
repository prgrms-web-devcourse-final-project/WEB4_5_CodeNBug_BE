-- ==================================================================================
-- Lua 스크립트: promote_all_waiting_for_event.lua
-- KEYS:
--   KEYS[1] = ENTRY_QUEUE_COUNT_HASH_KEY              (예: "ENTRY_QUEUE_COUNT")
--   KEYS[2] = "WAITING_QUEUE_RECORD:" .. eventId      (예: "WAITING_QUEUE_RECORD:42")
--   KEYS[3] = waitingZsetKey                          (예: "waiting:42")
--   KEYS[4] = "WAITING_QUEUE_IN_USER:" .. eventId      (예: "WAITING_QUEUE_IN_USER_RECORD:42")
--   KEYS[5] = ENTRY_QUEUE_STREAM_KEY                   (예: "ENTRY_QUEUE")
-- ARGV:
--   ARGV[1] = eventId
--
-- 이 스크립트가 에러 없이 끝까지 수행되면, 1을 리턴하고
-- 만약 자리가 부족하거나 JSON 파싱 중 예외가 발생할 경우, 전체 롤백 후 0을 리턴
-- ==================================================================================

local eventId     = ARGV[1]

-- 1) 현재 남은 자리 읽기
local rawCount = redis.call("HGET", KEYS[1], eventId)
if (not rawCount) or (tonumber(rawCount) < 1) then
    return 0
end

-- 2) waiting ZSet에서 모든 대기 아이템(itemJson) 가져오기
--    각 itemJson 형태: "{\"userId\":123}"
local waitingItems = redis.call("ZRANGE", KEYS[3], 0, -1)
-- 만약 ZSet이 비어 있으면, 실행할 필요 없이 그냥 1 리턴
if (#waitingItems == 0) then
    return 1
end

-- 3) 각 waitingItem마다 순차 처리
for idx = 1, #waitingItems do
    local itemJson = waitingItems[idx]
    -- JSON 파싱 (cjson 모듈 사용)
    local ok, itemObj = pcall(cjson.decode, itemJson)
    if not ok then
        -- JSON 형식 에러: 즉시 롤백
        error("JSON 파싱 실패: " .. itemJson)
    end

    local userId = tostring(itemObj["userId"])
    if (not userId) then
        error("userId 없음: " .. itemJson)
    end

    -- 3-1) 다시 count를 체크해서, 중간에 부족해진 경우 전체 롤백
    rawCount = redis.call("HGET", KEYS[1], eventId)
    if (not rawCount) or (tonumber(rawCount) < 1) then
        -- 남은 자리가 0 이하이면 전체 롤백
        return 0
    end

    -- 3-2) entry queue count를 -1 감소
    redis.call("HINCRBY", KEYS[1], eventId, -1)

    -- 3-3) waiting record 해시에서 해당 record JSON 가져오기
    local waitingHashKey = KEYS[2]         -- "WAITING_QUEUE_RECORD:{eventId}"
    local recordJson = redis.call("HGET", waitingHashKey, userId)
    if not recordJson then
        -- 대기 레코드가 없으면, 롤백
        error("Waiting record가 없음 for userId=" .. userId)
    end

    -- 3-4) recordJson도 JSON 파싱 ({"userId":.., "eventId":.., "instanceId":..} 형태라고 가정)
    local ok, recordObj = pcall(cjson.decode, recordJson)
    if not ok then
        error("Record JSON 파싱 실패: " .. recordJson)
    end

    local instanceId = tostring(recordObj["instanceId"])
    if (not instanceId) then
        error("instanceId 없음 in record: " .. recordJson)
    end

    -- 3-5) ENTRY_QUEUE 스트림에 XADD (with ID="*")
    local entryMsg = { "userId", userId, "eventId", eventId, "instanceId", instanceId }
    redis.call("XADD", KEYS[5], "*", unpack(entryMsg))

    -- 3-6) waiting ZSet(KEYS[3])에서 해당 itemJson 제거
    redis.call("ZREM", KEYS[3], itemJson)

    -- 3-7) WAITING_QUEUE_RECORD 해시(KEYS[2])에서 userId 필드 삭제
    redis.call("HDEL", KEYS[2], userId)

    -- 3-8) WAITING_QUEUE_IN_USER_RECORD 해시(KEYS[4])에서 userId 삭제
    redis.call("HDEL", KEYS[4], userId)
end

-- 모든 사용자 프로모션 성공
return 1
