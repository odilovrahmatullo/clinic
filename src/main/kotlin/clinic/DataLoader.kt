package clinic

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import jakarta.transaction.Transactional

@Component
class DataLoader(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String?) {
        if (!userRepository.existsByRoleAndDeletedFalse(Role.OWNER)) {
            val owner = User(
                fullName = "Ownerbek",
                username = "owner",
                password = passwordEncoder.encode("owner123"),
                role = Role.OWNER,
                Gender.MALE
            )
            userRepository.save(owner)
        }
    }
}
