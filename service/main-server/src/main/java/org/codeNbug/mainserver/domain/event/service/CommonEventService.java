package org.codeNbug.mainserver.domain.event.service;

import org.codeNbug.mainserver.domain.event.repository.CommonEventRepository;
import org.springframework.stereotype.Service;

@Service
public class CommonEventService {

	private final CommonEventRepository commonEventRepository;

	public CommonEventService(CommonEventRepository commonEventRepository) {
		this.commonEventRepository = commonEventRepository;
	}

}
