package com.example.english_app.repository.ipa;

import com.example.english_app.entity.ipa.IpaMinimalPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IpaMinimalPairRepository extends JpaRepository<IpaMinimalPair, Long> {

    @Query("SELECT m FROM IpaMinimalPair m JOIN FETCH m.phoneme1 JOIN FETCH m.phoneme2 WHERE m.isActive = true ORDER BY m.id ASC")
    List<IpaMinimalPair> findAllActiveWithPhonemes();
}
