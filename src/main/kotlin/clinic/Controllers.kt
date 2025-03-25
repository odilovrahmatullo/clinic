package clinic

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
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

@RequestMapping("/api/patient")
@RestController
@Validated
class PatientController(private val patientService: PatientService) {

    @PostMapping
    @PreAuthorize("hasRole('ROLE_DOCTOR')")
    fun create(@RequestBody @Valid request: PatientCreateRequest) = patientService.create(request)

    @GetMapping
    @PreAuthorize("hasRole('ROLE_DIRECTOR')")
    fun list(@Valid params: RequestParams): Page<PatientResponse> {
        return patientService.getAll(PageRequest.of(params.page, params.size))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_DOCTOR','ROLE_DIRECTOR')")
    fun getOne(@PathVariable("id") id: Long) = patientService.getOne(id)

    @PutMapping("/{id}")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: PatientUpdateRequest) =
        patientService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) = patientService.delete(id)

    @PutMapping("/balance/{id}")
    fun updateBalance(@PathVariable("id") id: Long, @RequestBody @Valid request: UpdateBalanceRequest) =
        patientService.updateBalance(id, request.balance)
}

@RequestMapping("/api/employee")
@RestController
class EmployeeController(private val employeeServiceService: EmployeeService) {

    @PostMapping
    fun create(@RequestBody @Valid request: EmployeeCreateRequest) = employeeServiceService.create(request)

    @GetMapping
    fun list(@Valid params: RequestParams): Page<EmployeeResponse> {
        return employeeServiceService.getAll(PageRequest.of(params.page, params.size))
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable("id") id: Long) = employeeServiceService.getOne(id)

    @PutMapping("/{id}")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: EmployeeUpdateRequest) =
        employeeServiceService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) = employeeServiceService.delete(id)

}

@RequestMapping("/api/services")
@RestController
class ServiceController(private val services: Services) {

    @PostMapping
    fun create(@RequestBody @Valid request: ServiceCreateRequest) = services.create(request)

    @GetMapping
    fun list(@Valid params: RequestParams): Page<ServiceResponse> {
        return services.getAll(PageRequest.of(params.page, params.size))
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable("id") id: Long) = services.getOne(id)

    @PutMapping("/{id}")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: ServiceUpdateRequest) =
        services.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) = services.delete(id)

}

@RequestMapping("/api/doctor")
@RestController
class DoctorScheduleController(private val doctorScheduleService: DoctorScheduleService) {
    @PostMapping
    fun create(@RequestBody @Valid request: DoctorScheduleCreateRequest) = doctorScheduleService.create(request)

    @GetMapping
    fun list(@Valid params: RequestParams): Page<DoctorScheduleResponse> {
        return doctorScheduleService.getAll(PageRequest.of(params.page, params.size))
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable("id") id: Long) = doctorScheduleService.getOne(id)

    @PutMapping("/{id}")
    fun update(@PathVariable("id") id: Long, @RequestBody @Valid request: DoctorScheduleUpdateRequest) =
        doctorScheduleService.update(id, request)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable("id") id: Long) = doctorScheduleService.delete(id)
}

@RequestMapping("/api/card")
@RestController
class CardController(private val cardService: CardServiceC) {

    @PostMapping
    fun create(@RequestBody @Valid request: CardRequest) = cardService.create(request)

    @PutMapping("/{id}/{doctorId}")
    fun updateCard(@PathVariable id: Long, @PathVariable("doctorId") doctorId: Long, request: CardUpdateRequest) =
        cardService.updateCardService(id, doctorId, request)

    @GetMapping("/{patientId}")
    fun getOne(@PathVariable patientId: Long): CardResponse = cardService.getPatientServices(patientId)
}

@RequestMapping("/api/payment")
@RestController
class PaymentController(private val paymentService: PaymentService) {
    @PostMapping
    fun create(@RequestBody @Valid request: PaymentCreateRequest) = paymentService.create(request)


    @GetMapping("{patientId}")
    fun list(@PathVariable patientId: Long) = paymentService.getDetailPaymentsOfPatient(patientId)

}



