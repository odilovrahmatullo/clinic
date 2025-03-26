package clinic

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationService: AuthenticationService
) {

    @PostMapping
    fun authenticate(@RequestBody authRequest: AuthenticationRequest): AuthenticationResponse =
        authenticationService.authentication(authRequest)

    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody request: RefreshTokenRequest
    ): TokenResponse =
        authenticationService.refreshAccessToken(request.token)?.mapToTokenResponse() ?: throw ForbiddenException()

    private fun String.mapToTokenResponse(): TokenResponse = TokenResponse(
        token = this
    )
}

@RequestMapping("/api/user")
@RestController
class UserController(private val userService: UserService) {

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_DIRECTOR','ROLE_OWNER')")
    fun createEmployee(@RequestBody @Valid request: UserCreateRequest) = userService.createEmployee(request)

    @PostMapping("/patient")
    fun createPatient(@RequestBody @Valid request: PatientCreateRequest) = userService.createPatient(request)


    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_DIRECTOR','ROLE_OWNER','ROLE_DOCTOR')")
    fun list(@Valid params: RequestParams,@RequestParam(required = false) role:Role? ): Page<UserResponse> {
        return userService.getAll(PageRequest.of(params.page, params.size),params.search,role)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_DIRECTOR') or @springSecurityUtil.getCurrentUserId() == #id")
    fun getOne(@PathVariable("id") id: Long) = userService.getOne(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_DIRECTOR') or @springSecurityUtil.getCurrentUserId() == #id")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: UserUpdateRequest) =
        userService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_DIRECTOR')")
    fun delete(@PathVariable("id") id: Long) = userService.delete(id)

}

@RequestMapping("/api/services")
@RestController
class ServiceController(private val services: ItemService) {

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_DIRECTOR','ROLE_DOCTOR')")
    fun create(@RequestBody @Valid request: ItemCreateRequest) = services.create(request)

    @GetMapping
    fun list(@Valid params: RequestParams): Page<ItemResponse> {
        return services.getAll(PageRequest.of(params.page, params.size),params.search)
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable("id") id: Long) = services.getOne(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_DIRECTOR','ROLE_DOCTOR')")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: ItemUpdateRequest) =
        services.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_DIRECTOR','ROLE_DOCTOR')")
    fun delete(@PathVariable("id") id: Long) = services.delete(id)

}

@RequestMapping("/api/doctor")
@RestController
class DoctorScheduleController(private val doctorScheduleService: DoctorScheduleService) {
    @PostMapping
    @PreAuthorize("hasRole('ROLE_DOCTOR')")
    fun create(@RequestBody @Valid request: DoctorScheduleCreateRequest) = doctorScheduleService.create(request)

    @GetMapping
    fun list(@Valid params: RequestParams): Page<DoctorScheduleResponse> {
        return doctorScheduleService.getAll(PageRequest.of(params.page, params.size))
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable("id") id: Long) = doctorScheduleService.getOne(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole(@springSecurityUtil.currentUserId = #id)")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: DoctorScheduleUpdateRequest) =
        doctorScheduleService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(@springSecurityUtil.currentUserId = #id)")
    fun delete(@PathVariable("id") id: Long) = doctorScheduleService.delete(id)
}

@RequestMapping("/api/card-item")
@RestController
class CardItemController(private val cardService: CardItemService) {

    @PostMapping
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    fun create(@RequestBody @Valid request: CardItemRequest) = cardService.create(request)

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ROLE_DOCTOR')")
    fun updateCard(@PathVariable id: Long, @RequestBody @Valid request: CardItemUpdateRequest) =
        cardService.updateCardItem(id, request)

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_DIRECTOR') or @springSecurityUtil.currentUserId == #patientId")
    fun getOne(@PathVariable patientId: Long): CardResponse = cardService.getPatientServices(patientId)
}

@RequestMapping("/api/payment")
@RestController
class PaymentController(private val paymentService: PaymentService) {
    @PostMapping
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    fun create(@RequestBody @Valid request: PaymentCreateRequest) = paymentService.create(request)


    @GetMapping("/{patientId}")
    @PreAuthorize("hasRole('ROLE_CASHIER') or @springSecurityUtil.currentUserId == #patientId")
    fun list(@PathVariable patientId: Long) = paymentService.getDetailPaymentsOfPatient(patientId)

}

@RequestMapping("/api/clinic")
@RestController
class ClinicController(private val clinicServiceService: ClinicService) {

    @PostMapping
    @PreAuthorize("hasRole('ROLE_OWNER')")
    fun create(@RequestBody @Valid request: ClinicCreateRequest) = clinicServiceService.create(request)

    @GetMapping
    @PreAuthorize("hasRole('ROLE_OWNER')")
    fun list(@Valid params: RequestParams): Page<ClinicResponse> {
        return clinicServiceService.getAll(PageRequest.of(params.page, params.size),params.search)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    fun getOne(@PathVariable("id") id: Long) = clinicServiceService.getOne(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: ClinicUpdateRequest) =
        clinicServiceService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_OWNER')")
    fun delete(@PathVariable("id") id: Long) = clinicServiceService.delete(id)

}

@RequestMapping("/api/card")
@RestController()
class CardController(
    private val cardService: CardService
){

    @PostMapping
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    fun create() = cardService.create()

    @PutMapping
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    fun updateBalance(@RequestBody @Valid updateBalanceRequest: UpdateBalanceRequest) = cardService.payMoneyToBalance(updateBalanceRequest.balance)

}


