package com.booking.professionalservice.repository;

import com.booking.professionalservice.model.entity.CleanerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleanerRepository extends JpaRepository<CleanerEntity, String> {

  Page<CleanerEntity> findByVehicle_Id(String vehicleId, Pageable pageable);

  List<CleanerEntity> findByVehicle_IdIn(List<String> vehicleIds);

}
