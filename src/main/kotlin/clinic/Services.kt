package clinic

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
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
import java.security.SecureRandom
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

    fun extractUsername(token: String): String? {
        return try {
            getAllClaims(token).subject
        } catch (e: Exception) {
            throw JwtTokenException()
        }
    }

    fun isExpired(token: String): Boolean {
        try {
            return getAllClaims(token)
                .expiration
                .before(Date(System.currentTimeMillis()))
        } catch (e: Exception) {
            throw JwtTokenException()
        }
    }

    fun isValid(token: String, userDetails: UserDetails): Boolean {
        val email = extractUsername(token)

        return userDetails.username == email && !isExpired(token)
    }

    private fun getAllClaims(token: String): Claims {
        return try {
            val parser = Jwts.parser()
                .verifyWith(secretKey)
                .build()
            parser.parseSignedClaims(token).payload
        } catch (e: Exception) {
            throw JwtTokenException()
        }
    }
}

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        return userRepository.findByUsernameAndDeletedFalse(username) ?: throw AuthenticationException()
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

        val employee = user as User

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

interface UserService {
    fun createEmployee(request: UserCreateRequest)
    fun createPatient(request: PatientCreateRequest)
    fun getAll(pageable: Pageable, search: String, role: Role?): Page<UserResponse>
    fun getOne(id: Long): UserResponse
    fun delete(id: Long)
    fun getUser(id: Long): User
    fun update(id: Long, request: UserUpdateRequest)
}


@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val encoder: PasswordEncoder
) : UserService {

    override fun createEmployee(request: UserCreateRequest) {
        createUser(request)
    }

    override fun createPatient(request: PatientCreateRequest) {
        createUser(request)
    }


    private fun <T : CreateUserRequest> createUser(request: T) {
        request.run {
            if (userRepository.existsByUsername(username))
                throw UserAlreadyExistException()
            userRepository.save(toEntity(encoder.encode(password)))
        }
    }


    override fun getAll(pageable: Pageable, search: String, role: Role?): Page<UserResponse> {
        val pages = userRepository.getUsers(pageable, search, role)
        return pages.map {
            UserResponse.toResponse(it)
        }
    }


    override fun getOne(id: Long): UserResponse {
        return UserResponse.toResponse(getUser(id))
    }

    @Transactional
    override fun delete(id: Long) {
        userRepository.trash(id) ?: throw UserNotFoundException()
    }

    override fun getUser(id: Long): User =
        userRepository.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()

    @Transactional
    override fun update(id: Long, request: UserUpdateRequest) {
        val patient = getUser(id)
        request.run {
            fullName?.let {
                patient.fullName = it
            }
            username?.let {
                if (userRepository.existsByUsernameAndIdNot(username, id))
                    throw UserAlreadyExistException()
                patient.username = it
            }
        }

        userRepository.save(patient)
    }
}

interface ItemService {
    fun create(request: ItemCreateRequest)
    fun getAll(pageable: Pageable, search: String): Page<ItemResponse>
    fun getOne(id: Long): ItemResponse
    fun delete(id: Long)
    fun update(id: Long, request: ItemUpdateRequest)
    fun getItem(id: Long): Item
}

@Service
class ItemServiceImpl(private val serviceRepository: ItemRepository) : ItemService {
    override fun create(request: ItemCreateRequest) {
        request.run {
            if (serviceRepository.existsByNameAndDeletedFalse(name)) throw ServiceAlreadyExistException()
            serviceRepository.save(toEntity(DateUtils.toDay(duration)))
        }
    }

    override fun getAll(pageable: Pageable, search: String): Page<ItemResponse> {
        val pages = serviceRepository.findAllAsSearch(pageable, search)
        return pages.map {
            ItemResponse.toResponse(it)
        }
    }


    override fun getOne(id: Long): ItemResponse {
        val service = getItem(id)
        return ItemResponse.toResponse(service)
    }

    @Transactional
    override fun delete(id: Long) {
        serviceRepository.trash(id) ?: throw ServiceNotFoundException()
    }

    @Transactional
    override fun update(id: Long, request: ItemUpdateRequest) {
        val service = getItem(id)

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

    override fun getItem(id: Long): Item =
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
    private val userService: UserService,
    private val springSecurityUtil: SpringSecurityUtil
) : DoctorScheduleService {

    override fun create(request: DoctorScheduleCreateRequest) {
        request.run {
            val user = springSecurityUtil.getCurrentUser() as User
            val date = DateUtils.toYearMonthDate(dayOfWeek)
            if (repository.existsByDate(date)) throw DayIsNotAvailable()
            repository.save(toEntity(user, date))
        }

    }

    @Transactional
    override fun getAll(pageable: Pageable): Page<DoctorScheduleResponse> {
        val pages = repository.findAllNotDeleted(pageable)
        return pages.map {
            DoctorScheduleResponse.toResponse(it, it.date.dayOfWeek.toString())
        }
    }

    @Transactional
    override fun getOne(id: Long): DoctorScheduleResponse {
        val doctorSchedule = getDoctorSchedule(id)
        doctorSchedule.run {
            return DoctorScheduleResponse.toResponse(
                doctorSchedule,
                date.dayOfWeek.toString()
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
                doctorSchedule.date = DateUtils.toYearMonthDate(it)
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
        val doctor = userService.getUser(id)
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

interface CardItemService {
    fun create(request: CardItemRequest)
    fun getCardItem(id: Long): CardItem
    fun updateCardItem(id: Long, cardRequest: CardItemUpdateRequest)
    fun getPatientServices(patientId: Long): CardResponse
}

@Service
class CardItemImpl(
    private val cardRepository: CardRepository,
    private val patientService: UserService,
    private val doctorScheduleService: DoctorScheduleService,
    private val services: ItemService,
    private val cardService: CardService,
    private val cardItemRepository: CardItemRepository,
    private val springSecurityUtil: SpringSecurityUtil,
    private val paymentRepository: PaymentRepository
) : CardItemService {
    @Transactional
    override fun create(request: CardItemRequest) {
        request.run {
            val card = cardService.getCard(springSecurityUtil.getCurrentUser() as User)

            val fromEDate = DateUtils.toYearMonthDate(request.fromDate)

            val doctor = doctorScheduleService.getEmptyDoctorSchedule(doctorId, fromEDate)

            val cardService = services.getItem(serviceId)

            val expectedDate = fromEDate.plusDays(DateUtils.toYearMonthDate(cardService.duration).dayOfMonth.toLong())

            cardItemRepository.save(
                CardItem(
                    card,
                    cardService,
                    doctor.doctor,
                    DateUtils.toYearMonthDate(fromDate),
                    expectedDate,
                    CardItemStatus.IN_PROCESS
                )
            )
            doctorScheduleService.changeDoctorStatus(doctor, DoctorStatus.HAS_PATIENT)
        }
    }

    override fun getCardItem(id: Long): CardItem =
        cardItemRepository.findByIdAndDeletedFalse(id) ?: throw CardServiceNotFoundException()

    @Transactional
    override fun updateCardItem(id: Long, cardRequest: CardItemUpdateRequest) {

        val doctor = springSecurityUtil.getCurrentUser() as User

        val cardService = getCardItem(id)

        if (cardService.doctor.id != doctor.id)
            throw NoHavePermission()

        var fromUpdatedDate = cardService.fromDate
        cardRequest.run {
            fromDate?.let {
                val temp = DateUtils.toYearMonthDate(it)
                cardService.fromDate = temp
                fromUpdatedDate = temp
            }
            serviceId?.let {
                cardService.item = services.getItem(it)
            }
            finishDate?.let {
                val finishTime = DateUtils.toYearMonthDate(finishDate)
                if (finishTime.isBefore(fromUpdatedDate)) {
                    throw BeforeTimeException()
                }
                cardService.toDate = finishTime
                cardService.status = CardItemStatus.DONE
            }

        }

        cardItemRepository.save(cardService)
    }

    override fun getPatientServices(patientId: Long): CardResponse {
        val patient = patientService.getUser(patientId)
        val card = cardRepository.findByPatientAndDeletedFalse(patient) ?: throw CardNotFound()
        val cardServicesOfPatient = cardItemRepository.findByCardAndDeletedFalse(card)

        val cardServiceResponses: List<CardItemResponse> = cardServicesOfPatient.map {
            map(it)
        }

        val totalAmount = cardServiceResponses.sumOf { it.paidMoney }
        return CardResponse(
            card.id!!,
            UserResponse.toResponse(patient),
            card.cardNumber,
            card.totalAmount,
            card.status,
            cardServiceResponses,
            totalPaidAmount = totalAmount
        )
    }

    private fun map(cardItem: CardItem): CardItemResponse {
        val serviceResponse = ItemResponse.toResponse(cardItem.item)
        var money = BigDecimal.ZERO
        val payment = paymentRepository.findByCardItemAndDeletedFalse(cardItem)
        if (payment != null) money = payment.paidAmount

        return CardItemResponse(
            serviceResponse,
            cardItem.doctor.fullName,
            cardItem.fromDate.toString(),
            cardItem.toDate.toString(),
            cardItem.status,
            money
        )
    }
}

interface PaymentService {
    fun create(request: PaymentCreateRequest)
    fun getDetailPaymentsOfPatient(patientId: Long): PaymentResponseDetail
}

@Service
class PaymentServiceImpl(
    private val paymentRepository: PaymentRepository,
    private val cardService: CardService,
    private val patientService: UserService,
    private val cardItemService: CardItemService
) : PaymentService {
    override fun create(request: PaymentCreateRequest) {
        request.run {

            val cardItem = cardItemService.getCardItem(cardServiceId)

            val payment = paymentRepository.findByCardItemAndDeletedFalse(cardItem)

            val userMoney = cardItem.card.totalAmount

            if (userMoney < paidAmount) throw BalanceNotEnoughException()


            if (payment != null) {
                if (payment.paymentStatus == PaymentStatus.PAID) throw ServiceFinishedAlreadyException()
                if (paidAmount > cardItem.item.price - payment.paidAmount) throw MuchMoneyDontNeedException()
                val temp:BigDecimal = payment.paidAmount + paidAmount
                payment.paidAmount = temp
                if (temp == cardItem.item.price){
                    payment.paymentStatus = PaymentStatus.PAID
                }else {
                    payment.paymentStatus = PaymentStatus.NOT_PAID
                }
                paymentRepository.save(payment)
            } else {
                if (paidAmount > cardItem.item.price) throw MuchMoneyDontNeedException()
                val status = if (paidAmount == cardItem.item.price) PaymentStatus.PAID else PaymentStatus.NOT_PAID
                val newPayment =
                    Payment(cardItem, cardItem.card.patient, paidAmount, PaymentMethod.valueOf(paymentMethod), status)

                paymentRepository.save(newPayment)
            }

            cardService.reduceMoney(cardItem.card.patient, paidAmount)
        }
    }

    override fun getDetailPaymentsOfPatient(patientId: Long): PaymentResponseDetail {
        val patient = patientService.getUser(patientId)

        val payments: List<Payment> = paymentRepository.findAllByUserAndDeletedFalse(patient)
        var totalMoney = BigDecimal.ZERO
        val paymentsList = mutableListOf<PaymentResponse>()

        payments.map {
            paymentsList.add(PaymentResponse.toEntity(it))
            totalMoney += it.paidAmount
        }

        return PaymentResponseDetail(patient.fullName, paymentsList, totalMoney)

    }

}

interface ClinicService {
    fun create(request: ClinicCreateRequest)
    fun getAll(pageable: Pageable, search: String): Page<ClinicResponse>
    fun getOne(id: Long): ClinicResponse
    fun delete(id: Long)
    fun getClinic(id: Long): ClinicEntity
    fun update(id: Long, request: ClinicUpdateRequest)
}

@Service
class ClinicServiceImpl(
    private val clinicRepository: ClinicRepository
) : ClinicService {
    override fun create(request: ClinicCreateRequest) {
        request.run {
            clinicRepository.save(toEntity())
        }
    }

    override fun getAll(pageable: Pageable, search: String): Page<ClinicResponse> {
        val pages: Page<ClinicEntity> = clinicRepository.getPages(pageable, search)
        return pages.map {
            ClinicResponse.toResponse(it)
        }
    }

    override fun getOne(id: Long): ClinicResponse {
        return ClinicResponse.toResponse(getClinic(id))
    }

    @Transactional
    override fun delete(id: Long) {
        clinicRepository.trash(id) ?: throw ClinicNotFoundException()
    }

    override fun getClinic(id: Long): ClinicEntity =
        clinicRepository.findByIdAndDeletedFalse(id) ?: throw ClinicNotFoundException()

    @Transactional
    override fun update(id: Long, request: ClinicUpdateRequest) {
        val clinic = getClinic(id)

        request.run {
            name?.let {
                clinic.name = it
            }
            description?.let {
                clinic.description = it
            }
            address?.let {
                clinic.address = it
            }
            phone?.let {
                clinic.phone = it
            }

            clinicRepository.save(clinic)
        }
    }

}

interface CardService {
    fun create()
    fun getCard(patient: User): Card
    fun payMoneyToBalance(money: BigDecimal)
    fun reduceMoney(patient: User, money: BigDecimal)
}

@Service
class CardServiceImpl(
    private val cardRepository: CardRepository,
    private val springSecurityUtil: SpringSecurityUtil
) : CardService {
    override fun create() {
        val patient = springSecurityUtil.getCurrentUser() as User
        if (cardRepository.existsByPatientAndDeletedFalse(patient)) throw UserHasCard()
        val cardNumber = generateUniqueCardNumber()

        cardRepository.save(Card(patient, BigDecimal.ZERO, CardStatus.ACTIVE, cardNumber))
    }

    private fun generateCardNumber(): String {
        val random = SecureRandom()
        val min = 1_0000_0000_0000_0000L
        val max = 9_9999_9999_9999_9999L

        val cardNumber = random.nextLong(max - min) + min

        return cardNumber.toString()
    }

    private fun generateUniqueCardNumber(): String {
        var cardNumber: String
        do {
            cardNumber = generateCardNumber()
        } while (cardRepository.existsByCardNumber(cardNumber))

        return cardNumber
    }

    override fun getCard(patient: User): Card =
        cardRepository.findByPatientAndDeletedFalse(patient) ?: throw CardNotFound()

    override fun payMoneyToBalance(money: BigDecimal) {
        val card = getCard(springSecurityUtil.getCurrentUser() as User)
        card.totalAmount += money
        cardRepository.save(card)
    }

    override fun reduceMoney(patient: User, money: BigDecimal) {
        val card = getCard(patient)
        card.totalAmount -= money
        cardRepository.save(card)
    }
}

