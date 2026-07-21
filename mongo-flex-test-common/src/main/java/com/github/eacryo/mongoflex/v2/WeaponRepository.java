package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.Find;
import com.github.eacryo.mongoflex.annotation.MRepository;
import com.github.eacryo.mongoflex.annotation.Param;
import com.github.eacryo.mongoflex.bean.Weapon;
import com.github.eacryo.mongoflex.repository.MongoRepository;

import java.util.List;

@MRepository
public interface WeaponRepository extends MongoRepository<Weapon, String> {

    @Find("{character_id: #{characterId}}")
    List<Weapon> findByCharacterId(@Param("characterId") String characterId);
}
