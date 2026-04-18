package com.uberall;

import com.uberall.exceptions.DistributedLockCreationException;
import com.uberall.models.Lock;
import com.uberall.repositories.DistributedLockRepository;
import io.micronaut.data.exceptions.DataAccessException;
import jakarta.inject.Inject;
import java.util.Optional;

public class MicronautDataJdbcLockService implements LockService {

    private final DistributedLockRepository distributedLockRepository;

    @Inject
    public MicronautDataJdbcLockService(DistributedLockRepository distributedLockRepository) {
        this.distributedLockRepository = distributedLockRepository;
    }

    @Override
    public Optional<Lock> get(String name) {
        Optional<DistributedLock> maybeDistributedLock = distributedLockRepository.findByName(name);
        if (!maybeDistributedLock.isPresent()) {
            return Optional.empty();
        }

        DistributedLock dl = maybeDistributedLock.get();
        return Optional.of(new Lock(dl.name, dl.until));
    }

    @Override
    public void save(Lock lock) {
        try {
            distributedLockRepository.save(new DistributedLock(lock.getName(), lock.getUntil()));
        } catch (DataAccessException e) {
            // Any DataAccessException during lock acquisition (constraint violation, deadlock, etc.)
            // means another instance is competing for the same lock — treat as lock-already-taken.
            throw new DistributedLockCreationException(lock.getName(), e);
        }
    }

    @Override
    public void delete(Lock lock) {
        distributedLockRepository.findByName(lock.getName())
                .ifPresent(distributedLockRepository::delete);
    }

    @Override
    public void clear() {
        distributedLockRepository.deleteAll();
    }
}
