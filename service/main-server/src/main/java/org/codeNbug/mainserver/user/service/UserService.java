package org.codeNbug.mainserver.user.service;

import org.codeNbug.mainserver.user.entity.User;

public interface UserService {
	User getLoggedInUser();
}
