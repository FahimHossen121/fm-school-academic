package com.example.fmschool.config;

import com.example.fmschool.entity.Account;
import com.example.fmschool.entity.Role;
import com.example.fmschool.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final Environment environment;

    public AdminBootstrap(AccountRepository accounts, PasswordEncoder encoder,
                          Environment environment) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        if (accounts.existsByRole(Role.ADMIN)) {
            return;
        }

        String password = environment.getProperty("FM_ADMIN_PASSWORD");
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = UUID.randomUUID().toString();
        }
        if (password.length() < 8 || password.length() > 64
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("FM_ADMIN_PASSWORD must be 8-64 characters and at most 72 UTF-8 bytes");
        }

        Account admin = new Account();
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        accounts.save(admin);

        log.info("Created the initial admin account. Username: admin");
        if (generated) {
            log.warn("INITIAL ADMIN PASSWORD (shown only on creation): {}", password);
        } else {
            log.info("Initial admin password taken from FM_ADMIN_PASSWORD.");
        }
    }
}
