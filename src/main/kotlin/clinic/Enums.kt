package clinic


enum class Gender {
    MALE,
    FEMALE
}

enum class Role{
    DOCTOR,
    DIRECTOR,
    CASHIER,
}
enum class DoctorStatus{
    HAS_PATIENT,
    NO_PATIENT
}
enum class CardStatus{
    ACTIVE,NOT_ACTIVE
}
enum class CardServiceStatus{
    IN_PROCESS,DONE
}

enum class PaymentMethod{
    PAYME,CLICK,PAYNET
}
enum class PaymentStatus{
    PAID,NOT_PAID
}
enum class ErrorCode(val code: Int) {

    PATIENT_NOT_FOUND(100),
    PATIENT_ALREADY_EXIST(101),
    EMPLOYEE_NOT_FOUND(200),
    SERVICE_NOT_FOUND(300),
    SERVICE_ALREADY_EXIST(301),
    DOCTOR_SCHEDULE_NOT_FOUND(400),
    DOCTOR_SCHEDULE_NOT_AVAILABLE(401),

    NO_HAVE_PERMISSION(500),

    CARD_NOT_FOUND(550),
    CARD_SERVICE_NOT_FOUND(551),

    VALIDATION_ERROR(600),
    BEFORE_TIME_ERROR(650),

    BALANCE_NOT_ENOUGH(700),
}