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
interface PatientRepository : BaseRepository<Patient> {
    fun existsByUsernameAndDeletedFalse(fullName: String): Boolean

    fun existsByUsernameAndIdNotAndDeletedFalse(fullName: String, id: Long): Boolean
}

@Repository
interface EmployeeRepository : BaseRepository<Employee>{
    fun findByUsernameAndDeletedFalse(username: String) : Employee?
}

@Repository
interface ServiceRepository : BaseRepository<Service> {
    fun existsByNameAndDeletedFalse(name: String): Boolean

    fun existsByNameAndIdNotAndDeletedFalse(name: String, id: Long): Boolean
}

interface DoctorScheduleRepository : BaseRepository<DoctorSchedule> {
    @Query("""SELECT ds FROM DoctorSchedule ds 
        where ds.doctor.id = :id 
        and ds.status = :noPatient 
        and ds.dayOfWeek = :fromDate
        and  ds.deleted = false
        """)
    fun getEmptyDoctor(
        @Param("id") id: Long,
        @Param("fromDate")fromDate: LocalDate,
        @Param("noPatient")noPatient: DoctorStatus
    ): DoctorSchedule?
}

interface CardRepository : BaseRepository<Card> {
    fun findByPatientAndDeletedFalse(patient: Patient): Card?
}

interface CardServiceRepository : BaseRepository<CardService> {
    fun findByCardAndDeletedFalse(card: Card?): List<CardService>
}

interface PaymentRepository : BaseRepository<Payment> {
    fun findAllByPatientAndDeletedFalse(patient: Patient): List<Payment>

    @Query("SELECT SUM(p.paidAmount) FROM Payment as p where p.patient = ?1 and p.deleted = false ")
    fun getTotalAmountOfUser(patient: Patient): BigDecimal
}

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken,Long> {
    fun findRefreshTokenByRefreshTokenAndDeletedFalse(token: String): RefreshToken
}