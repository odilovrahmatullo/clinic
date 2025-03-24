package clinic

import ch.qos.logback.core.net.server.Client
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.*
import java.util.stream.Collectors


@ControllerAdvice
class ExceptionHandler(
    private val errorMessageSource: ResourceBundleMessageSource
) {

    @ExceptionHandler(ClinicException::class)
    fun handleShoppingException(exception: ClinicException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }

    @ExceptionHandler(BindException::class)
    fun validation(e: BindException): ResponseEntity<Any> {
        e.printStackTrace()
        val fields: MutableMap<String, Any?> = HashMap()
        for (fieldError in e.bindingResult.fieldErrors) {
            fields[fieldError.field] = fieldError.defaultMessage
        }

        val errorCode = ErrorCode.VALIDATION_ERROR
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            Locale(LocaleContextHolder.getLocale().language)
        )
        return ResponseEntity.badRequest().body(
            ValidationErrorMessage(
                errorCode.code,
                message,
                fields
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun on(e: MethodArgumentNotValidException): ResponseEntity<Any> {
        val fields = e.bindingResult.fieldErrors.stream()
            .map { error ->
                ValidationFieldError(
                    error.field,
                    getErrorMessage(error.defaultMessage ?: ErrorCode.VALIDATION_ERROR.name, null, errorMessageSource)
                )
            }.collect(Collectors.toList())

        val errorCode = ErrorCode.VALIDATION_ERROR
        val message = getErrorMessage(ErrorCode.VALIDATION_ERROR.name, null, errorMessageSource)
        return ResponseEntity.badRequest().body(
            BaseMessage(
                errorCode.code,
                message!!,
                fields
            )
        )
    }

    fun getErrorMessage(
        errorCode: String,
        errorMessageArguments: Array<Any?>?,
        errorMessageSource: ResourceBundleMessageSource
    ): String? {
        val errorMessage = try {
            errorMessageSource.getMessage(errorCode, errorMessageArguments, LocaleContextHolder.getLocale())
        } catch (e: Exception) {
            e.message
        }
        return errorMessage
    }
}

sealed class ClinicException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorCode
    protected open fun getErrorMessageArguments(): Array<Any?>? = null
    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource): BaseMessage {
        return BaseMessage(
            errorType().code,
            errorMessageSource.getMessage(
                errorType().toString(),
                getErrorMessageArguments(),
                Locale(LocaleContextHolder.getLocale().language)
            )
        )
    }
}

data class ValidationErrorMessage(val code: Int, val message: String, val fields: Map<String, Any?>)

class PatientAlreadyExistException : ClinicException() {
    override fun errorType() = ErrorCode.PATIENT_ALREADY_EXIST
}

class PatientNotFoundException : ClinicException() {
    override fun errorType() = ErrorCode.PATIENT_NOT_FOUND
}

class EmployeeNotFoundException : ClinicException() {
    override fun errorType() = ErrorCode.EMPLOYEE_NOT_FOUND
}

class ServiceNotFoundException : ClinicException() {
    override fun errorType() = ErrorCode.SERVICE_NOT_FOUND
}

class ServiceAlreadyExistException : ClinicException() {
    override fun errorType() = ErrorCode.SERVICE_ALREADY_EXIST
}

class NoHavePermission : ClinicException() {
    override fun errorType() = ErrorCode.NO_HAVE_PERMISSION
}

class DoctorScheduleNotFound : ClinicException() {
    override fun errorType() = ErrorCode.DOCTOR_SCHEDULE_NOT_FOUND
}

class DoctorScheduleNotAvailable : ClinicException() {
    override fun errorType() = ErrorCode.DOCTOR_SCHEDULE_NOT_AVAILABLE
}

class CardNotFound : ClinicException() {
    override fun errorType() = ErrorCode.CARD_NOT_FOUND
}

class CardServiceNotFoundException() : ClinicException() {
    override fun errorType() = ErrorCode.CARD_SERVICE_NOT_FOUND
}

class BeforeTimeException : ClinicException() {
    override fun errorType() = ErrorCode.BEFORE_TIME_ERROR

}

class BalanceNotEnoughException : ClinicException(){
    override fun errorType() = ErrorCode.BALANCE_NOT_ENOUGH
}