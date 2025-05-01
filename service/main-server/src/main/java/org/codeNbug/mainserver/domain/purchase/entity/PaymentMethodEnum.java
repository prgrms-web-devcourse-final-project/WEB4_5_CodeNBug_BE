package org.codeNbug.mainserver.domain.purchase.entity;

public enum PaymentMethodEnum {
	카드("CARD"),
	가상계좌("VIRTUAL_ACCOUNT"),
	간편결제("EASY_PAY"),
	휴대폰("MOBILE_PHONE"),
	계좌이체("TRANSFER"),
	문화상품권("CULTURE_GIFT_CERTIFICATE"),
	도서문화상품권("BOOK_GIFT_CERTIFICATE"),
	게임문화상품권("GAME_GIFT_CERTIFICATE");

	private final String code;

	PaymentMethodEnum(String code) {
		this.code = code;
	}

	public static PaymentMethodEnum from(String code) {
		for (PaymentMethodEnum method : values()) {
			if (method.name().equalsIgnoreCase(code) || method.code.equalsIgnoreCase(code)) {
				return method;
			}
		}
		throw new IllegalArgumentException("지원하지 않는 결제 수단: " + code);
	}
}
