package clinic

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import org.hibernate.validator.constraints.Length
import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*


data class AuthenticationRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    val token: String
)

data class RefreshTokenRequest(
    val token: String
)

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(
    val code: Int,
    val message: String,
    val fields: MutableList<ValidationFieldError>? = null
)

data class ValidationFieldError(val field: String, val message: String?)


interface CreateUserRequest{
    val fullName:String
    val username:String
    val password:String
    val gender: String
    fun toEntity(encodedPassword:String):User
}
data class UserCreateRequest(
    @field:ValidEnum(enumClass = Role::class, message = "ROLE_ERROR")
    val role: String,
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    override val fullName: String,
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    override val username: String,
    override val password: String,
    @field:ValidEnum(enumClass = Gender::class, message = "GENDER_ERROR")
    override val gender: String,
):CreateUserRequest {
    override fun toEntity(encodedPassword: String): User {
        val expectedRole = Role.valueOf(role)
        if(expectedRole==Role.OWNER || expectedRole == Role.DIRECTOR) throw NoHavePermission()

            return User(fullName, username,encodedPassword,expectedRole,Gender.valueOf(gender))
        }
    }

data class PatientCreateRequest(
    @field:NotBlank(message = "THIS_FIELD_CANNOT_BE_BLANK")
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    override val fullName: String,
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    override val username: String,
    override val password: String,
    @field:ValidEnum(enumClass = Gender::class, message = "GENDER_ERROR")
    override val gender: String,
):CreateUserRequest {
    override fun toEntity(encodedPassword: String): User {
        return User(fullName, username,encodedPassword,Role.PATIENT,Gender.valueOf(gender))
    }
}


data class UserResponse(
    val id: Long,
    val fullName: String,
    val username: String,
    val role:Role,
    val gender: Gender
) {
    companion object {
        fun toResponse(patient: User): UserResponse {
            patient.run {
                return UserResponse(id!!, fullName, username,role,gender)
            }
        }
    }
}

data class UserUpdateRequest(
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
    var size: Int = 20,

    var search: String = ""
)


data class EmployeeUpdateRequest(
    val fullName: String?
)



data class ItemCreateRequest(
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    var name: String,
    var description: String,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var price: BigDecimal,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var duration: Long,
    var paymentType: String,
) {
    fun toEntity(day: Long): Item {
        return Item(name, description, price, day, paymentType)
    }
}

data class ItemUpdateRequest(
    @field:Length(max = 50, message = "THIS_FIELD_LENGTH_ERROR")
    var name: String?,
    var description: String?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var price: BigDecimal?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    var duration: Long?,
    var paymentType: String?,
)

data class ItemResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: BigDecimal,
    val duration: Long,
    val paymentType: String,
) {
    companion object {
        fun toResponse(service: Item): ItemResponse {
            service.run {
                return ItemResponse(id!!, name, description, price, duration, paymentType)
            }
        }
    }

}

data class DoctorScheduleCreateRequest(
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
    fun toEntity(doctor: User,date: LocalDate): DoctorSchedule {
        return DoctorSchedule(
            doctor,
            date,
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


data class CardItemRequest(
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val serviceId: Long,
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val doctorId: Long,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val fromDate: Long
)

data class CardItemUpdateRequest(
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val fromDate: Long?,
    @field: PositiveOrZero(message = "THIS_FIELD_MUST_BE_POSITIVE")
    val finishDate: Long?,
    @field:Min(1, message = "SIZE_ERROR_MIN")
    val serviceId: Long?
)

data class CardResponse(
    val id: Long,
    val patient: UserResponse,
    val cardNumber:String,
    val balance:BigDecimal,
    val status : CardStatus,
    val services: List<CardItemResponse>,
    val totalPaidAmount: BigDecimal
)

data class CardItemResponse(
    val serviceResponse: ItemResponse,
    val doctorName: String,
    val fromDate: String,
    val toDate: String,
    val status: CardItemStatus,
    val paidMoney:BigDecimal
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
                return PaymentResponse(id!!, cardItem.item.name, paidAmount, paymentStatus, createdDate!!)

            }
        }
    }
}

@ConfigurationProperties("jwt")
data class JwtProperties(
    val key: String,
    val accessTokenExpiration: Long,
    val refreshTokenExpiration: Long
)

data class ClinicCreateRequest(
    val name: String,
    val address: String,
    @field:Pattern(
        regexp = "^\\+998[0-9]{9}$",
        message = "PHONE_NUMBER_ERROR"
    )
    val phone: String,
    val description: String,
    @field:Min(4, message = "SIZE_ERROR_MIN")
    val openingHours: Long,
    @field:Min(4, message = "SIZE_ERROR_MIN")
    val closingHours: Long,
    @field:Min(6, message = "SIZE_ERROR_MIN")
    val createdYear:Long
){
    fun toEntity() : ClinicEntity{
        val openHours = DateUtils.toHour(openingHours)
        val closeHour = DateUtils.toHour(closingHours)
        val creatYear = DateUtils.toYearMonthDate(createdYear)
        if(closeHour.isAfter(openHours)) throw BeforeTimeException()

        if(creatYear.isBefore(LocalDate.now())) throw BeforeTimeException()

        return ClinicEntity(name,address,phone,description,openHours,closeHour,creatYear)
    }

}

data class ClinicUpdateRequest(
    val name: String?,
    val address: String?,
    @field:Pattern(
        regexp = "^\\+998[0-9]{9}$",
        message = "PHONE_NUMBER_ERROR"
    )
    val phone: String?,
    val description: String?,
)

data class ClinicResponse(
    val id:Long,
    val name: String,
    val address: String,
    val phone: String,
    val description: String,
    val openingHours: LocalTime,
    val closingHours: LocalTime,
    val createdYear:LocalDate
){
    companion object {
        fun toResponse(clinic: ClinicEntity):ClinicResponse{
            clinic.run {
                return ClinicResponse(id!!,name,address,phone,description,openingHours,closingHours,createdYear)
            }
        }
    }
}

/*data class CardResponse(
    val id:Long,
    val patientName:String,
    val balance:BigDecimal,
    val cardNumber:String,
    val status:CardStatus
)*/

