package clinic

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

@Service
class TokenService(
    jwtProperties: JwtProperties
) {
    private val secretKey = Keys.hmacShaKeyFor(
        jwtProperties.key.toByteArray()
    )

    fun generate(
        userDetails: UserDetails,
        expirationDate: Date,
        additionalClaims: Map<String, Any> = emptyMap()
    ): String {
        val role = userDetails.authorities.firstOrNull()?.authority ?: throw EmployeeRoleNotExistException()

        return Jwts.builder()
            .claims(additionalClaims + ("role" to role))
            .subject(userDetails.username)
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(expirationDate)
            .signWith(secretKey)
            .compact()
    }

    fun extractUsername(token: String): String? = getAllClaims(token)
        .subject

    fun isExpired(token: String): Boolean = getAllClaims(token)
        .expiration
        .before(Date(System.currentTimeMillis()))

    fun isValid(token: String, userDetails: UserDetails): Boolean {
        val email = extractUsername(token)

        return userDetails.username == email && !isExpired(token)


    }

    private fun getAllClaims(token: String): Claims {
        val parser = Jwts.parser()
            .verifyWith(secretKey)
            .build()

        return parser
            .parseSignedClaims(token)
            .payload
    }
}

@Service
class CustomUserDetailsService(
    private val employeeRepository: EmployeeRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        return employeeRepository.findByUsernameAndDeletedFalse(username)
            ?: throw AuthenticationException()
    }
}

@Service
class AuthenticationService(
    private val authManager: AuthenticationManager,
    private val userDetailsService: CustomUserDetailsService,
    private val tokenService: TokenService,
    private val jwtProperties: JwtProperties,
    private val refreshTokenRepository: RefreshTokenRepository

) {
    fun authentication(authRequest: AuthenticationRequest): AuthenticationResponse {
        authManager.authenticate(
            UsernamePasswordAuthenticationToken(
                authRequest.username,
                authRequest.password
            )
        )

        val user = userDetailsService.loadUserByUsername(authRequest.username)

        val employee = user as Employee

        val accessToken = generateAccessToken(user)

        val refreshToken = generateRefreshToken(user)

        refreshTokenRepository.save(RefreshToken(employee = employee, refreshToken = refreshToken))


        return AuthenticationResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun generateRefreshToken(user: UserDetails) = tokenService.generate(
        userDetails = user,
        expirationDate = Date(System.currentTimeMillis() + jwtProperties.refreshTokenExpiration)
    )

    private fun generateAccessToken(user: UserDetails) = tokenService.generate(
        userDetails = user,
        expirationDate = Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration)
    )

    fun refreshAccessToken(token: String): String? {
        val extractedUsername = tokenService.extractUsername(token)

        return extractedUsername?.let { username ->
            val currentUserDetails = userDetailsService.loadUserByUsername(username)
            val refreshTokenUser = refreshTokenRepository.findRefreshTokenByRefreshTokenAndDeletedFalse(token)

            if (!tokenService.isExpired(token) && currentUserDetails.username == refreshTokenUser.employee.username)
                generateAccessToken(currentUserDetails)
            else
                null
        }

    }

}

interface PatientService {
    fun create(request: PatientCreateRequest)
    fun getAll(pageable: Pageable): Page<PatientResponse>
    fun getOne(id: Long): PatientResponse
    fun delete(id: Long)
    fun getPatient(id: Long): Patient
    fun update(id: Long, request: PatientUpdateRequest)
    fun updateBalance(id: Long, money: BigDecimal)
    fun reduceBalance(patient: Patient,reducedMoney:BigDecimal)
}

// todo security qoshish kerak
@Service
class PatientServiceImpl(private val patientRepository: PatientRepository) : PatientService {
    override fun create(request: PatientCreateRequest) {
        request.run {
            if (patientRepository.existsByUsernameAndDeletedFalse(username))
                throw PatientAlreadyExistException()
            patientRepository.save(toEntity(DateUtils.toYearMonthDate(dateOfBirth)))
        }
    }

    override fun getAll(pageable: Pageable): Page<PatientResponse> {
        val pages = patientRepository.findAllNotDeleted(pageable)
        return pages.map {
            PatientResponse.toResponse(it)
        }
    }


    override fun getOne(id: Long): PatientResponse {
        return PatientResponse.toResponse(getPatient(id))
    }

    @Transactional
    override fun delete(id: Long) {
        patientRepository.trash(id) ?: throw PatientNotFoundException()
    }

    override fun getPatient(id: Long): Patient =
        patientRepository.findByIdAndDeletedFalse(id) ?: throw PatientNotFoundException()

    @Transactional
    override fun update(id: Long, request: PatientUpdateRequest) {
        val patient = getPatient(id)
        request.run {
            fullName?.let {
                patient.fullName = it
            }
            username?.let {
                if (patientRepository.existsByUsernameAndIdNotAndDeletedFalse(username, id))
                    throw PatientAlreadyExistException()
                patient.username = it
            }
        }

        patientRepository.save(patient)
    }

    override fun updateBalance(id: Long, money: BigDecimal) {
        val patient = getPatient(id)
        patient.balance += money
        patientRepository.save(patient)
    }

    override fun reduceBalance(patient: Patient, reducedMoney: BigDecimal) {
        patient.balance-=reducedMoney
        patientRepository.save(patient)
    }
}

interface EmployeeService {
    fun create(request: EmployeeCreateRequest)
    fun getAll(pageable: Pageable): Page<EmployeeResponse>
    fun getOne(id: Long): EmployeeResponse
    fun delete(id: Long)
    fun getEmployee(id: Long): Employee
    fun update(id: Long, request: EmployeeUpdateRequest)
}

@Service
class EmployeeServiceImpl(
    private val encoder: PasswordEncoder,
    private val employeeRepository: EmployeeRepository
) : EmployeeService {
    override fun create(request: EmployeeCreateRequest) {
        request.run {
            employeeRepository.save(toEntity(encoder.encode(password)))
        }
    }

    //todo search va filter qoshish
    override fun getAll(pageable: Pageable): Page<EmployeeResponse> {
        val pages: Page<Employee> = employeeRepository.findAllNotDeleted(pageable)
        return pages.map {
            EmployeeResponse.toResponse(it)
        }
    }

    override fun getOne(id: Long): EmployeeResponse {
        return EmployeeResponse.toResponse(getEmployee(id))
    }

    override fun delete(id: Long) {
        employeeRepository.trash(id) ?: throw EmployeeNotFoundException()
    }

    override fun getEmployee(id: Long): Employee =
        employeeRepository.findByIdAndDeletedFalse(id) ?: throw EmployeeNotFoundException()

    override fun update(id: Long, request: EmployeeUpdateRequest) {
        val employee = getEmployee(id)

        request.run {
            fullName?.let {
                employee.fullName = it
            }
        }
        employeeRepository.save(employee)
    }

}


interface Services {
    fun create(request: ServiceCreateRequest)
    fun getAll(pageable: Pageable): Page<ServiceResponse>
    fun getOne(id: Long): ServiceResponse
    fun delete(id: Long)
    fun update(id: Long, request: ServiceUpdateRequest)
    fun getService(id: Long): clinic.Service
}

@Service
class ServicesImpl(private val serviceRepository: ServiceRepository) : Services {
    override fun create(request: ServiceCreateRequest) {
        request.run {
            if (serviceRepository.existsByNameAndDeletedFalse(name)) throw ServiceAlreadyExistException()
            serviceRepository.save(toEntity(DateUtils.toDay(duration)))
        }
    }


    override fun getAll(pageable: Pageable): Page<ServiceResponse> {
        val pages = serviceRepository.findAllNotDeleted(pageable)
        return pages.map {
            ServiceResponse.toResponse(it)
        }
    }

    override fun getOne(id: Long): ServiceResponse {
        val service = getService(id)
        return ServiceResponse.toResponse(service)
    }

    @Transactional
    override fun delete(id: Long) {
        serviceRepository.trash(id) ?: throw ServiceNotFoundException()
    }

    @Transactional
    override fun update(id: Long, request: ServiceUpdateRequest) {
        val service = getService(id)

        request.run {
            name?.let {
                if (serviceRepository.existsByNameAndIdNotAndDeletedFalse(it, id))
                    service.name = it
            }
            description?.let {
                service.description = it
            }
            price?.let {
                service.price = it
            }
            duration?.let {
                service.duration = DateUtils.toDay(it)
            }
            paymentType?.let {
                service.paymentType = it
            }
            serviceRepository.save(service)
        }
    }

    override fun getService(id: Long): clinic.Service =
        serviceRepository.findByIdAndDeletedFalse(id) ?: throw ServiceNotFoundException()

}

interface DoctorScheduleService {
    fun create(request: DoctorScheduleCreateRequest)
    fun getAll(pageable: Pageable): Page<DoctorScheduleResponse>
    fun getOne(id: Long): DoctorScheduleResponse
    fun delete(id: Long)
    fun update(id: Long, request: DoctorScheduleUpdateRequest)
    fun getDoctorSchedule(id: Long): DoctorSchedule
    fun getEmptyDoctorSchedule(id: Long, fromDate: LocalDate): DoctorSchedule
    fun changeDoctorStatus(doctorSchedule: DoctorSchedule, doctorStatus: DoctorStatus)
}

@Service
class DoctorScheduleServiceImpl(
    private val repository: DoctorScheduleRepository,
    private val employeeService: EmployeeService
) : DoctorScheduleService {

    override fun create(request: DoctorScheduleCreateRequest) {
        request.run {
            val doctor = employeeService.getEmployee(request.doctorId)
            if (doctor.role != Role.DOCTOR) throw NoHavePermission()
            repository.save(toEntity(doctor))
        }

    }

    @Transactional
    override fun getAll(pageable: Pageable): Page<DoctorScheduleResponse> {
        val pages = repository.findAllNotDeleted(pageable)
        return pages.map {
            DoctorScheduleResponse.toResponse(it, it.dayOfWeek.dayOfWeek.toString())
        }
    }

    @Transactional
    override fun getOne(id: Long): DoctorScheduleResponse {
        val doctorSchedule = getDoctorSchedule(id)
        doctorSchedule.run {
            return DoctorScheduleResponse.toResponse(
                doctorSchedule,
                dayOfWeek.dayOfWeek.toString()
            )
        }
    }

    @Transactional
    override fun delete(id: Long) {
        repository.trash(id) ?: throw DoctorScheduleNotFound()
    }

    @Transactional
    override fun update(id: Long, request: DoctorScheduleUpdateRequest) {
        val doctorSchedule = getDoctorSchedule(id)

        request.run {
            dayOfWeek?.let {
                doctorSchedule.dayOfWeek = DateUtils.toYearMonthDate(it)
            }

            startTime?.let {
                doctorSchedule.startTime = DateUtils.toHour(it)
            }

            finishTime?.let {
                doctorSchedule.finishTime = DateUtils.toHour(it)
            }
        }

        repository.save(doctorSchedule)

    }

    override fun getDoctorSchedule(id: Long): DoctorSchedule =
        repository.findByIdAndDeletedFalse(id) ?: throw DoctorScheduleNotFound()

    @Transactional
    override fun getEmptyDoctorSchedule(id: Long, fromDate: LocalDate): DoctorSchedule {
        val doctor = employeeService.getEmployee(id)
        if (doctor.role != Role.DOCTOR) throw NoHavePermission()
        val emptyDoctor =
            repository.getEmptyDoctor(id, fromDate, DoctorStatus.NO_PATIENT) ?: throw DoctorScheduleNotAvailable()

        return emptyDoctor
    }

    override fun changeDoctorStatus(doctorSchedule: DoctorSchedule, doctorStatus: DoctorStatus) {
        doctorStatus.run {
            doctorSchedule.status = doctorStatus
            repository.save(doctorSchedule)
        }
    }

}

interface CardServiceC {
    fun create(request: CardRequest)
    fun getCard(id: Long): Card
    fun updateCardService(id: Long, doctorId: Long, cardRequest: CardUpdateRequest)
    fun getPatientServices(patientId: Long): CardResponse
}

@Service
class CardImpl(
    private val cardRepository: CardRepository,
    private val patientService: PatientService,
    private val doctorScheduleService: DoctorScheduleService,
    private val services: Services,
    private val cardServiceRepository: CardServiceRepository,
    private val employeeService: EmployeeService,
    private val paymentService: PaymentService
) : CardServiceC {

    @Transactional
    override fun create(request: CardRequest) {
        request.run {
            val patient = patientService.getPatient(patientId)
            val card = cardRepository.findByPatientAndDeletedFalse(patient) ?: cardRepository.save(
                Card(
                    patient,
                    BigDecimal.ZERO,
                    CardStatus.ACTIVE
                )
            )


            val fromEDate = DateUtils.toYearMonthDate(request.fromDate)
            val doctor = doctorScheduleService.getEmptyDoctorSchedule(doctorId, fromEDate)
            val cardService = services.getService(serviceId)
            val expectedDate = fromEDate.plusDays(DateUtils.toYearMonthDate(cardService.duration).dayOfMonth.toLong())
            cardServiceRepository.save(
                CardService(
                    card,
                    cardService,
                    doctor.doctor,
                    DateUtils.toYearMonthDate(fromDate),
                    expectedDate,
                    CardServiceStatus.IN_PROCESS
                )
            )
            doctorScheduleService.changeDoctorStatus(doctor, DoctorStatus.HAS_PATIENT)
        }
    }

    override fun getCard(id: Long): Card = cardRepository.findByIdAndDeletedFalse(id) ?: throw CardNotFound()

    @Transactional
    override fun updateCardService(id: Long, doctorId: Long, cardRequest: CardUpdateRequest) {
        val employee = employeeService.getEmployee(doctorId)
        val cardService = cardServiceRepository.findByIdAndDeletedFalse(id) ?: throw CardServiceNotFoundException()

        if (cardService.doctor.id != doctorId && employee.role != Role.DIRECTOR)
            throw NoHavePermission()
        var fromUpdatedDate = cardService.fromDate
        cardRequest.run {
            fromDate?.let {
                val temp = DateUtils.toYearMonthDate(it)
                cardService.fromDate = temp
                fromUpdatedDate = temp
            }
            serviceId?.let {
                cardService.service = services.getService(it)
            }
            finishDate?.let {
                val finishTime = DateUtils.toYearMonthDate(finishDate)
                if (finishTime.isBefore(fromUpdatedDate)) {
                    throw BeforeTimeException()
                }
                cardService.toDate = finishTime
                cardService.status = CardServiceStatus.DONE
            }

        }

        cardServiceRepository.save(cardService)
    }

    override fun getPatientServices(patientId: Long): CardResponse {
        val patient = patientService.getPatient(patientId)
        val card = cardRepository.findByPatientAndDeletedFalse(patient) ?: throw CardNotFound()
        val cardServicesOfPatient = cardServiceRepository.findByCardAndDeletedFalse(card)
        val cardServiceResponses: List<CardServiceResponse> = cardServicesOfPatient.map {
            map(it)
        }
        val totalAmount = paymentService.getTotalAmountOfPatient(patient)
        return CardResponse(card.id!!, PatientResponse.toResponse(patient), totalAmount, cardServiceResponses)
    }

    private fun map(cardService: CardService): CardServiceResponse {
        val serviceResponse = ServiceResponse.toResponse(cardService.service)

        return CardServiceResponse(
            serviceResponse,
            cardService.doctor.fullName,
            cardService.fromDate.toString(),
            cardService.toDate.toString(),
            cardService.status
        )
    }
}

interface PaymentService {
    fun create(request: PaymentCreateRequest)
    fun getDetailPaymentsOfPatient(patientId: Long): PaymentResponseDetail
    fun getTotalAmountOfPatient(patient: Patient): BigDecimal
}

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val cardServiceRepository: CardServiceRepository,
    private val patientService: PatientService,
) : PaymentService {
    override fun create(request: PaymentCreateRequest) {
        request.run {
            val cardService =
                cardServiceRepository.findByIdAndDeletedFalse(cardServiceId) ?: throw CardServiceNotFoundException()

            val userMoney = cardService.card.patient.balance

            if (userMoney < paidAmount) throw BalanceNotEnoughException()

            paymentRepository.save(
                Payment(
                    cardService, cardService.card.patient, paidAmount,
                    PaymentMethod.valueOf(paymentMethod), PaymentStatus.PAID
                )
            )
            patientService.reduceBalance(cardService.card.patient, paidAmount)
        }
    }

    override fun getDetailPaymentsOfPatient(patientId: Long): PaymentResponseDetail {
        val patient = patientService.getPatient(patientId)

        val payments: List<Payment> = paymentRepository.findAllByPatientAndDeletedFalse(patient)
        var totalMoney = BigDecimal.ZERO
        val paymentsList = mutableListOf<PaymentResponse>()

        payments.map {
            paymentsList.add(PaymentResponse.toEntity(it))
            totalMoney += it.paidAmount
        }

        return PaymentResponseDetail(patient.fullName, paymentsList, totalMoney)

    }


    override fun getTotalAmountOfPatient(patient: Patient): BigDecimal {
        return paymentRepository.getTotalAmountOfUser(patient)
    }


}

