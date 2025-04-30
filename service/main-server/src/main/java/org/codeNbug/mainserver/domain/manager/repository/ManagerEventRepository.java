package org.codeNbug.mainserver.domain.manager.repository;

import org.codeNbug.mainserver.domain.manager.entity.Event;
import org.codeNbug.mainserver.domain.manager.entity.ManagerEvent;
import org.codeNbug.mainserver.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManagerEventRepository extends JpaRepository<ManagerEvent, Long> {
    @Query("SELECT me.event FROM ManagerEvent me WHERE me.manager = :manager")
    List<Event> findEventsByManager(@Param("manager") User manager);

    @Query("SELECT me.manager FROM ManagerEvent me WHERE me.event = :event")
    List<User> findManagersByEvent(@Param("event") Event event);
}
