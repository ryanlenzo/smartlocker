package com.everest.locker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.everest.locker.entity.Locker;
import com.everest.locker.entity.LockerId;
import com.everest.locker.entity.Size;
import org.junit.jupiter.api.Test;

class InMemoryLockerRepositoryTest {

    private final InMemoryLockerRepository repository = new InMemoryLockerRepository();

    @Test
    void issuesSequentialIdsStartingAtOne() {
        assertThat(repository.nextId()).isEqualTo(new LockerId(1));
        assertThat(repository.nextId()).isEqualTo(new LockerId(2));
    }

    @Test
    void findAllReturnsLockersInNumericIdOrder() {
        for (int i = 0; i < 11; i++) {
            repository.save(new Locker(repository.nextId(), Size.SMALL));
        }

        assertThat(repository.findAll())
                .extracting(locker -> locker.id().toString())
                .startsWith("L1", "L2", "L3")
                .endsWith("L10", "L11");
    }

    @Test
    void isEmptyInitially() {
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findsALockerById() {
        Locker locker = new Locker(repository.nextId(), Size.SMALL);
        repository.save(locker);

        assertThat(repository.findById(new LockerId(1))).containsSame(locker);
        assertThat(repository.findById(new LockerId(2))).isEmpty();
    }
}
