package clinic

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import org.hibernate.validator.constraints.Length
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.data.jpa.domain.AbstractPersistable_.id
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*


data class AuthenticationRequest(
    val username: String,
    val password:String
)

data class TokenResponse(
    val token:String
)
data class RefreshTokenRequest(
    val token:String
)
data class AuthenticationResponse(
    val accessToken:String,
    val refreshToken:String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int,
    val message: String,
    val fields: MutableList<ValidationFieldError>? = null
)

data class ValidationFieldError(val field: String, val message: String?)


data class PatientCreateRequest(
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    val fullName: String,
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    val username: String,
    @field:PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val dateOfBirth: Long,
    @field:PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val balance: BigDecimal,
    @field:ValidEnum(enumClass = Gender::class, message = "GENDER_ERROR")
    val gender: String,
) {
    fun toEntity(birthDate: LocalDate): Patient {
        return Patient(fullName, username, birthDate, balance, Gender.valueOf(gender))
    }
}

data class PatientResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val dateOfBirth: LocalDate,
    val balance: BigDecimal,
    val gender: Gender
) {
    companion object {
        fun toResponse(patient: Patient): PatientResponse {
            patient.run {
                return PatientResponse(id!!, fullName, username, dateOfBirth, balance, gender)
            }
        }
    }
}

data class PatientUpdateRequest(
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    val fullName: String?,
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    val username: String?,
)

data class UpdateBalanceRequest(
    @field:PositiveOrZero(message = "BALANCE_MUST_BE_POSITIVE")
    val balance: BigDecimal,
)


data class RequestParams(
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var page: Int = 0,

    @field:Min(1, message = "SIZE_ERROR_MIN")
    var size: Int = 20

)

data class EmployeeCreateRequest(
    val fullName: String,
    val username:String,
    val password:String,
    @field:ValidEnum(enumClass = Role::class, message = "ROLE_ERROR")
    val role: String
) {
    fun toEntity(bcryptPassword:String): Employee {
        return Employee(fullName, username,bcryptPassword, Role.valueOf(role))
    }
}

data class EmployeeUpdateRequest(
    val fullName: String?
)

data class EmployeeResponse(
    val id: Long,
    val fullName: String,
    val role: Role,
) {
    companion object {
        fun toResponse(employee: Employee): EmployeeResponse {
            employee.run { return EmployeeResponse(id!!, fullName, role) }
        }
    }
}

data class ServiceCreateRequest(
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    var name: String,
    var description: String,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var price: BigDecimal,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var duration: Long,
    var paymentType: String,
) {
    fun toEntity(day: Long): Service {
        return Service(name, description, price, day, paymentType)
    }
}

data class ServiceUpdateRequest(
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    var name: String?,
    var description: String?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var price: BigDecimal?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var duration: Long?,
    var paymentType: String?,
)

data class ServiceResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val duration: Long,
    val paymentType: String,
) {
    companion object {
        fun toResponse(service: Service): ServiceResponse {
            service.run {
                return ServiceResponse(id!!, name, description, price, duration, paymentType)
            }
        }
    }

}

data class DoctorScheduleCreateRequest(
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val doctorId: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val dayOfWeek: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val startTime: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val finishTime: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val launchStart: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val launchEnd: Long,
) {
    fun toEntity(doctor: Employee): DoctorSchedule {
        return DoctorSchedule(
            doctor,
            DateUtils.toYearMonthDate(dayOfWeek),
            DateUtils.toHour(startTime),
            DateUtils.toHour(finishTime),
            DateUtils.toHour(launchStart),
            DateUtils.toHour(launchEnd),
            DoctorStatus.NO_PATIENT
        )
    }
}

data class DoctorScheduleUpdateRequest(
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val dayOfWeek: Long?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val startTime: Long?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val finishTime: Long?,
)

data class DoctorScheduleResponse(
    val id: Long,
    val doctorName: String,
    val dayOfWeek: String,
    val startTime: LocalTime,
    val finishTime: LocalTime,
    val launchStart: LocalTime,
    val launchEnd: LocalTime,
    val status: DoctorStatus
) {
    companion object {

        fun toResponse(schedule: DoctorSchedule, week: String): DoctorScheduleResponse {

            schedule.run {
                return DoctorScheduleResponse(
                    id!!,
                    doctor.fullName,
                    week,
                    startTime,
                    finishTime,
                    launchStart,
                    launchEnd,
                    status
                )
            }

        }
    }
}

data class CardRequest(
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val patientId: Long,
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val serviceId: Long,
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val doctorId: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val fromDate: Long
)

data class CardUpdateRequest(
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val fromDate: Long?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val finishDate: Long?,
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val serviceId: Long?
)

data class CardResponse(
    val id: Long,
    val patient: PatientResponse,
    val totalAmount: BigDecimal,
    val services: List<CardServiceResponse>
)

data class CardServiceResponse(
    val serviceResponse: ServiceResponse,
    val doctorName: String,
    val fromDate: String,
    val toDate: String,
    val status: CardServiceStatus,
)

data class PaymentCreateRequest(
    val cardServiceId: Long,
    @field:ValidEnum(enumClass = PaymentMethod::class, message = "PAYMENT_METHOD_ERROR")
    val paymentMethod: String,
    val paidAmount: BigDecimal
)


data class PaymentResponseDetail(
    val patientName: String,
    val payments: List<PaymentResponse>,
    val totalPaidMoney: BigDecimal
)

data class PaymentResponse(
    val paymentId: Long,
    val serviceName: String,
    val paymentMoney: BigDecimal,
    val paymentStatus: PaymentStatus,
    val paymentDat: Date,


    ) {
    companion object {
        fun toEntity(payment: Payment): PaymentResponse {
            payment.run {
                return PaymentResponse(id!!, cardService.service.name, paidAmount, paymentStatus, createdDate!!)

            }
        }
    }
}

@ConfigurationProperties("jwt")
data class JwtProperties(
    val key:String,
    val accessTokenExpiration:Long,
    val refreshTokenExpiration:Long
)

data class JwtResponse(
    val username: String,
    val role:String
)

