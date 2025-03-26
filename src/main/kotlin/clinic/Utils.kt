package clinic

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.*
import java.util.concurrent.TimeUnit

class DateUtils {
    companion object {
        fun toYearMonth(timestamp: Long): YearMonth {
            val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
            return YearMonth.of(localDateTime.year, localDateTime.monthValue)
        }

        fun toYearMonthDate(timestamp: Long): LocalDate {
            return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        fun toDay(timestamp: Long): Long {
            return TimeUnit.MILLISECONDS.toDays(timestamp)
        }

        fun toHour(timestamp: Long): LocalTime {
            return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalTime()
        }
    }
}

@Component
class SpringSecurityUtil {
        private fun getAuthentication(): Authentication {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication == null || !authentication.isAuthenticated || authentication.principal == "anonymousUser") {
                throw ForbiddenException()
            }
            return authentication
        }

        fun getCurrentUser(): UserDetails {
            return getAuthentication().principal as UserDetails
        }

        fun getCurrentUserId(): Long {
            return (getAuthentication().principal as User).getId()
        }
}
