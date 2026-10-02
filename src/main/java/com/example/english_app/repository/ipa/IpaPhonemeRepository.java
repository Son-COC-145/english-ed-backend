package com.example.english_app.repository.ipa;

import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.ipa.IpaPhoneme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IpaPhonemeRepository extends JpaRepository<IpaPhoneme, Short> {

    List<IpaPhoneme> findAllByOrderByIdAsc();
    
    @Query("""
           SELECT p FROM IpaPhoneme p 
           WHERE (:type IS NULL OR p.phonemeType = :type) 
           AND (:isCommonError IS NULL OR p.isCommonVnError = :isCommonError)
           """)
    List<IpaPhoneme> findByFilters(@Param("type") PhonemeType type, @Param("isCommonError") Boolean isCommonError);
    
    @Query("SELECT p FROM IpaPhoneme p LEFT JOIN FETCH p.exampleWords WHERE p.id = :id")
    Optional<IpaPhoneme> findByIdWithWords(@Param("id") Short id);
}
