package org.codeNbug.mainserver.domain.seat.service;

import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisListenerService implements MessageListener {

	private final SeatRepository seatRepository;
	private final EventRepository eventRepository;

	@Value("${spring.redis.prefix:seat:lock:}")
	private String lockKeyPrefix;

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