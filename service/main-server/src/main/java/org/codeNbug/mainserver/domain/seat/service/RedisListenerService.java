package org.codeNbug.mainserver.domain.seat.service;

import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Redis TTL 만료 이벤트 감지 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class RedisListenerService implements MessageListener {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;
	private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";

	/**
	 * Redis에서 TTL 만료된 키 이벤트를 수신하여,
	 * 좌석을 available 상태로 복구하는 메서드
	 *
	 * @param message 만료된 Redis 키에 대한 메시지
	 * @param pattern 패턴 (사용하지 않음)
	 */
	@Override
	public void onMessage(Message message, byte[] pattern) {
		String messageBody = new String(message.getBody());

		if (messageBody.startsWith(SEAT_LOCK_KEY_PREFIX)) {
			String[] parts = messageBody.split(":");

			if (parts.length < 5) {
				System.err.println("[onMessage] TTL 키 형식이 올바르지 않습니다: " + messageBody);
				return;
			}
			try {
				Long eventId = Long.valueOf(parts[3]);
				Long seatId = Long.valueOf(parts[4]);

				eventRepository.findById(eventId)
					.orElseThrow(() -> new RuntimeException("[onMessage] 존재하지 않는 행사입니다."));
				Seat seat = seatRepository.findById(seatId)
					.orElseThrow(() -> new IllegalArgumentException("[onMessage] 좌석이 존재하지 않습니다."));
				seat.cancelReserve();
				seatRepository.save(seat);

				System.out.println("[onMessage] TTL 만료로 좌석 " + seatId + "의 상태가 available = true 로 변경되었습니다.");
			} catch (NumberFormatException e) {
				System.out.println("[onMessage] TTL 키 파싱 실패: " + messageBody);
			}
		}
	}
}