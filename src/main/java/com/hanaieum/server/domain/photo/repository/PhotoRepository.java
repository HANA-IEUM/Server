package com.hanaieum.server.domain.photo.repository;

import com.hanaieum.server.domain.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findAllByGroup_Id(Long groupId);
    List<Photo> findAllByUploader_Id(Long memberId);
}
