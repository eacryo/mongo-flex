package com.github.eacryo.mongoflex.v2;

import com.github.eacryo.mongoflex.bean.ObjectIdEntity;

@MRepository
public interface ObjectIdEntityRepository extends MongoRepository<ObjectIdEntity, String> {
}
