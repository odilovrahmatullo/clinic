package clinic

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id:Long?=null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date?=null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date?=null,
    @CreatedBy var createdBy:Long?=null,
    @LastModifiedBy var lastModifiedBy: Long?=null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted:Boolean = false
)
@Entity
class Patient(
    var fullName :String,
    @Column(unique = true, nullable = false)
    var username :String,
    var dateOfBirth : LocalDate,
    var balance:BigDecimal,
    @Enumerated(EnumType.STRING)
    var gender:Gender,
):BaseEntity()

@Entity
class Employee(
    var fullName:String,
    @Enumerated(EnumType.STRING)
    var role:Role,
    ):BaseEntity()

@Entity
class Service(
    @Column(unique = true, nullable = false)
    var name:String,
    @Column(columnDefinition = "TEXT")
    var description:String,
    var price:BigDecimal,
    var duration : Long,
    var paymentType : String,
):BaseEntity()


@Entity
class DoctorSchedule(
   @ManyToOne
   var doctor:Employee,
   var dayOfWeek : LocalDate,
   var startTime : LocalTime,
   var finishTime:LocalTime,
   var launchStart : LocalTime,
   var launchEnd:LocalTime,
   @Enumerated(EnumType.STRING)
   var status : DoctorStatus
):BaseEntity()

@Entity
class Card(
   @OneToOne
   val patient:Patient,
   var totalAmount:BigDecimal,
   var status:CardStatus
):BaseEntity()

@Entity
class CardService(
    @ManyToOne
    var card:Card,
    @ManyToOne
    var service:Service,
    @ManyToOne
    var doctor:Employee,
    var fromDate:LocalDate,
    var toDate:LocalDate?=null,
    var status:CardServiceStatus,
):BaseEntity()


@Entity
class Payment(
    @ManyToOne
    val cardService : CardService,
    @ManyToOne
    val patient: Patient,
    var paidAmount : BigDecimal,
    var paymentMethod: PaymentMethod,
    var paymentStatus: PaymentStatus
):BaseEntity()




@Embeddable
class LocalizedString(
    var uz: String,
    var ru: String,
    var en: String,
    ){
    @Transient
    fun localized(): String {
        return when (LocaleContextHolder.getLocale().language) {
            "uz" -> this.uz
            "en" -> this.en
            "ru" -> this.ru
            else -> this.uz
        }
    }
}

