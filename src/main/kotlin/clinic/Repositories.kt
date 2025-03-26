package clinic

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)

    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }

    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }

}

@Repository
interface UserRepository : BaseRepository<User> {
    fun existsByUsername(fullName: String): Boolean

    fun existsByUsernameAndIdNot(fullName: String, id: Long): Boolean

    @Query("FROM User p WHERE (LOWER(p.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND (:role IS NULL OR p.role = :role)")
    fun getUsers(
        pageable: Pageable,
        @Param("search") search: String?,
        @Param("role") role: Role?
    ): Page<User>

    fun findByUsernameAndDeletedFalse(username: String): User?
    fun existsByRoleAndDeletedFalse(owner: Role): Boolean
}


@Repository
interface ItemRepository : BaseRepository<Item> {
    fun existsByNameAndDeletedFalse(name: String): Boolean

    fun existsByNameAndIdNotAndDeletedFalse(name: String, id: Long): Boolean

    @Query("SELECT s FROM Item as s where (lower(s.name) Like Lower(concat('%', :search,'%') )) and s.deleted = false ")
    fun findAllAsSearch(pageable: Pageable, @Param("search") search: String): Page<Item>
}

interface DoctorScheduleRepository : BaseRepository<DoctorSchedule> {
    @Query(
        """SELECT ds FROM DoctorSchedule ds 
        where ds.doctor.id = :id 
        and ds.status = :noPatient 
        and ds.date = :fromDate
        and  ds.deleted = false
        """
    )
    fun getEmptyDoctor(
        @Param("id") id: Long,
        @Param("fromDate") fromDate: LocalDate,
        @Param("noPatient") noPatient: DoctorStatus
    ): DoctorSchedule?

    fun existsByDate(date: LocalDate): Boolean
}

interface CardRepository : BaseRepository<Card> {
    fun findByPatientAndDeletedFalse(patient: User): Card?
    fun existsByCardNumber(cardNumber: String): Boolean
    fun existsByPatientAndDeletedFalse(patient: User): Boolean
}

interface CardItemRepository : BaseRepository<CardItem> {
    fun findByCardAndDeletedFalse(card: Card?): List<CardItem>
}

interface PaymentRepository : BaseRepository<Payment> {
    @Query("SELECT p FROM Payment p WHERE p.patient = :patient AND p.deleted = false")
    fun findAllByUserAndDeletedFalse(patient: User): List<Payment>

    fun findByCardItemAndDeletedFalse(cardItem: CardItem): Payment?
}

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findRefreshTokenByRefreshTokenAndDeletedFalse(token: String): RefreshToken
}

interface ClinicRepository:BaseRepository<ClinicEntity> {

    @Query("SELECT s FROM ClinicEntity as s where (lower(s.name) Like Lower(concat('%', :search,'%') )) and s.deleted = false ")
    fun getPages(pageable: Pageable, search: String): Page<ClinicEntity>
}