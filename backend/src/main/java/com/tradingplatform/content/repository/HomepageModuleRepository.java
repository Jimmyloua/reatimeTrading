package com.tradingplatform.content.repository;

import com.tradingplatform.content.entity.HomepageModule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomepageModuleRepository extends JpaRepository<HomepageModule, Long> {

    @EntityGraph(attributePaths = "items")
    List<HomepageModule> findAllByActiveTrueOrderByDisplayOrderAsc();
}
