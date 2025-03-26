package clinic


import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [LocalizedStringValidator::class])
annotation class ValidLocalizedString(
    val message: String = "LOCALIZED_STRING_TOO_LONG",
    val max: Int = 255,
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class LocalizedStringValidator : ConstraintValidator<ValidLocalizedString, LocalizedString> {

    private var max: Int = 255

    override fun initialize(constraintAnnotation: ValidLocalizedString) {
        this.max = constraintAnnotation.max
    }

    override fun isValid(value: LocalizedString?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        return listOf(value.en, value.ru, value.uz)
            .all { it.length <= max }
    }
}


@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EnumValidator::class])
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val message: String = "Noto‘g‘ri enum qiymati!",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)


class EnumValidator : ConstraintValidator<ValidEnum, String> {
    private lateinit var enumValues: Array<out Enum<*>>

    override fun initialize(annotation: ValidEnum) {
        enumValues = annotation.enumClass.java.enumConstants
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) return false
        return enumValues.any { it.name == value }
    }
}






