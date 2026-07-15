package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.MRepository;
import com.github.eacryo.mongoflex.repository.MongoRepository;

import com.github.eacryo.mongoflex.bean.CustomIdEntity;

@MRepository
public interface CustomIdEntityRepository extends MongoRepository<CustomIdEntity, String> {
}
