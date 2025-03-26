package clinic

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.*


@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @CreatedBy var createdBy: Long? = null,
    @LastModifiedBy var lastModifiedBy: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    val employee: User,
    val refreshToken: String,
    val deleted: Boolean = false
)


@Entity
@Table(name = "users")
class User(
    var fullName: String,
    @Column(unique = true, nullable = false)
    @get:JvmName("getEmployeeUsername")
    var username: String,
    @get:JvmName("getEmployeePassword")
    var password: String,
    @Enumerated(EnumType.STRING)
    var role: Role = Role.PATIENT,
    @Enumerated(EnumType.STRING)
    var gender: Gender

) : BaseEntity(), UserDetails {
    override fun getAuthorities(): List<SimpleGrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_$role"))
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    fun getId(): Long {
        return id!!
    }

}

@Entity
class Item(
    @Column(unique = true, nullable = false)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var description: String,
    var price: BigDecimal,
    var duration: Long,
    var paymentType: String,
) : BaseEntity()


@Entity
class DoctorSchedule(
    @ManyToOne
    var doctor: User,
    var date: LocalDate,
    var startTime: LocalTime,
    var finishTime: LocalTime,
    var launchStart: LocalTime,
    var launchEnd: LocalTime,
    @Enumerated(EnumType.STRING)
    var status: DoctorStatus
) : BaseEntity()

@Entity
class Card(
    @OneToOne
    val patient: User,
    var totalAmount: BigDecimal,
    var status: CardStatus,
    val cardNumber:String
) : BaseEntity()

@Entity
class CardItem(
    @ManyToOne
    var card: Card,
    @ManyToOne
    var item: Item,
    @ManyToOne
    var doctor: User,
    var fromDate: LocalDate,
    var toDate: LocalDate? = null,
    var status: CardItemStatus,
) : BaseEntity()


@Entity
class Payment(
    @ManyToOne
    var cardItem: CardItem,
    @ManyToOne
    val patient: User,
    var paidAmount: BigDecimal,
    var paymentMethod: PaymentMethod,
    var paymentStatus: PaymentStatus
) : BaseEntity()


@Embeddable
class LocalizedString(
    var uz: String,
    var ru: String,
    var en: String,
) {
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

@Entity
@Table(name = "clinic")
class ClinicEntity(
    @Column(nullable = false)
    var name: String,
    var address: String,
    var phone: String,
    @Column(columnDefinition = "TEXT")
    var description: String,
    var openingHours: LocalTime,
    var closingHours: LocalTime,
    var createdYear:LocalDate
):BaseEntity()

