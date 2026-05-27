package com.project95.thesis.thesis.repository;

import com.project95.thesis.thesis.domain.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
  Optional<Tag> findByName(String name);

  List<Tag> findAllByNameIn(Collection<String> names);
}
