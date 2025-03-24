package clinic

import java.time.*
import java.util.*
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