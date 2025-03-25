package clinic

import ch.qos.logback.core.net.server.Client
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.HttpClientErrorException.Forbidden
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

    @ExceptionHandler(AccessDeniedException::class)
    fun handleBadCredentialsException(exception: AccessDeniedException ): ResponseEntity<BaseMessage> {
        val errorCode = ErrorCode.FORBIDDEN_EXCEPTION
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            Locale(LocaleContextHolder.getLocale().language)
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(BaseMessage(errorCode.code, message))
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleBadCredentialsException(exception: BadCredentialsException): ResponseEntity<BaseMessage> {
        val errorCode = ErrorCode.LOGIN_ERROR_EXCEPTION
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            Locale(LocaleContextHolder.getLocale().language)
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(BaseMessage(errorCode.code, message))
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

@Component
class CustomAccessDeniedHandler(
    private val errorMessageSource: ResourceBundleMessageSource
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        accessDeniedException: org.springframework.security.access.AccessDeniedException?
    ) {
        val errorCode = ErrorCode.FORBIDDEN_EXCEPTION
        val message = errorMessageSource.getMessage(
            errorCode.toString(),
            null,
            Locale(LocaleContextHolder.getLocale().language)
        )

        val responseBody = BaseMessage(errorCode.code, message)

        response?.status = HttpServletResponse.SC_FORBIDDEN
        response?.contentType = "application/json"
        response?.characterEncoding = "UTF-8"
        response?.writer?.write(ObjectMapper().writeValueAsString(responseBody))
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

class AuthenticationException : ClinicException(){
    override fun errorType() = ErrorCode.LOGIN_ERROR_EXCEPTION
}

class ForbiddenException : ClinicException(){
    override fun errorType() = ErrorCode.FORBIDDEN_EXCEPTION
}
class EmployeeRoleNotExistException : ClinicException(){
    override fun errorType () = ErrorCode.EMPLOYEE_HAS_NO_ROLE
}