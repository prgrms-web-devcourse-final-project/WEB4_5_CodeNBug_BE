package org.codeNbug.mainserver.domain.manager.repository;

import java.util.List;

import org.codeNbug.mainserver.domain.event.entity.Event;
import org.codeNbug.mainserver.domain.manager.entity.ManagerEvent;
import org.codenbug.user.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManagerEventRepository extends JpaRepository<ManagerEvent, Long> {
    @Query("SELECT me.event FROM ManagerEvent me WHERE me.manager = :manager")
    List<Event> findEventsByManager(@Param("manager") User manager);

    @Query("SELECT me.manager FROM ManagerEvent me WHERE me.event = :event")
    List<User> findManagersByEvent(@Param("event") Event event);

    /**
     * manager, event 로 해당 manager 가 event 를 관리하는지 알 수 있는 메서드
     * @param manager
     * @param event
     * @return
     */
    boolean existsByManagerAndEvent(User manager, Event event);
}
