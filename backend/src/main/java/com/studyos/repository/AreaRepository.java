package com.studyos.repository;

import com.studyos.entity.Area;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findAllByOrderByPositionAscNameAsc();

    List<Area> findByArchivedFalseOrderByPositionAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select coalesce(max(a.position), 0) from Area a")
    int findMaxPosition();
}