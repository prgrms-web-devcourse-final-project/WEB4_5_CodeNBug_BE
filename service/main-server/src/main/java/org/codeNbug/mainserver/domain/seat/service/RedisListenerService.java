package org.codeNbug.mainserver.domain.seat.service;

import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${spring.redis.prefix:seat:lock:}")
	private String lockKeyPrefix;

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

		if (messageBody.startsWith(lockKeyPrefix)) {
			String[] parts = messageBody.split(":");
			Long eventId = Long.valueOf(parts[2]);
			Long seatId = Long.valueOf(parts[3]);

			eventRepository.findById(eventId)
				.orElseThrow(() -> new RuntimeException("존재하지 않는 행사입니다."));
			Seat seat = seatRepository.findById(seatId)
				.orElseThrow(() -> new IllegalArgumentException("좌석이 존재하지 않습니다."));
			seat.setAvailable(true);
			seatRepository.save(seat);

			System.out.println("TTL 만료로 좌석 " + seatId + "의 상태가 available = true 로 변경되었습니다.");
		}
	}
}