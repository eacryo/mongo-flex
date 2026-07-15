package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.annotation.MRepository;
import com.github.eacryo.mongoflex.repository.MongoRepository;

import com.github.eacryo.mongoflex.bean.ObjectIdEntity;

@MRepository
public interface ObjectIdEntityRepository extends MongoRepository<ObjectIdEntity, String> {
}
