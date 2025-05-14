package org.codeNbug.mainserver.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.event.entity.EventCategoryEnum;
import org.codeNbug.mainserver.domain.event.entity.EventInformation;
import org.codeNbug.mainserver.domain.event.entity.EventStatusEnum;
import org.codeNbug.mainserver.domain.manager.repository.EventRepository;
import org.codeNbug.mainserver.domain.seat.entity.Seat;
import org.codeNbug.mainserver.domain.seat.entity.SeatGrade;
import org.codeNbug.mainserver.domain.seat.entity.SeatGradeEnum;
import org.codeNbug.mainserver.domain.seat.entity.SeatLayout;
import org.codeNbug.mainserver.domain.seat.repository.SeatGradeRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatLayoutRepository;
import org.codeNbug.mainserver.domain.seat.repository.SeatRepository;
import org.codenbug.user.domain.user.entity.User;
import org.codenbug.user.domain.user.repository.UserRepository;
import org.codenbug.user.redis.service.TokenService;
import org.codenbug.user.security.service.CustomUserDetailsService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@SpringBootTest
@Transactional
@Component
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BaseTestUtil {
	@Autowired
	protected UserRepository userRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;
	@Autowired
	protected TokenService tokenService;
	@Autowired
	protected CustomUserDetailsService userDetailsService;
	@Autowired
	protected EventRepository eventRepository;
	@Autowired
	protected SeatLayoutRepository seatLayoutRepository;
	@Autowired
	protected SeatGradeRepository seatGradeRepository;
	@Autowired
	protected SeatRepository seatRepository;
	@Autowired
	protected StringRedisTemplate stringRedisTemplate;

	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";
	public static String testToken;
	public static User testUser;
	public static Event testEvent;

	public User setUpUser() {
		// 테스트용 사용자 생성
		testUser = User.builder()
			.email("test" + UUID.randomUUID() + "@example.com")
			.password(passwordEncoder.encode("Test1234!"))
			.name("테스트")
			.age(25)
			.sex("남성")
			.phoneNum("010-1234-5678")
			.location("서울시 강남구")
			.role("ROLE_USER")
			.build();
		userRepository.save(testUser);

		return testUser;
	}

	public String setUpToken() {
		// 테스트용 토큰 생성
		TokenService.TokenInfo tokenInfo = tokenService.generateTokens(testUser.getEmail());
		UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getEmail());
		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		testToken = tokenInfo.getAccessToken();

		return testToken;
	}

	public Event setUpEvent() throws JSONException {
		// 테스트용 행사 생성
		EventInformation info = EventInformation.builder()
			.title("테스트 공연")
			.thumbnailUrl("https://example.com/thumbnail.jpg")
			.description("이것은 테스트 공연입니다.")
			.ageLimit(15)
			.restrictions("음식물 반입 금지")
			.location("서울 강남구 공연장")
			.hallName("테스트홀")
			.eventStart(LocalDateTime.now().plusDays(1))
			.eventEnd(LocalDateTime.now().plusDays(2))
			.seatCount(200)
			.build();

		testEvent = new Event(
			EventCategoryEnum.CONCERT,
			info,
			LocalDateTime.now().plusDays(1),
			LocalDateTime.now().plusDays(2),
			0,
			LocalDateTime.now(),
			LocalDateTime.now(),
			EventStatusEnum.OPEN,
			true,
			false,
			null
		);
		eventRepository.save(testEvent);

		// 테스트용 좌석 레이아웃 생성
		String layoutJson = """
			{
			  "layout": [
			    ["A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10"],
			    ["B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "B10"]
			  ],
			  "seat": {
			    "A1": { "grade": "VIP" },
			    "A2": { "grade": "VIP" },
			    "A3": { "grade": "VIP" },
			    "A4": { "grade": "VIP" },
			    "A5": { "grade": "VIP" },
			    "A6": { "grade": "VIP" },
			    "A7": { "grade": "VIP" },
			    "A8": { "grade": "VIP" },
			    "A9": { "grade": "VIP" },
			    "A10": { "grade": "VIP" },
			    "B1": { "grade": "R" },
			    "B2": { "grade": "R" },
			    "B3": { "grade": "R" },
			    "B4": { "grade": "R" },
			    "B5": { "grade": "R" },
			    "B6": { "grade": "R" },
			    "B7": { "grade": "R" },
			    "B8": { "grade": "R" },
			    "B9": { "grade": "R" },
			    "B10": { "grade": "R" }
			  }
			}
			""";

		SeatLayout seatLayout = SeatLayout.builder()
			.layout(layoutJson)
			.event(testEvent)
			.build();
		seatLayoutRepository.save(seatLayout);

		testEvent.setSeatLayout(seatLayout);
		eventRepository.save(testEvent);

		// 좌석 등급 및 좌석 생성
		createSeatGradesAndSeats(layoutJson);

		return testEvent;
	}

	private void createSeatGradesAndSeats(String layoutJson) throws JSONException {
		JSONObject fullJson = new JSONObject(layoutJson);
		JSONArray rows = fullJson.getJSONArray("layout");
		JSONObject seatDetails = fullJson.getJSONObject("seat");

		Map<SeatGradeEnum, SeatGrade> gradeMap = new HashMap<>();
		for (SeatGradeEnum gradeEnum : SeatGradeEnum.values()) {
			SeatGrade seatGrade = SeatGrade.builder()
				.grade(gradeEnum)
				.amount(getSeatGradeAmount(gradeEnum))
				.event(testEvent)
				.build();
			seatGradeRepository.save(seatGrade);
			gradeMap.put(gradeEnum, seatGrade);
		}

		// 좌석 생성
		for (int i = 0; i < rows.length(); i++) {
			JSONArray row = rows.optJSONArray(i);
			if (row == null)
				continue;

			for (int j = 0; j < row.length(); j++) {
				String seatName = row.getString(j);
				String grade = seatDetails.getJSONObject(seatName).getString("grade");

				SeatGradeEnum gradeEnum = SeatGradeEnum.fromString(grade);
				SeatGrade seatGrade = gradeMap.get(gradeEnum);

				Seat testSeat = Seat.builder()
					.location(seatName)
					.grade(seatGrade)
					.layout(testEvent.getSeatLayout())
					.event(testEvent)
					.available(true)
					.build();

				seatRepository.save(testSeat);
			}
		}
	}

	private int getSeatGradeAmount(SeatGradeEnum gradeEnum) {
		return switch (gradeEnum) {
			case VIP -> 100000;
			case R -> 80000;
			case S -> 60000;
			case A -> 50000;
			default -> 40000;
		};
	}

	public void setUpRedis() {
		// 테스트용 entry token 저장
		stringRedisTemplate.opsForHash().put(
			ENTRY_TOKEN_STORAGE_KEY_NAME,
			String.valueOf(testUser.getUserId()),
			testToken
		);
	}
}
