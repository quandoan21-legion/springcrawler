package com.example.springcrawler.repository;

import com.example.springcrawler.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceRepository extends JpaRepository<Source, Long> {

    List<Source> findAllByDeletedFalseOrderByIdDesc();
}
