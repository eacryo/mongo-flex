package com.github.eacryo.mongoflex.mql;

import com.github.eacryo.mongoflex.TestApplication;
import com.github.eacryo.mongoflex.bean.Character;
import com.github.eacryo.mongoflex.v2.CharacterRepository;
import com.github.eacryo.mongoflex.ulid.Ulid;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

@Slf4j
@SpringBootTest(classes = {TestApplication.class})
@ActiveProfiles("v2")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqlUpdateTest {

    @Autowired
    private CharacterRepository repo;

    @Test
    @Order(1)
    void testUpdateOne() {
        log.info("=== test @Update updateOne ===");
        String id = Ulid.generate();
        String name = "UpdateOneTest-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setLevel(1);
        c.setBirthday(new Date());
        log.info("before insert: level={}", c.getLevel());
        repo.insert(c);
        log.info("after insert: id={}, name={}", c.getId(), c.getName());

        long modified = repo.updateLevelByName(name, 99);
        log.info("updateLevelByName result: {}, expected: 1", modified);
        Assertions.assertEquals(1, modified);

        Character fetched = repo.findById(id);
        log.info("after update: id={}, level={}, expected level=99", fetched.getId(), fetched.getLevel());
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals(99, fetched.getLevel());

        log.info("cleanup: deleteById({})", id);
        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        repo.updateOneById(del);
    }

    @Test
    @Order(2)
    void testUpdateOneNoMatch() {
        log.info("=== test @Update updateOne no match ===");
        String noMatchName = "NonExistent-" + Ulid.generate();
        log.info("before update: name={} does not exist, expected modified=0", noMatchName);
        long modified = repo.updateLevelByName(noMatchName, 50);
        log.info("after update: modified={}, expected: 0", modified);
        Assertions.assertEquals(0, modified);
    }

    @Test
    @Order(3)
    void testUpsertInsert() {
        log.info("=== test @Update upsert (insert) ===");
        String name = "UpsertInsertTest-" + Ulid.generate();
        log.info("before upsert: name={} does not exist, upsert=true should insert", name);
        long modified = repo.upsertLevelByName(name, 80);
        log.info("after upsert: modified={}, expected: 0 (insert, not update)", modified);
        Assertions.assertEquals(0, modified);

        Character fetched = repo.findOneByName(name);
        log.info("after upsert: fetched={}, name={}, level={}, expected level=80", fetched.getId(), fetched.getName(), fetched.getLevel());
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals(80, fetched.getLevel());

        log.info("cleanup: deleteById({})", fetched.getId());
        Character del1 = new Character();
        del1.setId(fetched.getId());
        del1.setDeleted(true);
        repo.updateOneById(del1);
    }

    @Test
    @Order(4)
    void testUpsertUpdate() {
        log.info("=== test @Update upsert (update) ===");
        String id = Ulid.generate();
        String name = "UpsertUpdateTest-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setLevel(10);
        c.setBirthday(new Date());
        log.info("before insert: level={}", c.getLevel());
        repo.insert(c);
        log.info("after insert: id={}, name={}, level={}", c.getId(), c.getName(), c.getLevel());

        long modified = repo.upsertLevelByName(name, 99);
        log.info("after upsert: modified={}, expected: 1 (update existing)", modified);
        Assertions.assertEquals(1, modified);

        Character fetched = repo.findById(id);
        log.info("after upsert: fetched id={}, level={}, expected level=99", fetched.getId(), fetched.getLevel());
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals(99, fetched.getLevel());

        log.info("cleanup: deleteById({})", id);
        Character del2 = new Character();
        del2.setId(id);
        del2.setDeleted(true);
        repo.updateOneById(del2);
    }

    @Test
    @Order(5)
    void testMultiUpdate() {
        log.info("=== test @Update multi ===");
        String vision = "VisionMulti-" + Ulid.generate();
        String id1 = Ulid.generate();
        String id2 = Ulid.generate();
        String id3 = Ulid.generate();

        Character c1 = new Character();
        c1.setId(id1);
        c1.setName("MultiUpdate1-" + id1);
        c1.setVision(vision);
        c1.setBirthday(new Date());
        repo.insert(c1);
        log.info("inserted c1: id={}, vision={}, status={}", c1.getId(), c1.getVision(), c1.getStatus());

        Character c2 = new Character();
        c2.setId(id2);
        c2.setName("MultiUpdate2-" + id2);
        c2.setVision(vision);
        c2.setBirthday(new Date());
        repo.insert(c2);
        log.info("inserted c2: id={}, vision={}, status={}", c2.getId(), c2.getVision(), c2.getStatus());

        Character c3 = new Character();
        c3.setId(id3);
        c3.setName("MultiUpdate3-" + id3);
        c3.setVision(vision);
        c3.setBirthday(new Date());
        repo.insert(c3);
        log.info("inserted c3: id={}, vision={}, status={}", c3.getId(), c3.getVision(), c3.getStatus());

        log.info("before update: 3 docs with vision={}, status=null, multi=true should update all", vision);
        long modified = repo.updateStatusByVision(vision, "BANNED");
        log.info("after update: modified={}, expected: 3", modified);
        Assertions.assertEquals(3, modified);

        Character f1 = repo.findById(id1);
        log.info("fetched c1: id={}, status={}, expected status=BANNED", f1.getId(), f1.getStatus());
        Assertions.assertEquals("BANNED", f1.getStatus());
        Character f2 = repo.findById(id2);
        log.info("fetched c2: id={}, status={}, expected status=BANNED", f2.getId(), f2.getStatus());
        Assertions.assertEquals("BANNED", f2.getStatus());
        Character f3 = repo.findById(id3);
        log.info("fetched c3: id={}, status={}, expected status=BANNED", f3.getId(), f3.getStatus());
        Assertions.assertEquals("BANNED", f3.getStatus());

        log.info("cleanup: deleteById({}, {}, {})", id1, id2, id3);
        for (String cid : new String[] {id1, id2, id3}) {
            Character del = new Character();
            del.setId(cid);
            del.setDeleted(true);
            repo.updateOneById(del);
        }
    }

    @Test
    @Order(6)
    void testMultiUpdateNoMatch() {
        log.info("=== test @Update multi no match ===");
        String noMatchVision = "NonExistentVision-" + Ulid.generate();
        log.info("before update: vision={} does not exist, expected modified=0", noMatchVision);
        long modified = repo.updateStatusByVision(noMatchVision, "UNKNOWN");
        log.info("after update: modified={}, expected: 0", modified);
        Assertions.assertEquals(0, modified);
    }

    @Test
    @Order(7)
    void testVoidReturn() {
        log.info("=== test @Update void return ===");
        String id = Ulid.generate();
        String name = "VoidReturnTest-" + id;
        Character c = new Character();
        c.setId(id);
        c.setName(name);
        c.setBirthday(new Date());
        log.info("before insert: id={}, name={}, address=null", c.getId(), c.getName());
        repo.insert(c);
        log.info("after insert: id={}, name={}", c.getId(), c.getName());

        log.info("before update: void updateAddressByName, expected address='New Void Address'");
        repo.updateAddressByName(name, "New Void Address");
        log.info("after update: void return — no result to check");

        Character fetched = repo.findById(id);
        log.info("after update: fetched id={}, address={}, expected address='New Void Address'", fetched.getId(), fetched.getAddress());
        Assertions.assertNotNull(fetched);
        Assertions.assertEquals("New Void Address", fetched.getAddress());

        log.info("cleanup: deleteById({})", id);
        Character del = new Character();
        del.setId(id);
        del.setDeleted(true);
        repo.updateOneById(del);
    }
}
