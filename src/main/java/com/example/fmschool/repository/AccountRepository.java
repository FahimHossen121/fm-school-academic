package com.example.fmschool.repository;

import com.example.fmschool.entity.Account;
import com.example.fmschool.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByRole(Role role);
    java.util.List<Account> findByRoleOrderByUsernameAsc(Role role);
}
